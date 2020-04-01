package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class LightLevelCondition extends OperatorCondition {

	private byte level = 0;

	@Override
	public boolean setVar(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.setVar(var);

		try {
			level = Byte.parseByte(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return lightLevel(livingEntity.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return lightLevel(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return lightLevel(location);
	}

	private boolean lightLevel(Location location) {
		if (equals) return location.getBlock().getLightLevel() == level;
		else if (moreThan) return location.getBlock().getLightLevel() > level;
		else if (lessThan) return location.getBlock().getLightLevel() < level;
		return false;
	}

}
