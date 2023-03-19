package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class AngleCondition extends OperatorCondition {

	private double angle;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			angle = AccurateMath.toRadians(Double.parseDouble(var.substring(1)));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return false;
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return angle(caster.getLocation(), target.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return angle(caster.getLocation(), location);
	}

	private boolean angle(Location from, Location to) {
		Location startLoc = from.clone();
		Location endLoc = to.clone();

		startLoc.setY(0);
		startLoc.setPitch(0);

		endLoc.setY(0);
		endLoc.setPitch(0);

		Vector direction = endLoc.toVector().subtract(startLoc.toVector()).normalize();

		double degrees = direction.angle(endLoc.getDirection());
		return checkAngle(degrees);
	}

	private boolean checkAngle(double degrees) {
		if (equals) return degrees == angle;
		else if (moreThan) return degrees > angle;
		else if (lessThan) return degrees < angle;
		return false;
	}

}
