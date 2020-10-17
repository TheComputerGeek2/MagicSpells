package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class UpTimeCondition extends Condition {

	private static long startTime = System.currentTimeMillis();
	
	private int ms;
	
	@Override
	public boolean initialize(String var) {
		try {
			ms = Integer.parseInt(var) * (int) TimeUtil.MILLISECONDS_PER_SECOND;
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return System.currentTimeMillis() > startTime + ms;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return System.currentTimeMillis() > startTime + ms;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return System.currentTimeMillis() > startTime + ms;
	}

}
