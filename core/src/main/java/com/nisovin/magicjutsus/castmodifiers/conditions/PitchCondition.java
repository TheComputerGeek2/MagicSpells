package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.DebugHandler;

public class PitchCondition extends OperatorCondition {

	private float pitch;
	
	@Override
	public boolean setVar(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.setVar(var);

		try {
			pitch = Float.parseFloat(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return pitch(livingEntity.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return pitch(target.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return pitch(location);
	}

	private boolean pitch(Location location) {
		if (equals) return location.getPitch() == pitch;
		else if (moreThan) return location.getPitch() > pitch;
		else if (lessThan) return location.getPitch() < pitch;
		return false;
	}

}
