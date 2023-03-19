package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class TimeCondition extends Condition {

	private int start;
	private int end;

	@Override
	public boolean initialize(String var) {
		try {
			String[] varData = var.split("-");
			start = Integer.parseInt(varData[0]);
			end = Integer.parseInt(varData[1]);
			return true;
		} catch (Exception e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}
	
	@Override
	public boolean check(LivingEntity caster) {
		return time(caster.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return time(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return time(location);
	}

	private boolean time(Location location) {
		long time = location.getWorld().getTime();
		if (end >= start) return start <= time && time <= end;
		return time >= start || time <= end;
	}

}
