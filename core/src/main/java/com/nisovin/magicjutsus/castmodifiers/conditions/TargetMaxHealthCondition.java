package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.Util;

public class TargetMaxHealthCondition extends OperatorCondition {

	private double health;
	
	@Override
	public boolean setVar(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.setVar(var);

		try {
			health = Double.parseDouble(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return maxHealth(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean maxHealth(LivingEntity livingEntity) {
		if (equals) return Util.getMaxHealth(livingEntity) == health;
		else if (moreThan) return Util.getMaxHealth(livingEntity) > health;
		else if (lessThan) return Util.getMaxHealth(livingEntity) < health;
		return false;
	}

}
