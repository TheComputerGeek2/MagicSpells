package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

import org.apache.commons.math3.util.FastMath;

public class AngleCondition extends OperatorCondition {

	private double angle;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) return false;

		super.initialize(var);

		try {
			angle = Double.parseDouble(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		Vector dir = livingEntity.getLocation().toVector().subtract(target.getLocation().toVector());
		Vector facing = target.getLocation().getDirection();

		double degrees = FastMath.round(FastMath.toDegrees(FastMath.abs(dir.angle(facing))));

		return checkAngle(degrees);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean checkAngle(double degrees) {
		if (equals) return degrees == angle;
		else if (moreThan) return degrees > angle;
		else if (lessThan) return degrees < angle;
		return false;
	}

}
