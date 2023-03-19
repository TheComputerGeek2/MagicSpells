package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class RainingCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isRaining(caster.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isRaining(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return isRaining(location);
	}

	private boolean isRaining(Location location) {
		return location.getWorld().hasStorm();
	}

}
