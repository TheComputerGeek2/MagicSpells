package com.nisovin.magicjutsus.castmodifiers.conditions;

import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.InventoryHolder;

import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.InventoryUtil;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class HasItemCondition extends Condition {

	private Material material;
	private short data;
	private String name;
	private boolean checkData;
	private boolean checkName;

	@Override
	public boolean setVar(String var) {
		try {
			if (var.contains("|")) {
				String[] subvardata = var.split("\\|");
				var = subvardata[0];
				name = ChatColor.translateAlternateColorCodes('&', subvardata[1]).replace("__", " ");
				if (name.isEmpty()) name = null;
				checkName = true;
			} else {
				name = null;
				checkName = false;
			}
			if (var.contains(":")) {
				String[] vardata = var.split(":");
				material = Material.matchMaterial(vardata[0], true);
				if (vardata[1].equals("*")) {
					data = 0;
					checkData = false;
				} else {
					data = Short.parseShort(vardata[1]);
					checkData = true;
				}
			} else {
				material = Material.matchMaterial(var, true);
				checkData = false;
			}
			return true;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		if (target == null) return false;
		if (target instanceof InventoryHolder) return check(((InventoryHolder) target).getInventory());
		else return check(target.getEquipment());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		Block target = location.getBlock();
		if (target == null) return false;
		
		BlockState targetState = target.getState();
		if (targetState == null) return false;
		return targetState instanceof InventoryHolder && check(((InventoryHolder) targetState).getInventory());
	}

	private boolean check(Inventory inventory) {
		if (inventory == null) return false;
		if (checkData || checkName) {
			for (ItemStack item : inventory.getContents()) {
				if (item == null) continue;
				String thisname = null;
				try {
					if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) thisname = item.getItemMeta().getDisplayName();
				} catch (Exception e) {
					DebugHandler.debugGeneral(e);
				}
				if (item.getType() == material && (!checkData || item.getDurability() == data) && (!checkName || Objects.equals(thisname, name))) return true;
			}

			return false;
		}

		return inventory.contains(material);
	}

	private boolean check(EntityEquipment entityEquipment) {
		if (entityEquipment == null) return false;
		ItemStack[] items = InventoryUtil.getEquipmentItems(entityEquipment);

		if (checkData || checkName) {
			for (ItemStack item : items) {
				if (item == null) continue;
				String name = null;
				try {
					if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) name = item.getItemMeta().getDisplayName();
				} catch (Exception e) {
					DebugHandler.debugGeneral(e);
				}
				if (item.getType() == material && (!checkData || item.getDurability() == data) && (!checkName || Objects.equals(name, name))) return true;
			}

			return false;
		}

		for (ItemStack i : items) {
			if (i == null) continue;
			return i.getType() == material;
		}

		return false;
	}

}
