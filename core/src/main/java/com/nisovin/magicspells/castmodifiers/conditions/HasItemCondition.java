package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.Damageable;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.InventoryUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class HasItemCondition extends Condition {

	private Material material;
	private short durability;
	private Component name;
	private boolean checkName;

	@Override
	public boolean initialize(String var) {
		try {
			if (var.contains("|")) {
				String[] subVarData = var.split("\\|");
				var = subVarData[0];
				name = Util.getMiniMessage(subVarData[1].replace("__", " "));
				checkName = true;
			} else checkName = false;

			if (var.contains(":")) {
				String[] varData = var.split(":");
				material = Util.getMaterial(varData[0]);
				durability = varData[1].equals("*") ? 0 : Short.parseShort(varData[1]);
			} else material = Util.getMaterial(var);

			return true;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return check(caster, caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		if (target == null) return false;
		if (target instanceof InventoryHolder holder) return checkInventory(holder.getInventory());
		else return checkEquipment(target.getEquipment());
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		Block target = location.getBlock();
		BlockState targetState = target.getState();
		return targetState instanceof InventoryHolder holder && checkInventory(holder.getInventory());
	}

	private boolean checkInventory(Inventory inventory) {
		if (inventory == null) return false;
		if (checkName) {
			for (ItemStack item : inventory.getContents()) {
				if (item == null) continue;
				Component itemName = null;
				try {
					if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) itemName = item.getItemMeta().displayName();
				} catch (Exception e) {
					DebugHandler.debugGeneral(e);
				}
				if (item.getType() == material && (item instanceof Damageable damageable && damageable.getDamage() == durability) && (!checkName || Objects.equals(itemName, name))) return true;
			}

			return false;
		}

		return inventory.contains(material);
	}

	private boolean checkEquipment(EntityEquipment entityEquipment) {
		if (entityEquipment == null) return false;
		ItemStack[] items = InventoryUtil.getEquipmentItems(entityEquipment);

		if (checkName) {
			for (ItemStack item : items) {
				if (item == null) continue;
				Component itemName = null;
				try {
					if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) itemName = item.getItemMeta().displayName();
				} catch (Exception e) {
					DebugHandler.debugGeneral(e);
				}
				if (item.getType() == material && (item instanceof Damageable damageable && damageable.getDamage() == durability) && (!checkName || Objects.equals(name, itemName))) return true;
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
