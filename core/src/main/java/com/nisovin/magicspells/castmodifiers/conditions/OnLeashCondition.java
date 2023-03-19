package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OnLeashCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isLeashed(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isLeashed(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean isLeashed(LivingEntity target) {
		return target.isLeashed();
	}

}
