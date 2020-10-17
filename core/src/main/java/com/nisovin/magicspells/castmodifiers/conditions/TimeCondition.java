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
			String[] vardata = var.split("-");
			start = Integer.parseInt(vardata[0]);
			end = Integer.parseInt(vardata[1]);
			return true;
		} catch (Exception e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(target, target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		long time = location.getWorld().getTime();
		if (end >= start) return start <= time && time <= end;
		return time >= start || time <= end;
	}

}
