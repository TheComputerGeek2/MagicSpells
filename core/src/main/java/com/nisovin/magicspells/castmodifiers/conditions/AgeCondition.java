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
	public boolean check(LivingEntity caster) {
		return age(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return age(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean age(LivingEntity target) {
		if (!(target instanceof Ageable t)) return false;
		boolean adult = t.isAdult();
		return adult ? passAdult : passBaby;
	}

}
