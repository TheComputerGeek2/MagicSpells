package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class AgeCondition extends Condition {
	
	private boolean passBaby = false;
	private boolean passAdult = false;
	
	@Override
	public boolean initialize(String var) {
		if (var != null) {
			if (var.equalsIgnoreCase("baby")) {
				passBaby = true;
				return true;
			} else if (var.equalsIgnoreCase("adult")) {
				passAdult = true;
				return true;
			}
		}
		passBaby = true;
		passAdult = true;
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		check(livingEntity, livingEntity);
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		if (target instanceof Ageable) {
			boolean adult = ((Ageable) target).isAdult();
			return adult ? passAdult : passBaby;
		}
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
