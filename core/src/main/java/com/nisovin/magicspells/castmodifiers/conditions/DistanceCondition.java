package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class DistanceCondition extends OperatorCondition {

	private double distanceSq;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.initialize(var);

		try {
			distanceSq = Double.parseDouble(var.substring(1));
			distanceSq = distanceSq * distanceSq;
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
		return distance(livingEntity.getLocation(), target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return distance(livingEntity.getLocation(), location);
	}

	private boolean distance(Location from, Location to) {
		if (from == null || to == null) return false;
		if (!from.getWorld().equals(to.getWorld())) return false;
		if (equals) return from.distanceSquared(to) == distanceSq;
		else if (moreThan) return from.distanceSquared(to) > distanceSq;
		else if (lessThan) return from.distanceSquared(to) < distanceSq;
		return false;
	}

}
