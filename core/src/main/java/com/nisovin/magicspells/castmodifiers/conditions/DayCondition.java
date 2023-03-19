package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class DayCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}
	
	@Override
	public boolean check(LivingEntity caster) {
		return checkTime(caster.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkTime(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return checkTime(location);
	}

	private boolean checkTime(Location location) {
		long time = location.getWorld().getTime();
		return !(time > 13000 && time < 23000);
	}

}
