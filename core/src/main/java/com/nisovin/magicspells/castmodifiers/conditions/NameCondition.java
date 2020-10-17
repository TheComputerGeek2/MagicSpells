package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import com.nisovin.magicspells.util.Util;

import com.nisovin.magicspells.castmodifiers.Condition;

public class NameCondition extends Condition {

	private String name;
	
	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		name = Util.colorize(var);
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return target.getName().equalsIgnoreCase(name);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
