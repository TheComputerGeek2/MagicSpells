package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class InWorldCondition extends Condition {

	private String world = "";

	@Override
	public boolean initialize(String var) {
		world = var;
		return true;
	}
	
	@Override
	public boolean check(LivingEntity caster) {
		return checkWorld(caster.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkWorld(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return checkWorld(location);
	}

	private boolean checkWorld(Location location) {
		return location.getWorld().getName().equalsIgnoreCase(world);
	}

}
