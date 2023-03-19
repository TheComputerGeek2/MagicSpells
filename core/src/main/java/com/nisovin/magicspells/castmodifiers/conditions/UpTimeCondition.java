package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class UpTimeCondition extends Condition {

	private static final long startTime = System.currentTimeMillis();
	
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
	public boolean check(LivingEntity caster) {
		return checkUptime();
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkUptime();
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return checkUptime();
	}

	private boolean checkUptime() {
		return System.currentTimeMillis() > startTime + ms;
	}

}
