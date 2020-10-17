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
	public boolean check(LivingEntity livingEntity) {
		return livingEntity.isSwimming();
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return target.isSwimming();
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
