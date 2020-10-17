package com.nisovin.magicspells.spells;

import java.util.*;

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

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.util.PlayerNameUtils;
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
			option.spellSneakRightName = getConfigString(path + "spell-sneak-left", "");
			option.spellSneakLeftName = getConfigString(path + "spell-sneak-right", "");
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
			if (option.modifierList != null) option.menuOptionModifiers = new ModifierSet(option.modifierList);
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		for (MenuOption option : options.values()) {
			option.spell = initSubspell(option.spellName, "MenuSpell '" + internalName + "' has an invalid 'spell' defined for: " + option.menuOptionName);
			option.spellRight = initSubspell(option.spellRightName, "MenuSpell '" + internalName + "' has an invalid 'spell' defined for: " + option.menuOptionName);
			option.spellMiddle = initSubspell(option.spellMiddleName, "MenuSpell '" + internalName + "' has an invalid 'spell' defined for: " + option.menuOptionName);
			option.spellSneakLeft = initSubspell(option.spellSneakLeftName, "MenuSpell '" + internalName + "' has an invalid 'spell' defined for: " + option.menuOptionName);
			option.spellSneakRight = initSubspell(option.spellSneakRightName, "MenuSpell '" + internalName + "' has an invalid 'spell' defined for: " + option.menuOptionName);
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			LivingEntity entityTarget = null;
			Location locTarget = null;
			Player opener = player;

			if (requireEntityTarget) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
				if (targetInfo != null) entityTarget = targetInfo.getTarget();
				if (entityTarget == null) return noTarget(player);
				if (targetOpensMenuInstead) {
					if (!(entityTarget instanceof Player)) return noTarget(player);
					opener = (Player) entityTarget;
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
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (requireEntityTarget && !validTargetList.canTarget(caster, target)) return false;
		if (!(caster instanceof Player)) return false;
		Player opener = (Player) caster;
		if (targetOpensMenuInstead) {
			if (!(target instanceof Player)) return false;
			opener = (Player) target;
			target = null;
		}
		open((Player) caster, opener, target, null, power, MagicSpells.NULL_ARGS);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!targetOpensMenuInstead) return false;
		if (requireEntityTarget && !validTargetList.canTarget(target)) return false;
		if (!(target instanceof Player)) return false;
		open(null, (Player) target, null, null, power, MagicSpells.NULL_ARGS);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		if (!(caster instanceof Player)) return false;
		open((Player) caster, (Player) caster, null, target, power, MagicSpells.NULL_ARGS);
		return true;
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

	private void open(final Player caster, Player opener, LivingEntity entityTarget, Location locTarget, final float power, final String[] args) {
		if (delay < 0) {
			openMenu(caster, opener, entityTarget, locTarget, power, args);
			return;
		}
		final Player p = opener;
		final Location l = locTarget;
		final LivingEntity e = entityTarget;
		MagicSpells.scheduleDelayedTask(() -> openMenu(caster, p, e, l, power, args), delay);
	}

	private void openMenu(Player caster, Player opener, LivingEntity entityTarget, Location locTarget, float power, String[] args) {
		castPower.put(opener.getUniqueId(), power);
		if (requireEntityTarget && entityTarget != null) castEntityTarget.put(opener.getUniqueId(), entityTarget);
		if (requireLocationTarget && locTarget != null) castLocTarget.put(opener.getUniqueId(), locTarget);

		Inventory inv = Bukkit.createInventory(opener, size, internalName);
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
			item = MagicSpells.getVolatileCodeHandler().setNBTString(item, "menuOption", option.menuOptionName);
			item = translateItem(opener, args, item);

			Variable variable = MagicSpells.getVariableManager().getVariable(option.quantity);
			int quantity = 1;
			if (variable != null) quantity = (int) Math.round(variable.getValue(opener));
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

	private ItemStack translateItem(Player opener, String[] args, ItemStack item) {
		ItemStack newItem = item.clone();
		ItemMeta meta = newItem.getItemMeta();
		if (meta == null) return newItem;
		meta.setDisplayName(Util.colorize(MagicSpells.doArgumentAndVariableSubstitution(meta.getDisplayName(), opener, args)));
		List<String> lore = meta.getLore();
		if (lore != null) {
			for (int i = 0; i < lore.size(); i++) {
				lore.set(i, Util.colorize(MagicSpells.doArgumentAndVariableSubstitution(lore.get(i), opener, args)));
			}
			meta.setLore(lore);
		}
		newItem.setItemMeta(meta);
		return newItem;
	}

	@EventHandler
	public void onInvClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		if (!event.getView().getTitle().equals(internalName)) return;
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
		Inventory newInv = Bukkit.createInventory(player, event.getView().getTopInventory().getSize(), internalName);
		applyOptionsToInventory(player, newInv, MagicSpells.NULL_ARGS);
		player.openInventory(newInv);
		Util.setInventoryTitle(player, title);
	}

	private String castSpells(Player player, ItemStack item, ClickType click) {
		// Outside inventory.
		if (item == null) return stayOpenNonOption ? "ignore" : "close";
		String key = MagicSpells.getVolatileCodeHandler().getNBTString(item, "menuOption");
		// Probably a filler or air.
		if (key == null || key.isEmpty() || !options.containsKey(key)) return stayOpenNonOption ? "ignore" : "close";
		MenuOption option = options.get(key);
		switch(click) {
			case LEFT: return processClickSpell(player, option.spell, option);
			case RIGHT: return processClickSpell(player, option.spellRight, option);
			case MIDDLE: return processClickSpell(player, option.spellMiddle, option);
			case SHIFT_LEFT: return processClickSpell(player, option.spellSneakLeft, option);
			case SHIFT_RIGHT: return processClickSpell(player, option.spellSneakRight, option);
			default: return option.stayOpen ? "ignore" : "close";
		}
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
