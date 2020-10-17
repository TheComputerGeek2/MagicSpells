package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.InventoryUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class OpenSlotsCondition extends OperatorCondition {

	private int slots;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.initialize(var);

		try {
			slots = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return openSlots(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return openSlots(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean openSlots(LivingEntity livingEntity) {
		int c = 0;
		ItemStack[] inv;

		if (livingEntity instanceof Player) inv = ((Player) livingEntity).getInventory().getContents();
		else inv = InventoryUtil.getEquipmentItems(livingEntity.getEquipment());

		if (inv == null) return false;

		for (ItemStack itemStack : inv) {
			if (itemStack == null) c++;
		}

		if (equals) return c == slots;
		else if (moreThan) return c > slots;
		else if (lessThan) return c < slots;
		return false;
	}

}
