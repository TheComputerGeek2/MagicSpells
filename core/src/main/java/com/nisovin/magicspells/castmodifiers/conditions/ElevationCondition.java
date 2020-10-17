package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class ElevationCondition extends OperatorCondition {

	private double y;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.initialize(var);

		try {
			y = Double.parseDouble(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return elevation(livingEntity.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return elevation(target.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return elevation(location);
	}

	private boolean elevation(Location location) {
		if (equals) return location.getY() == y;
		else if (moreThan) return location.getY() > y;
		else if (lessThan) return location.getY() < y;
		return false;
	}

}
