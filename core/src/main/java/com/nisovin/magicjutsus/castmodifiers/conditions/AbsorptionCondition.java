package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;

public class AbsorptionCondition extends OperatorCondition {

	private float health = 0;

	@Override
	public boolean setVar(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.setVar(var);

		try {
			health = Float.parseFloat(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return absorption(livingEntity);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return absorption(target);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean absorption(LivingEntity target) {
		if (equals) return MagicJutsus.getVolatileCodeHandler().getAbsorptionHearts(target) == health;
		else if (moreThan) return MagicJutsus.getVolatileCodeHandler().getAbsorptionHearts(target) > health;
		else if (lessThan) return MagicJutsus.getVolatileCodeHandler().getAbsorptionHearts(target) < health;
		return false;
	}
	
}
