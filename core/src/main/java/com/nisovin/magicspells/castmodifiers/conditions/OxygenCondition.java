package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class OxygenCondition extends OperatorCondition {
	
	private int oxygen;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.initialize(var);

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

