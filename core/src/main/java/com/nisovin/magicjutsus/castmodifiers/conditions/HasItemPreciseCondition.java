package com.nisovin.magicjutsus.castmodifiers.conditions;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.EntityEquipment;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.InventoryUtil;
import com.nisovin.magicjutsus.castmodifiers.Condition;

// Only accepts predefined items and uses a much stricter match
public class HasItemPreciseCondition extends Condition {

	private ItemStack itemStack = null;
	
	@Override
	public boolean setVar(String var) {
		var = var.trim();
		ItemStack item = Util.predefinedItems.get(var.trim());
		if (InventoryUtil.isNothing(item)) return false;
		this.itemStack = item.clone();
		return true;
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
		return targetState instanceof InventoryHolder && check(((InventoryHolder)targetState).getInventory());
	}

	private boolean check(Inventory inventory) {
		if (inventory == null) return false;
		return Arrays.stream(inventory.getContents())
				.filter(item -> !InventoryUtil.isNothing(item))
				.anyMatch(item -> itemStack.isSimilar(item));
	}

	private boolean check(EntityEquipment entityEquipment) {
		if (entityEquipment == null) return false;
		return Arrays.stream(InventoryUtil.getEquipmentItems(entityEquipment))
				.filter(item -> !InventoryUtil.isNothing(item))
				.anyMatch(item -> itemStack.isSimilar(item));
	}

}
