package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class OxygenCondition extends OperatorCondition {
	
	private int oxygen;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			oxygen = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return oxygen(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return oxygen(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean oxygen(LivingEntity target) {
		if (equals) return target.getRemainingAir() == oxygen;
		else if (moreThan) return target.getRemainingAir() > oxygen;
		else if (lessThan) return target.getRemainingAir() < oxygen;
		return false;
	}
	
}

