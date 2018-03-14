package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.List;
import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.HandHandler;
import com.nisovin.magicspells.util.InventoryUtil;

public class ItemHasLoreCondition extends Condition {

	String typeOfCheck;
	String loreToCheck;

	@Override
	public boolean setVar(String var) {
		try {
			String[] condition = var.split(";", 2);
			typeOfCheck = condition[1];
			loreToCheck = condition[0];

			switch (typeOfCheck) {
				case "mainhand":
				case "offhand":
				case "inventory":
				case "wearing":
					break;

				default:
					MagicSpells.error("Invalid Check-Type was specified within modifier condition.");
					MagicSpells.error("mainhand, offhand, inventory, wearing are the only valid ones.");
					return false;
			}
			return true;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		PlayerInventory inv = player.getInventory();
		ItemStack[] contents = inv.getContents();
		ItemStack[] equipment = inv.getArmorContents();
		ItemStack mainHand = HandHandler.getItemInMainHand(player);
		ItemStack offHand = HandHandler.getItemInOffHand(player);

		boolean found;

		switch (typeOfCheck) {

			case "mainhand":
				found = check((ItemStack)mainHand);
				if (found) return true;
				break;

			case "offhand":
				found = check((ItemStack)offHand);
				if (found) return true;
				break;

			case "inventory":
				for (ItemStack item : contents) {
					found = check((ItemStack)item);
					if (found) return true;
				}
				break;

			case "wearing":
				for (ItemStack gear : equipment) {
					found = check((ItemStack)gear);
					if (found) return true;
				}
				break;

			default:
				throw new IllegalStateException("Invalid typeOfCheck was used.");
				break;
		}
		//End of Switch
		return false;
	}

	@Override
	private boolean check(Player player, InventoryHolder target) {
		Inventory inv = target.getInventory;
		if (inv == null) return false;

		ItemStack[] contents = inv.getContents();

		boolean found;

		switch (typeOfCheck) {

			case "inventory":
				for (ItemStack item : contents) {
					found = check((ItemStack)item);
					if (found) return true;
				}
				break;

			default:
				throw new IllegalStateException("Invalid typeOfCheck was used.");
				break;
		}
		return false;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		//Wait, the target is a player? Use the Player version of check();
		if (target instanceof Player) return check((Player)target);
		if (target instanceof InventoryHolder) return check((InventoryHolder)target);

		EntityEquipment equip = target.getEquipment();
		if (equip == null) return false;

		ItemStack[] wearing = equip.getArmorContents();
		ItemStack mainHand = HandHandler.getItemInMainHand(equip);
		ItemStack offHand = HandHandler.getItemInOffHand(equip);

		boolean found;

		switch (typeOfCheck) {

			case "mainhand":
				found = check((ItemStack)mainHand);
				if (found) return true;
				break;

			case "offhand":
				found = check((ItemStack)offHand);
				if (found) return true;
				break;

			case "wearing":
				for (ItemStack gear : wearing) {
					found = check((ItemStack)offHand);
					if (found) return true;
				}
				break;
			//You can't check a mob's inventory unless they are part of the InventoryHolder Interface.
			case "inventory":
				break;

			default:
				throw new IllegalStateException("Invalid typeOfCheck was used.");
				break;
		}
		return false;
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

	private boolean check(ItemStack item) {
		if (item == null || InventoryUtil.isNothing(item) || !item.hasItemMeta()) return false;

		ItemMeta meta = item.getItemMeta();
		if (!meta.hasLore()) return false;

		List<String> itemLore = meta.getLore();
		for (String lore : itemLore) {
			if (!lore.equals(loreToCheck)) continue;
			return true;
		}
		return false;
	}
}
