package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class RiptidingCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isRiptiding(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isRiptiding(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean isRiptiding(LivingEntity target) {
		return target.isRiptiding();
	}

}
