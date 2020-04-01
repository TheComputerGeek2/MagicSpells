package com.nisovin.magicjutsus.jutsus.instant;

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

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class RepairJutsu extends InstantJutsu {

	private static final String REPAIR_SELECTOR_KEY_HELD = "held";
	private static final String REPAIR_SELECTOR_KEY_HOTBAR = "hotbar";
	private static final String REPAIR_SELECTOR_KEY_INVENTORY = "inventory";
	private static final String REPAIR_SELECTOR_KEY_HELMET = "helmet";
	private static final String REPAIR_SELECTOR_KEY_CHESTPLATE = "chestplate";
	private static final String REPAIR_SELECTOR_KEY_LEGGINGS = "leggings";
	private static final String REPAIR_SELECTOR_KEY_BOOTS = "boots";
	
	private int repairAmt;

	private String strNothingToRepair;

	private String[] toRepair;

	private Set<Material> ignoreItems;
	private Set<Material> allowedItems;

	public RepairJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		repairAmt = getConfigInt("repair-amount", 300);

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
				MagicJutsus.error("RepairJutsu '" + internalName + "' has defined an invalid to-repair option: " + s);
				iter.remove();
			}
		}
		toRepair = new String[toRepairList.size()];
		toRepair = toRepairList.toArray(toRepair);
		
		ignoreItems = EnumSet.noneOf(Material.class);
		List<String> list = getConfigStringList("ignore-items", null);
		if (list != null) {
			for (String s : list) {
				Material mat = Material.getMaterial(s.toUpperCase());
				if (mat == null) continue;
				ignoreItems.add(mat);
			}
		}
		if (ignoreItems.isEmpty()) ignoreItems = null;
		
		allowedItems = EnumSet.noneOf(Material.class);
		list = getConfigStringList("allowed-items", null);
		if (list != null) {
			for (String s : list) {
				Material mat = Material.getMaterial(s.toUpperCase());
				if (mat == null) continue;
				allowedItems.add(mat);
			}
		}
		if (allowedItems.isEmpty()) allowedItems = null;
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			boolean repaired = false;
			for (String s : toRepair) {
				if (s.equals(REPAIR_SELECTOR_KEY_HELD)) {
					ItemStack item = player.getInventory().getItemInMainHand();
					item = repair(item);
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
						item = repair(item);
						if (item == null) continue;
						items[i] = item;
						repaired = true;
					}
					player.getInventory().setContents(items);
					continue;
				}

				if (s.equals(REPAIR_SELECTOR_KEY_HELMET)) {
					ItemStack item = player.getInventory().getHelmet();
					item = repair(item);
					if (item == null) continue;
					repaired = true;
					player.getInventory().setHelmet(item);
					continue;
				}

				if (s.equals(REPAIR_SELECTOR_KEY_CHESTPLATE)) {
					ItemStack item = player.getInventory().getChestplate();
					item = repair(item);
					if (item == null) continue;
					repaired = true;
					player.getInventory().setChestplate(item);
					continue;
				}

				if (s.equals(REPAIR_SELECTOR_KEY_LEGGINGS)) {
					ItemStack item = player.getInventory().getLeggings();
					item = repair(item);
					if (item == null) continue;
					repaired = true;
					player.getInventory().setLeggings(item);
					continue;
				}

				if (s.equals(REPAIR_SELECTOR_KEY_BOOTS)) {
					ItemStack item = player.getInventory().getBoots();
					item = repair(item);
					if (item == null) continue;
					repaired = true;
					player.getInventory().setBoots(item);
				}

			}
			if (!repaired) {
				sendMessage(strNothingToRepair, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			playJutsuEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private ItemStack repair(ItemStack item) {
		if (item == null) return null;
		if (!isRepairable(item)) return null;
		Damageable meta = (Damageable) item.getItemMeta();
		if (meta.getDamage() > 0) {
			meta.setDamage(newDurability(item));
			item.setItemMeta((ItemMeta) meta);
			return item;
		}
		return null;
	}

	private boolean isRepairable(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (!(meta instanceof Damageable)) return false;
		if (ignoreItems != null && ignoreItems.contains(item.getType())) return false;
		if (allowedItems != null && !allowedItems.contains(item.getType())) return false;
		return true;
	}

	private int newDurability(ItemStack item) {
		Damageable meta = (Damageable) item.getItemMeta();
		int durability = meta.getDamage();
		durability -= repairAmt;
		if (durability < 0) durability = 0;
		return durability;
	}

}
