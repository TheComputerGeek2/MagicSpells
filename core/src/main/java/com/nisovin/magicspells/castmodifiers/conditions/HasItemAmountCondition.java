package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.EntityEquipment;

import com.nisovin.magicspells.util.InventoryUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class HasItemAmountCondition extends OperatorCondition {

	private MagicItemData itemData;
	private int amount;
	
	@Override
	public boolean initialize(String var) {
		String[] args = var.split(";");
		if (args.length < 2) return false;

		if (!super.initialize(var)) return false;

		try {
			amount = Integer.parseInt(args[0].substring(1));
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}

		itemData = MagicItems.getMagicItemDataFromString(args[1]);
		return itemData != null;
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
		BlockState targetState = location.getBlock().getState();
		return targetState instanceof InventoryHolder holder && checkInventory(holder.getInventory());
	}

	private boolean checkInventory(Inventory inventory) {
		int c = 0;
		for (ItemStack i : inventory.getContents()) {
			if (!isSimilar(i)) continue;
			c += i.getAmount();

			if (moreThan && c > amount) return true;
			if (lessThan && c >= amount) return false;
		}

		if (equals) return c == amount;
		if (moreThan) return c > amount;
		if (lessThan) return c < amount;
		return false;
	}

	private boolean checkEquipment(EntityEquipment entityEquipment) {
		int c = 0;
		for (ItemStack i : InventoryUtil.getEquipmentItems(entityEquipment)) {
			if (!isSimilar(i)) continue;
			c += i.getAmount();

			if (moreThan && c > amount) return true;
			if (lessThan && c >= amount) return false;
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

		return itemData.matches(magicItemData);
	}

}
