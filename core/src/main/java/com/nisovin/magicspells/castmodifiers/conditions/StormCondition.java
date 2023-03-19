package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class StormCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return stormy(caster.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return stormy(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return stormy(location);
	}

	private boolean stormy(Location location) {
		return location.getWorld().hasStorm() || location.getWorld().isThundering();
	}

}
