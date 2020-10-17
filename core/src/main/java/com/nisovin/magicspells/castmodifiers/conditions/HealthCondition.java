package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class HealthCondition extends OperatorCondition {

	private int health = 0;
	private boolean percent = false;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.initialize(var);

		String number = var.substring(1);

		try {
			if (number.endsWith("%")) {
				percent = true;
				number = number.replace("%", "");
			}
			health = Integer.parseInt(number);
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return health(livingEntity);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return health(target);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean health(LivingEntity livingEntity) {
		double hp = livingEntity.getHealth();
		double percentHp = hp / Util.getMaxHealth(livingEntity) * 100;
		if (equals) {
			if (percent) return percentHp == health;
			return hp == health;
		} else if (moreThan) {
			if (percent) return percentHp > health;
			return hp > health;
		} else if (lessThan) {
			if (percent) return percentHp < health;
			return hp < health;
		}
		return false;
	}

}
