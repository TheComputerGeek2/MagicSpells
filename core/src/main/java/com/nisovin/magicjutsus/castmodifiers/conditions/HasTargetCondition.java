package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.castmodifiers.Condition;

public class HasTargetCondition extends Condition {

	@Override
	public boolean setVar(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return hasTarget(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return hasTarget(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean hasTarget(LivingEntity livingEntity) {
		if (!(livingEntity instanceof Creature)) return false;
		LivingEntity target = ((Creature) livingEntity).getTarget();
		return target != null && target.isValid();
	}

}
