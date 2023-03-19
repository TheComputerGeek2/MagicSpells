package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class LightLevelCondition extends OperatorCondition {

	private byte level = 0;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			level = Byte.parseByte(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}
	
	@Override
	public boolean check(LivingEntity caster) {
		return lightLevel(caster.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return lightLevel(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return lightLevel(location);
	}

	private boolean lightLevel(Location location) {
		if (equals) return location.getBlock().getLightLevel() == level;
		else if (moreThan) return location.getBlock().getLightLevel() > level;
		else if (lessThan) return location.getBlock().getLightLevel() < level;
		return false;
	}

}
