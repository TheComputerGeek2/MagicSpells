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
import com.nisovin.magicspells.handlers.DebugHandler;

import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class HasItemAmountCondition extends OperatorCondition {

	private MagicItemData itemData;

	private ItemStack item;

	private int amount;
	
	@Override
	public boolean initialize(String var) {
		String[] args = var.split(";");
		if (args.length < 2) return false;

		super.initialize(var);

		try {
			amount = Integer.parseInt(args[0].substring(1));
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}

		try {
			MagicItem magicItem = MagicItems.getMagicItemFromString(args[1]);
			if (magicItem == null) return false;

			item = magicItem.getItemStack();
			if (item == null) return false;

			itemData = magicItem.getMagicItemData();
			if (itemData == null) return false;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}

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
		
		if (targetState instanceof InventoryHolder) return check(((InventoryHolder) targetState).getInventory());
		
		return false;
	}

	private boolean check(Inventory inventory) {
		int c = 0;
		for (ItemStack i : inventory.getContents()) {
			if (!isSimilar(i)) continue;
			c += i.getAmount();
		}

		if (equals) return c == amount;
		if (moreThan) return c > amount;
		if (lessThan) return c < amount;
		return false;
	}

	private boolean check(EntityEquipment entityEquipment) {
		int c = 0;
		for (ItemStack i : InventoryUtil.getEquipmentItems(entityEquipment)) {
			if (!isSimilar(i)) continue;
			c += i.getAmount();
		}

		if (equals) return c == amount;
		if (moreThan) return c > amount;
		if (lessThan) return c < amount;
		return false;
	}

	private boolean isSimilar(ItemStack item) {
		if (item == null) return false;

		MagicItemData magicItemData = MagicItems.getMagicItemDataFromItemStack(item);
		if (magicItemData == null) return false;
		if (!magicItemData.equals(itemData)) return false;

		return true;
	}

}
