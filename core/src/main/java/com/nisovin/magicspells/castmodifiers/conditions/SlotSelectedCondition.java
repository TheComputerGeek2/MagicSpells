
package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class SlotSelectedCondition extends OperatorCondition {

	private int slot;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.length() < 2) return false;
		if (!super.initialize(var)) return false;

		try {
			slot = Integer.parseInt(var.substring(1));
			return slot >= 0 && slot <= 8;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		if (!(livingEntity instanceof Player)) return false;
		int theirSlot = ((Player) livingEntity).getInventory().getHeldItemSlot();
		if (equals) return theirSlot == slot;
		else if (moreThan) return theirSlot > slot;
		else if (lessThan) return theirSlot < slot;
		return false;
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return check(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

}