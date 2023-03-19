package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class FallingCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isFalling(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isFalling(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean isFalling(LivingEntity target) {
		return target.getFallDistance() > 0;
	}

}
