package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class NightCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return night(caster.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return night(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return night(location);
	}

	private boolean night(Location location) {
		long time = location.getWorld().getTime();
		return time > 13000 && time < 23000;
	}
	
}
