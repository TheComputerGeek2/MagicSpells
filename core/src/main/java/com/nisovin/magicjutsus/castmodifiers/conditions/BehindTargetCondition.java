package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.castmodifiers.Condition;

public class BehindTargetCondition extends Condition {

	@Override
	public boolean setVar(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		float targetFaceAngle = target.getLocation().getDirection().angle(new Vector(0, 0, 0));
		float diffAngle = target.getLocation().toVector().subtract(livingEntity.getLocation().toVector()).angle(new Vector(0, 0, 0));
		float diff = Math.abs(targetFaceAngle - diffAngle);
		return diff >= 160 && diff <= 200;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
