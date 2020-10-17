package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class YawCondition extends OperatorCondition {
	
	private float yaw;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.initialize(var);

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
