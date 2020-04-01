package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.DebugHandler;

public class OxygenCondition extends OperatorCondition{
	
	private int oxygen;
	
	@Override
	public boolean setVar(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.setVar(var);

		try {
			oxygen = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return oxygen(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return oxygen(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean oxygen(LivingEntity livingEntity) {
		if (equals) return livingEntity.getRemainingAir() == oxygen;
		else if (moreThan) return livingEntity.getRemainingAir() > oxygen;
		else if (lessThan) return livingEntity.getRemainingAir() < oxygen;
		return false;
	}
	
}

