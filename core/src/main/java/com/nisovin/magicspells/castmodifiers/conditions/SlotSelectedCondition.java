package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class SlotSelectedCondition extends OperatorCondition {

	private int slot;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			slot = Integer.parseInt(var.substring(1));
			return slot >= 0 && slot <= 8;
		} catch (NumberFormatException exception) {
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return slot(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return slot(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean slot(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		int theirSlot = pl.getInventory().getHeldItemSlot();

		if (equals) return theirSlot == slot;
		else if (moreThan) return theirSlot > slot;
		else if (lessThan) return theirSlot < slot;
		return false;
	}

}
