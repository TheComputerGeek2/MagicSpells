package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class RotationCondition extends OperatorCondition {

	private float rotation;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.initialize(var);

		try {
			rotation = Float.parseFloat(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return rotation(livingEntity.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return rotation(target.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return rotation(location);
	}

	private boolean rotation(Location location) {
		float yaw = location.getYaw();
		if (yaw < 0) yaw += 360;

		if (equals) return yaw == rotation;
		else if (moreThan) return yaw > rotation;
		else if (lessThan) return yaw < rotation;
		return false;
	}

}
