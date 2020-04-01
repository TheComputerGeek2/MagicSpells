package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class YawCondition extends OperatorCondition {
	
	private float yaw;
	
	@Override
	public boolean setVar(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.setVar(var);

		try {
			yaw = Float.parseFloat(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return yaw(livingEntity.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return yaw(target.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return yaw(location);
	}

	private boolean yaw(Location location) {
		if (equals) return location.getYaw() == yaw;
		else if (moreThan) return location.getYaw() > yaw;
		else if (lessThan) return location.getYaw() < yaw;
		return false;
	}

}
