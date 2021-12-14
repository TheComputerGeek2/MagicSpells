package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.List;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class RepairSpell extends InstantSpell {

	private static final String REPAIR_SELECTOR_KEY_HELD = "held";
	private static final String REPAIR_SELECTOR_KEY_HOTBAR = "hotbar";
	private static final String REPAIR_SELECTOR_KEY_INVENTORY = "inventory";
	private static final String REPAIR_SELECTOR_KEY_HELMET = "helmet";
	private static final String REPAIR_SELECTOR_KEY_CHESTPLATE = "chestplate";
	private static final String REPAIR_SELECTOR_KEY_LEGGINGS = "leggings";
	private static final String REPAIR_SELECTOR_KEY_BOOTS = "boots";
	
	private ConfigData<Integer> repairAmt;

	private String strNothingToRepair;

	private String[] toRepair;

	private Set<Material> ignoredItems;
	private Set<Material> allowedItems;

	public RepairSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		repairAmt = getConfigDataInt("repair-amount", 300);

		strNothingToRepair = getConfigString("str-nothing-to-repair", "Nothing to repair.");

		List<String> toRepairList = getConfigStringList("to-repair", null);
		if (toRepairList == null) toRepairList = new ArrayList<>();
		if (toRepairList.isEmpty()) toRepairList.add(REPAIR_SELECTOR_KEY_HELD);
		Iterator<String> iter = toRepairList.iterator();
		while (iter.hasNext()) {
			String s = iter.next().toLowerCase();
			if (!s.equals(REPAIR_SELECTOR_KEY_HELD) &&
					!s.equals(REPAIR_SELECTOR_KEY_HOTBAR) &&
					!s.equals(REPAIR_SELECTOR_KEY_INVENTORY) &&
					!s.equals(REPAIR_SELECTOR_KEY_HELMET) &&
					!s.equals(REPAIR_SELECTOR_KEY_CHESTPLATE) &&
					!s.equals(REPAIR_SELECTOR_KEY_LEGGINGS) &&
					!s.equals(REPAIR_SELECTOR_KEY_BOOTS)) {
				MagicSpells.error("RepairSpell '" + internalName + "' has defined an invalid to-repair option: " + s);
				iter.remove();
			}
		}
		toRepair = new String[toRepairList.size()];
		toRepair = toRepairList.toArray(toRepair);
		
		ignoredItems = EnumSet.noneOf(Material.class);
		List<String> list = getConfigStringList("ignore-items", null);
		if (list != null) {
			for (String s : list) {
				Material mat = Util.getMaterial(s);
				if (mat == null) continue;
				ignoredItems.add(mat);
			}
		}
		if (ignoredItems.isEmpty()) ignoredItems = null;
		
		allowedItems = EnumSet.noneOf(Material.class);
		list = getConfigStringList("allowed-items", null);
		if (list != null) {
			for (String s : list) {
				Material mat = Util.getMaterial(s);
				if (mat == null) continue;
				allowedItems.add(mat);
			}
		}
		if (allowedItems.isEmpty()) allowedItems = null;
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player player) {
			boolean repaired = false;
			for (String s : toRepair) {
				if (s.equals(REPAIR_SELECTOR_KEY_HELD)) {
					ItemStack item = player.getInventory().getItemInMainHand();
					item = repair(caster, item, power, args);
					if (item == null) continue;
					repaired = true;
					player.getInventory().setItemInMainHand(item);
					continue;
				}
				
				if (s.equals(REPAIR_SELECTOR_KEY_HOTBAR) || s.equals(REPAIR_SELECTOR_KEY_INVENTORY)) {
					int start;
					int end;
					ItemStack[] items = player.getInventory().getContents();
					if (s.equals(REPAIR_SELECTOR_KEY_HOTBAR)) {
						start = 0; 
						end = 9;
					} else {
						start = 9; 
						end = 36;
					}
					for (int i = start; i < end; i++) {
						ItemStack item = items[i];
						item = repair(caster, item, power, args);
						if (item == null) continue;
						items[i] = item;
						repaired = true;
					}
					player.getInventory().setContents(items);
					continue;
				}

				if (s.equals(REPAIR_SELECTOR_KEY_HELMET)) {
					ItemStack item = player.getInventory().getHelmet();
					item = repair(caster, item, power, args);
					if (item == null) continue;
					repaired = true;
					player.getInventory().setHelmet(item);
					continue;
				}

				if (s.equals(REPAIR_SELECTOR_KEY_CHESTPLATE)) {
					ItemStack item = player.getInventory().getChestplate();
					item = repair(caster, item, power, args);
					if (item == null) continue;
					repaired = true;
					player.getInventory().setChestplate(item);
					continue;
				}

				if (s.equals(REPAIR_SELECTOR_KEY_LEGGINGS)) {
					ItemStack item = player.getInventory().getLeggings();
					item = repair(caster, item, power, args);
					if (item == null) continue;
					repaired = true;
					player.getInventory().setLeggings(item);
					continue;
				}

				if (s.equals(REPAIR_SELECTOR_KEY_BOOTS)) {
					ItemStack item = player.getInventory().getBoots();
					item = repair(caster, item, power, args);
					if (item == null) continue;
					repaired = true;
					player.getInventory().setBoots(item);
				}

			}
			if (!repaired) {
				sendMessage(strNothingToRepair, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private ItemStack repair(LivingEntity caster, ItemStack item, float power, String[] args) {
		if (item == null) return null;
		if (!isRepairable(item)) return null;
		Damageable meta = (Damageable) item.getItemMeta();
		if (meta.getDamage() > 0) {
			meta.setDamage(newDurability(caster, item, power, args));
			item.setItemMeta(meta);
			return item;
		}
		return null;
	}

	private boolean isRepairable(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (!(meta instanceof Damageable)) return false;
		if (ignoredItems != null && ignoredItems.contains(item.getType())) return false;
		return allowedItems == null || allowedItems.contains(item.getType());
	}

	private int newDurability(LivingEntity caster, ItemStack item, float power, String[] args) {
		Damageable meta = (Damageable) item.getItemMeta();

		int durability = meta.getDamage() - repairAmt.get(caster, null, power, args);
		durability = Math.min(durability, item.getType().getMaxDurability());
		durability = Math.max(durability, 0);

		return durability;
	}

	public static String getRepairSelectorKeyHeld() {
		return REPAIR_SELECTOR_KEY_HELD;
	}

	public static String getRepairSelectorKeyHotbar() {
		return REPAIR_SELECTOR_KEY_HOTBAR;
	}

	public static String getRepairSelectorKeyInventory() {
		return REPAIR_SELECTOR_KEY_INVENTORY;
	}

	public static String getRepairSelectorKeyHelmet() {
		return REPAIR_SELECTOR_KEY_HELMET;
	}

	public static String getRepairSelectorKeyChestplate() {
		return REPAIR_SELECTOR_KEY_CHESTPLATE;
	}

	public static String getRepairSelectorKeyLeggings() {
		return REPAIR_SELECTOR_KEY_LEGGINGS;
	}

	public static String getRepairSelectorKeyBoots() {
		return REPAIR_SELECTOR_KEY_BOOTS;
	}

	public String getStrNothingToRepair() {
		return strNothingToRepair;
	}

	public void setStrNothingToRepair(String strNothingToRepair) {
		this.strNothingToRepair = strNothingToRepair;
	}

	public String[] getToRepair() {
		return toRepair;
	}

	public void setToRepair(String[] toRepair) {
		this.toRepair = toRepair;
	}

	public Set<Material> getIgnoredItems() {
		return ignoredItems;
	}

	public Set<Material> getAllowedItems() {
		return allowedItems;
	}

}
