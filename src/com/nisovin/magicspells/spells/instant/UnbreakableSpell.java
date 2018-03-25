// unbreakable spell made by JigglyJohn & Rifle D. Luffy
package com.nisovin.magicspells.spells.instant;

import org.apache.commons.lang.ArrayUtils;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.InventoryUtil;

public class UnbreakableSpell extends InstantSpell {

	private static final int hotbarSize = 9;
	private static final int armorSize = 4;

	//Config Checks
	private boolean correctlyConfigured = true;

	private String[] validInventories = new String[]{"mainHand","offHand","wearing","hotbar","inventory"};
	private String inventoryType;
	private boolean toggle;

	public UnbreakableSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		// set the config below to mainHand or offHand
		inventoryType = getConfigString("inventory-type", null);
		toggle = getConfigBoolean("toggle", true);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (inventoryType == null) {
			correctlyConfigured = false;
			MagicSpells.error("Inventory-Type wasn't defined!");
			return;
		}
		if (inventoryType.isEmpty()) {
			MagicSpells.error("Inventory-Type was left empty!");
			correctlyConfigured = false;
		}
		if (!ArrayUtils.contains(validInventories,inventoryType)) {
			MagicSpells.error("Invalid Inventory-Type was defined");
			correctlyConfigured = false;
		}
		if (!correctlyConfigured) MagicSpells.error("UnbreakableSpell " + internalName + " was configured incorrectly!");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (!correctlyConfigured) return PostCastAction.ALREADY_HANDLED;

			PlayerInventory inv = player.getInventory();
			ArrayList<Integer> indexes = new ArrayList<>();
			ArrayList<ItemStack> itemsToModify = new ArrayList<>();
			ItemStack[] wearing = inv.getArmorContents();
			ItemStack item;
			switch (inventoryType) {
				case "mainHand":
					itemsToModify.add(inv.getItemInMainHand());
					break;
				case "offHand":
					itemsToModify.add(inv.getItemInOffHand());
					break;
				case "wearing":
					for (int i = 0; i < armorSize; i++) {
						item = wearing[i];
						itemsToModify.add(item);
					}
					break;
				case "hotbar":
					for (int i = 0; i < hotbarSize; i++) {
						item = inv.getItem(i);
						if (InventoryUtil.isNothing(item)) continue;
						itemsToModify.add(inv.getItem(i));
						indexes.add(i);
					}
					break;
				case "inventory":
					for (int i = 0; i < inv.getSize(); i++) {
						item = inv.getItem(i);
						if (InventoryUtil.isNothing(item)) continue;
						itemsToModify.add(inv.getItem(i));
						indexes.add(i);
					}
					break;
				default:
					MagicSpells.error("Invalid inventory-type defined; Use mainHand, offHand, wearing, hotbar or inventory.");
					throw new IllegalStateException();
			}
			//The spell will not bother with empty items at all.
			if (itemsToModify.size() <= 0) return PostCastAction.ALREADY_HANDLED;
			ItemStack newItem;
			switch (inventoryType) {
				case "mainHand":
					newItem = setUnbreakable(itemsToModify.get(0), toggle);
					inv.setItemInMainHand(newItem);
					break;
				case "offHand":
					newItem = setUnbreakable(itemsToModify.get(0), toggle);
					inv.setItemInOffHand(newItem);
					break;
				case "wearing":
					ItemStack[] wearingModified = new ItemStack[4];
					for (int i = 0; i < itemsToModify.size(); i++) {
						newItem = setUnbreakable(itemsToModify.get(i), toggle);
						wearingModified[i] = newItem;
					}
					inv.setArmorContents(wearingModified);
					break;
				case "hotbar":
					for (int i = 0; i < itemsToModify.size(); i++) {
						newItem = setUnbreakable(itemsToModify.get(i), toggle);
						inv.setItem(indexes.get(i),newItem);
					}
					break;
				case "inventory":
					for (int i = 0; i < itemsToModify.size(); i++) {
						newItem = setUnbreakable(itemsToModify.get(i), toggle);
						inv.setItem(indexes.get(i),newItem);
					}
					break;
				default:
					throw new IllegalStateException();
			}
			player.updateInventory();
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	public ItemStack setUnbreakable(ItemStack item, boolean shouldToggle) {
		ItemMeta meta = item.getItemMeta();
		if (meta.isUnbreakable() && shouldToggle) meta.setUnbreakable(false);
		else meta.setUnbreakable(true);
		item.setItemMeta(meta);
		return item;
	}

}
