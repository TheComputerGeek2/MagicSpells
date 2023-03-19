package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class SwimmingCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isSwimming(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isSwimming(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean isSwimming(LivingEntity target) {
		return target.isSwimming();
	}

}
