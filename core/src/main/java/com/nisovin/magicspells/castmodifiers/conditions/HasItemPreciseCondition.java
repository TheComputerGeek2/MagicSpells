package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.EntityEquipment;

import com.nisovin.magicspells.util.InventoryUtil;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

// Only accepts magic items and uses a much stricter match
public class HasItemPreciseCondition extends Condition {

	private MagicItemData itemData = null;
	
	@Override
	public boolean initialize(String var) {
		MagicItem magicItem = MagicItems.getMagicItemFromString(var.trim());
		if (magicItem == null) return false;

		itemData = magicItem.getMagicItemData();
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
		return targetState instanceof InventoryHolder && check(((InventoryHolder) targetState).getInventory());
	}

	private boolean check(Inventory inventory) {
		if (inventory == null) return false;

		boolean found = false;
		for (ItemStack itemStack : inventory.getContents()) {
			MagicItemData data = MagicItems.getMagicItemDataFromItemStack(itemStack);
			if (data == null) continue;
			if (data.equals(itemData)) found = true;
		}

		return found;
	}

	private boolean check(EntityEquipment entityEquipment) {
		if (entityEquipment == null) return false;

		boolean found = false;
		for (ItemStack itemStack : InventoryUtil.getEquipmentItems(entityEquipment)) {
			MagicItemData data = MagicItems.getMagicItemDataFromItemStack(itemStack);
			if (data == null) continue;
			if (data.equals(itemData)) found = true;
		}

		return found;
	}

}
