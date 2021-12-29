package com.nisovin.magicspells.spells;

import java.util.*;

import co.aikar.commands.ACFUtil;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

public class MenuSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private final Map<UUID, Float> castPower = new HashMap<>();
	private final Map<UUID, Location> castLocTarget = new HashMap<>();
	private final Map<UUID, LivingEntity> castEntityTarget = new HashMap<>();
	private final Map<String, MenuOption> options = new LinkedHashMap<>();

	private int size;

	private final String title;
	private final int delay;
	private final ItemStack filler;
	private final boolean stayOpenNonOption;
	private final boolean bypassNormalCast;
	private final boolean requireEntityTarget;
	private final boolean requireLocationTarget;
	private final boolean targetOpensMenuInstead;

	public MenuSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		title = getConfigString("title", "Window Title " + spellName);
		delay = getConfigInt("delay", 0);
		filler = createItem("filler");
		stayOpenNonOption = getConfigBoolean("stay-open-non-option", false);
		bypassNormalCast = getConfigBoolean("bypass-normal-cast", true);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		requireLocationTarget = getConfigBoolean("require-location-target", false);
		targetOpensMenuInstead = getConfigBoolean("target-opens-menu-instead", false);

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
					items.add(itemStack);
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
			option.spellMiddleName = getConfigString(path + "spell-middle", "");
			option.spellSneakLeftName = getConfigString(path + "spell-sneak-left", "");
			option.spellSneakRightName = getConfigString(path + "spell-sneak-right", "");
			option.power = getConfigFloat(path + "power", 1);
			option.modifierList = getConfigStringList(path + "modifiers", null);
			option.stayOpen = getConfigBoolean(path + "stay-open", false);
			options.put(optionName, option);
		}
		size = (int) Math.ceil((maxSlot+1) / 9.0) * 9;
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
			option.spell = initSubspell(option.spellName, "MenuSpell '" + internalName + "' has an invalid 'spell' defined for: " + option.menuOptionName);
			option.spellRight = initSubspell(option.spellRightName, "MenuSpell '" + internalName + "' has an invalid 'spell-right' defined for: " + option.menuOptionName);
			option.spellMiddle = initSubspell(option.spellMiddleName, "MenuSpell '" + internalName + "' has an invalid 'spell-middle' defined for: " + option.menuOptionName);
			option.spellSneakLeft = initSubspell(option.spellSneakLeftName, "MenuSpell '" + internalName + "' has an invalid 'spell-sneak-left' defined for: " + option.menuOptionName);
			option.spellSneakRight = initSubspell(option.spellSneakRightName, "MenuSpell '" + internalName + "' has an invalid 'spell-sneak-right' defined for: " + option.menuOptionName);
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player player) {
			LivingEntity entityTarget = null;
			Location locTarget = null;
			Player opener = player;

			if (requireEntityTarget) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power, args);
				if (targetInfo != null) entityTarget = targetInfo.getTarget();
				if (entityTarget == null) return noTarget(player);
				if (targetOpensMenuInstead) {
					if (!(entityTarget instanceof Player targetPlayer)) return noTarget(player);
					opener = targetPlayer;
					entityTarget = null;
				}
			} else if (requireLocationTarget) {
				Block block = getTargetedBlock(player, power);
				if (block == null || BlockUtils.isAir(block.getType())) return noTarget(player);
				locTarget = block.getLocation();
			}

			open(player, opener, entityTarget, locTarget, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (requireEntityTarget && !validTargetList.canTarget(caster, target)) return false;
		if (!(caster instanceof Player opener)) return false;
		if (targetOpensMenuInstead) {
			if (!(target instanceof Player player)) return false;
			opener = player;
			target = null;
		}
		open((Player) caster, opener, target, null, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (!targetOpensMenuInstead) return false;
		if (requireEntityTarget && !validTargetList.canTarget(target)) return false;
		if (!(target instanceof Player player)) return false;
		open(null, player, null, null, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		if (!(caster instanceof Player player)) return false;
		open(player, player, null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args.length < 1) return false;
		Player player = PlayerNameUtils.getPlayer(args[0]);
		String[] spellArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : null;
		if (player != null) {
			open(null, player, null, null, 1, spellArgs);
			return true;
		}
		return false;
	}

	private ItemStack createItem(String path) {
		ItemStack item = null;
		if (isConfigSection(path)) {
			MagicItem magicItem = MagicItems.getMagicItemFromSection(getConfigSection(path));
			if (magicItem != null) item = magicItem.getItemStack();
		} else {
			MagicItem magicItem = MagicItems.getMagicItemFromString(getConfigString(path, ""));
			if (magicItem != null) item = magicItem.getItemStack();
		}
		return item;
	}

	private void open(Player caster, Player opener, LivingEntity entityTarget, Location locTarget, float power, String[] args) {
		if (delay < 0) {
			openMenu(caster, opener, entityTarget, locTarget, power, args);
			return;
		}
		MagicSpells.scheduleDelayedTask(() -> openMenu(caster, opener, entityTarget, locTarget, power, args), delay);
	}

	private void openMenu(Player caster, Player opener, LivingEntity entityTarget, Location locTarget, float power, String[] args) {
		castPower.put(opener.getUniqueId(), power);
		if (requireEntityTarget && entityTarget != null) castEntityTarget.put(opener.getUniqueId(), entityTarget);
		if (requireLocationTarget && locTarget != null) castLocTarget.put(opener.getUniqueId(), locTarget);


		Inventory inv = Bukkit.createInventory(opener, size, Component.text(internalName));
		applyOptionsToInventory(opener, inv, args);
		opener.openInventory(inv);
		Util.setInventoryTitle(opener, title);

		if (entityTarget != null && caster != null) {
			playSpellEffects(caster, entityTarget);
			return;
		}
		playSpellEffects(EffectPosition.SPECIAL, opener);
		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster);
		if (locTarget != null) playSpellEffects(EffectPosition.TARGET, locTarget);
	}

	private void applyOptionsToInventory(Player opener, Inventory inv, String[] args) {
		// Setup option items.
		for (MenuOption option : options.values()) {
			// Check modifiers.
			if (option.menuOptionModifiers != null) {
				MagicSpellsGenericPlayerEvent event = new MagicSpellsGenericPlayerEvent(opener);
				option.menuOptionModifiers.apply(event);
				if (event.isCancelled()) continue;
			}
			// Select and finalise item to display.
			ItemStack item = (option.item != null ? option.item : option.items.get(Util.getRandomInt(option.items.size()))).clone();
			DataUtil.setString(item, "menuOption", option.menuOptionName);
			item = translateItem(opener, args, item);

			int quantity;
			Variable variable = MagicSpells.getVariableManager().getVariable(option.quantity);
			if (variable == null) quantity = ACFUtil.parseInt(option.quantity, 1);
			else quantity = (int) Math.round(variable.getValue(opener));
			item.setAmount(quantity);

			// Set item for all defined slots.
			for (int slot : option.slots) {
				if (inv.getItem(slot) == null) inv.setItem(slot, item);
			}
		}
		// Fill inventory.
		if (filler == null) return;
		ItemStack item = translateItem(opener, args, filler);
		for (int i = 0; i < inv.getSize(); i++) {
			if (inv.getItem(i) != null) continue;
			inv.setItem(i, item);
		}
	}

	private Component translateRawComponent(Component component, Player player, String[] args) {
		String text = Util.getStringFromComponent(component);
		text = MagicSpells.doArgumentAndVariableSubstitution(text, player, args);
		return Util.getMiniMessage(text);
	}

	private ItemStack translateItem(Player opener, String[] args, ItemStack item) {
		ItemStack newItem = item.clone();
		ItemMeta meta = newItem.getItemMeta();
		if (meta == null) return newItem;
		meta.displayName(translateRawComponent(meta.displayName(), opener, args));
		List<Component> lore = meta.lore();
		if (lore != null) {
			for (int i = 0; i < lore.size(); i++) {
				lore.set(i, translateRawComponent(lore.get(i), opener, args));
			}
			meta.lore(lore);
		}
		newItem.setItemMeta(meta);
		return newItem;
	}

	@EventHandler
	public void onInvClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		if (!Util.getStringFromComponent(event.getView().title()).equals(internalName)) return;
		event.setCancelled(true);

		String closeState = castSpells(player, event.getCurrentItem(), event.getClick());

		UUID id = player.getUniqueId();
		castPower.remove(id);
		castLocTarget.remove(id);
		castEntityTarget.remove(id);

		if (closeState.equals("ignore")) return;
		if (closeState.equals("close")) {
			MagicSpells.scheduleDelayedTask(player::closeInventory, 0);
			return;
		}
		// Reopen.
		Inventory newInv = Bukkit.createInventory(player, event.getView().getTopInventory().getSize(), Component.text(internalName));
		applyOptionsToInventory(player, newInv, MagicSpells.NULL_ARGS);
		player.openInventory(newInv);
		Util.setInventoryTitle(player, title);
	}

	private String castSpells(Player player, ItemStack item, ClickType click) {
		// Outside inventory.
		if (item == null) return stayOpenNonOption ? "ignore" : "close";
		String key = DataUtil.getString(item, "menuOption");
		// Probably a filler or air.
		if (key == null || key.isEmpty() || !options.containsKey(key)) return stayOpenNonOption ? "ignore" : "close";
		MenuOption option = options.get(key);
		return switch (click) {
			case LEFT -> processClickSpell(player, option.spell, option);
			case RIGHT -> processClickSpell(player, option.spellRight, option);
			case MIDDLE -> processClickSpell(player, option.spellMiddle, option);
			case SHIFT_LEFT -> processClickSpell(player, option.spellSneakLeft, option);
			case SHIFT_RIGHT -> processClickSpell(player, option.spellSneakRight, option);
			default -> option.stayOpen ? "ignore" : "close";
		};
	}

	private String processClickSpell(Player player, Subspell spell, MenuOption option) {
		if (spell == null) return option.stayOpen ? "ignore" : "close";
		UUID id = player.getUniqueId();
		float power = option.power;
		if (castPower.containsKey(id)) power *= castPower.get(id);
		if (spell.isTargetedEntitySpell() && castEntityTarget.containsKey(id)) spell.castAtEntity(player, castEntityTarget.get(id), power);
		else if (spell.isTargetedLocationSpell() && castLocTarget.containsKey(id)) spell.castAtLocation(player, castLocTarget.get(id), power);
		else if (bypassNormalCast) spell.cast(player, power);
		else spell.getSpell().cast(player, power, MagicSpells.NULL_ARGS);
		return option.stayOpen ? "reopen" : "close";
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		UUID id = event.getPlayer().getUniqueId();
		castPower.remove(id);
		castLocTarget.remove(id);
		castEntityTarget.remove(id);
	}

	private static class MenuOption {

		private String menuOptionName;
		private List<Integer> slots;
		private ItemStack item;
		private List<ItemStack> items;
		private String quantity;
		private String spellName;
		private String spellRightName;
		private String spellMiddleName;
		private String spellSneakLeftName;
		private String spellSneakRightName;
		private Subspell spell;
		private Subspell spellRight;
		private Subspell spellMiddle;
		private Subspell spellSneakLeft;
		private Subspell spellSneakRight;
		private float power;
		private List<String> modifierList;
		private ModifierSet menuOptionModifiers;
		private boolean stayOpen;

	}

}
