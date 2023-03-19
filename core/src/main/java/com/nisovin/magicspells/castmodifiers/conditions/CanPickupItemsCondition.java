package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class CanPickupItemsCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return canPickup(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return canPickup(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean canPickup(LivingEntity target) {
		return target.getCanPickupItems();
	}

}
