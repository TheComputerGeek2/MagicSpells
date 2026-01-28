package com.nisovin.magicspells.spells;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.InventoryView;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

public class MenuSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private static final NamespacedKey OPTION_KEY = new NamespacedKey(MagicSpells.getInstance(), "menu_option");

	private final Map<String, MenuOption> options = new LinkedHashMap<>();

	private int size;

	private final ItemStack filler;
	private final ConfigData<Integer> delay;
	private final ConfigData<Component> title;
	private final ConfigData<Boolean> stayOpenNonOption;
	private final ConfigData<Boolean> bypassNormalCast;
	private final ConfigData<Boolean> requireEntityTarget;
	private final ConfigData<Boolean> requireLocationTarget;
	private final ConfigData<Boolean> targetOpensMenuInstead;

	public MenuSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		title = getConfigDataComponent("title", Component.text("Window Title " + spellName));
		delay = getConfigDataInt("delay", 0);
		filler = createItem("filler");
		stayOpenNonOption = getConfigDataBoolean("stay-open-non-option", false);
		bypassNormalCast = getConfigDataBoolean("bypass-normal-cast", true);
		requireEntityTarget = getConfigDataBoolean("require-entity-target", false);
		requireLocationTarget = getConfigDataBoolean("require-location-target", false);
		targetOpensMenuInstead = getConfigDataBoolean("target-opens-menu-instead", false);

		Set<String> optionKeys = getConfigKeys("options");
		if (optionKeys == null) {
			MagicSpells.error("MenuSpell '" + spellName + "' has no menu options!");
			return;
		}
		int maxSlot = (getConfigInt("min-rows", 1) * 9) - 1;
		for (String optionName : optionKeys) {
			String path = "options." + optionName + ".";

			List<Integer> slots = getConfigIntList(path + "slots", new ArrayList<>());
			if (slots.isEmpty()) slots.add(getConfigInt(path + "slot", -1));

			List<Integer> validSlots = new ArrayList<>();
			for (int slot : slots) {
				if (slot < 0 || slot > 53) {
					MagicSpells.error("MenuSpell '" + internalName + "' a slot defined which is out of bounds for '" + optionName + "': " + slot);
					continue;
				}
				validSlots.add(slot);
				if (slot > maxSlot) maxSlot = slot;
			}
			if (validSlots.isEmpty()) {
				MagicSpells.error("MenuSpell '" + internalName + "' has no slots defined for: " + optionName);
				continue;
			}

			ItemStack item = createItem(path + "item");
			List<String> itemList = getConfigStringList(path + "items", null);
			List<ItemStack> items = new ArrayList<>();
			if (item == null) {
				// If no items are defined, exit.
				if (itemList == null) {
					MagicSpells.error("MenuSpell '" + internalName + "' has no items defined for: " + optionName);
					continue;
				}
				// Otherwise process item list.
				for (String itemName : itemList) {
					MagicItem magicItem = MagicItems.getMagicItemFromString(itemName);
					if (magicItem == null) {
						MagicSpells.error("MenuSpell '" + internalName + "' has an invalid item listed in '" + optionName + "': " + itemName);
						continue;
					}
					ItemStack itemStack = magicItem.getItemStack();
					if (itemStack == null) {
						MagicSpells.error("MenuSpell '" + internalName + "' has an invalid item listed in '" + optionName + "': " + itemName);
						continue;
					}
					items.add(itemStack.clone());
				}
				// Skip if list was invalid.
				if (items.isEmpty()) {
					MagicSpells.error("MenuSpell '" + internalName + "' has no items defined for: " + optionName);
					continue;
				}
			}

			MenuOption option = new MenuOption();
			option.menuOptionName = optionName;
			option.slots = validSlots;
			option.item = item;
			option.items = items;
			option.quantity = getConfigString(path + "quantity", "");
			option.spellName = getConfigString(path + "spell", "");
			option.spellRightName = getConfigString(path + "spell-right", "");
			option.spellSneakLeftName = getConfigString(path + "spell-sneak-left", "");
			option.spellSneakRightName = getConfigString(path + "spell-sneak-right", "");
			option.spellDropName = getConfigString(path + "spell-drop", "");
			option.spellSwapName = getConfigString(path + "spell-swap", "");
			option.power = getConfigFloat(path + "power", 1);
			option.modifierList = getConfigStringList(path + "modifiers", null);
			option.stayOpen = getConfigBoolean(path + "stay-open", false);
			options.put(optionName, option);
		}
		size = (int) Math.ceil((maxSlot + 1) / 9.0) * 9;
		if (options.isEmpty()) MagicSpells.error("MenuSpell '" + spellName + "' has no menu options!");
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		for (MenuOption option : options.values()) {
			if (option.modifierList != null) option.menuOptionModifiers = new ModifierSet(option.modifierList, this);
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		for (MenuOption option : options.values()) {
			String error = "MenuSpell '" + internalName + "' has an invalid '%s' defined for: " + option.menuOptionName;
			option.spell = initSubspell(option.spellName,
					error.formatted("spell"),
					true);
			option.spellRight = initSubspell(option.spellRightName,
					error.formatted("spell-right"),
					true);
			option.spellSneakLeft = initSubspell(option.spellSneakLeftName,
					error.formatted("spell-sneak-left"),
					true);
			option.spellSneakRight = initSubspell(option.spellSneakRightName,
					error.formatted("spell-sneak-right"),
					true);
			option.spellDrop = initSubspell(option.spellDropName,
					error.formatted("spell-drop"),
					true);
			option.spellSwap = initSubspell(option.spellSwapName,
					error.formatted("spell-swap"),
					true);
		}
	}

	@Override
	protected void turnOff() {
		InventoryView view;
		for (Player player : Bukkit.getOnlinePlayers()) {
			view = player.getOpenInventory();
			if (view.getTopInventory().getHolder(false) instanceof MenuInventory menu && menu.getSpell() == this)
				view.close();
		}
	}

	@Override
	public CastResult cast(SpellData data) {
		boolean targetOpensMenuInstead = this.targetOpensMenuInstead.get(data);

		if (requireEntityTarget.get(data)) {
			if (targetOpensMenuInstead) {
				TargetInfo<Player> info = getTargetedPlayer(data);
				if (info.noTarget()) return noTarget(info);
				data = info.spellData();
			} else {
				TargetInfo<LivingEntity> info = getTargetedEntity(data);
				if (info.noTarget()) return noTarget(info);
				data = info.spellData();
			}
		} else if (requireLocationTarget.get(data)) {
			TargetInfo<Location> info = getTargetedBlockLocation(data, false);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();
		}

		LivingEntity openerEntity = targetOpensMenuInstead ? data.target() : data.caster();
		if (!(openerEntity instanceof Player opener)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		open(opener, data, targetOpensMenuInstead);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		open(caster, data, false);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		boolean targetOpensMenuInstead = this.targetOpensMenuInstead.get(data);
		LivingEntity openerEntity = targetOpensMenuInstead ? data.target() : data.caster();
		if (!(openerEntity instanceof Player opener)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		open(opener, data, targetOpensMenuInstead);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args.length < 1) return false;

		Player player = Bukkit.getPlayer(args[0]);
		if (player == null) return false;

		String[] spellArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : null;
		open(player, new SpellData(null, player, 1f, spellArgs), true);

		return true;
	}

	private ItemStack createItem(String path) {
		MagicItem magicItem = isConfigSection(path) ?
				MagicItems.getMagicItemFromSection(getConfigSection(path)) :
				MagicItems.getMagicItemFromString(getConfigString(path, ""));
		ItemStack item = magicItem == null ? null : magicItem.getItemStack();
		return item == null ? null : item.clone();
	}

	private void open(Player opener, SpellData data, boolean targetOpensMenuInstead) {
		int delay = this.delay.get(data);
		if (delay < 0) openMenu(opener, data, targetOpensMenuInstead);
		else MagicSpells.scheduleDelayedTask(() -> openMenu(opener, data, targetOpensMenuInstead), delay);
	}

	private void openMenu(Player opener, SpellData data, boolean targetOpensMenuInstead) {
		MenuInventory menu = new MenuInventory(data, targetOpensMenuInstead);
		applyOptionsToInventory(opener, menu);
		opener.openInventory(menu.inventory);

		playSpellEffects(data);
		playSpellEffects(EffectPosition.SPECIAL, opener, data);
	}

	private void applyOptionsToInventory(Player opener, MenuInventory menu) {
		Inventory inv = menu.getInventory();

		// Setup option items.
		for (MenuOption option : options.values()) {
			// Check modifiers.
			if (option.menuOptionModifiers != null) {
				MagicSpellsGenericPlayerEvent event = new MagicSpellsGenericPlayerEvent(opener);
				option.menuOptionModifiers.apply(event);
				if (event.isCancelled()) continue;
			}
			// Select and finalise item to display.
			ItemStack item = option.item != null ? option.item : option.items.get(random.nextInt(option.items.size()));
			item.editMeta(meta -> meta.getPersistentDataContainer().set(OPTION_KEY, PersistentDataType.STRING, option.menuOptionName));
			item = translateItem(opener, item, menu.data);

			int quantity;
			Variable variable = MagicSpells.getVariableManager().getVariable(option.quantity);
			if (variable == null) {
				try {
					quantity = Integer.parseInt(option.quantity);
				} catch (NumberFormatException e) {
					quantity = 1;
				}
			} else quantity = (int) Math.round(variable.getValue(opener));
			item.setAmount(quantity);

			// Set item for all defined slots.
			for (int slot : option.slots) {
				if (inv.getItem(slot) == null) inv.setItem(slot, item);
			}
		}
		// Fill inventory.
		if (filler == null) return;
		ItemStack item = translateItem(opener, filler, menu.data);
		for (int i = 0; i < inv.getSize(); i++) {
			if (inv.getItem(i) != null) continue;
			inv.setItem(i, item);
		}
	}

	private Component translateRawComponent(Component component, Player player, SpellData data) {
		String text = Util.getStringFromComponent(component);
		text = MagicSpells.doReplacements(text, player, data);
		return Util.getItemMiniMessage(text);
	}

	private ItemStack translateItem(Player opener, ItemStack item, SpellData data) {
		ItemStack newItem = item.clone();

		newItem.editMeta(meta -> {
			meta.displayName(translateRawComponent(meta.displayName(), opener, data));

			List<Component> lore = meta.lore();
			if (lore != null) {
				lore.replaceAll(component -> translateRawComponent(component, opener, data));
				meta.lore(lore);
			}
		});

		return newItem;
	}

	@EventHandler
	public void onInvClick(InventoryClickEvent event) {
		InventoryView view = event.getView();

		Inventory inventory = view.getTopInventory();
		if (!(inventory.getHolder(false) instanceof MenuInventory menu) || menu.getSpell() != this) return;

		event.setCancelled(true);
		if (event.getClickedInventory() != inventory) return;

		Player player = (Player) event.getWhoClicked();
		PostClickState state = castSpells(menu, event.getCurrentItem(), event.getClick());

		if (state == PostClickState.CLOSE) {
			if (player.getOpenInventory() == view) player.closeInventory();
			return;
		}

		if (state == PostClickState.REOPEN) {
			MenuInventory newMenu = new MenuInventory(menu.data, menu.targetOpensMenuInstead);
			applyOptionsToInventory(player, newMenu);
			player.openInventory(newMenu.getInventory());
		}
	}

	@EventHandler
	public void onInvDrag(InventoryDragEvent event) {
		InventoryView view = event.getView();

		Inventory inventory = view.getTopInventory();
		if (!(inventory.getHolder(false) instanceof MenuInventory menu) || menu.getSpell() != this) return;

		event.setCancelled(true);
	}

	private PostClickState castSpells(MenuInventory menu, ItemStack item, ClickType click) {
		// Outside inventory or not an option item.
		if (item == null || !item.hasItemMeta())
			return menu.stayOpenNonOption ? PostClickState.IGNORE : PostClickState.CLOSE;

		String key = item.getItemMeta().getPersistentDataContainer().get(OPTION_KEY, PersistentDataType.STRING);
		if (key == null || !options.containsKey(key))
			return menu.stayOpenNonOption ? PostClickState.IGNORE : PostClickState.CLOSE;

		MenuOption option = options.get(key);
		return switch (click) {
			case LEFT -> processClickSpell(menu, option.spell, option);
			case RIGHT -> processClickSpell(menu, option.spellRight, option);
			case SHIFT_LEFT -> processClickSpell(menu, option.spellSneakLeft, option);
			case SHIFT_RIGHT -> processClickSpell(menu, option.spellSneakRight, option);
			case DROP -> processClickSpell(menu, option.spellDrop, option);
			case SWAP_OFFHAND -> processClickSpell(menu, option.spellSwap, option);
			default -> option.stayOpen ? PostClickState.IGNORE : PostClickState.CLOSE;
		};
	}

	private PostClickState processClickSpell(MenuInventory menu, Subspell spell, MenuOption option) {
		if (spell == null) return option.stayOpen ? PostClickState.IGNORE : PostClickState.CLOSE;

		SpellData subData = menu.data.power(menu.data.power() * option.power);
		if (menu.targetOpensMenuInstead) subData = subData.builder().caster(subData.target()).target(null).build();

		if (subData.hasTarget() || subData.hasLocation() || menu.bypassNormalCast) spell.subcast(subData);
		else spell.getSpell().hardCast(subData);

		return option.stayOpen ? PostClickState.REOPEN : PostClickState.CLOSE;
	}

	private class MenuInventory implements InventoryHolder {

		private final SpellData data;
		private final Inventory inventory;

		private final boolean bypassNormalCast;
		private final boolean stayOpenNonOption;
		private final boolean targetOpensMenuInstead;

		public MenuInventory(SpellData data, boolean targetOpensMenuInstead) {
			this.data = data;
			this.targetOpensMenuInstead = targetOpensMenuInstead;

			bypassNormalCast = MenuSpell.this.bypassNormalCast.get(data);
			stayOpenNonOption = MenuSpell.this.stayOpenNonOption.get(data);

			inventory = Bukkit.createInventory(this, size, title.get(data));
		}

		@Override
		@NotNull
		public Inventory getInventory() {
			return inventory;
		}

		private MenuSpell getSpell() {
			return MenuSpell.this;
		}

	}

	private enum PostClickState {
		REOPEN,
		CLOSE,
		IGNORE
	}

	private static class MenuOption {

		private String menuOptionName;
		private List<Integer> slots;
		private ItemStack item;
		private List<ItemStack> items;
		private String quantity;
		private String spellName;
		private String spellRightName;
		private String spellSneakLeftName;
		private String spellSneakRightName;
		private String spellDropName;
		private String spellSwapName;
		private Subspell spell;
		private Subspell spellRight;
		private Subspell spellSneakLeft;
		private Subspell spellSneakRight;
		private Subspell spellDrop;
		private Subspell spellSwap;
		private float power;
		private List<String> modifierList;
		private ModifierSet menuOptionModifiers;
		private boolean stayOpen;

	}

}
