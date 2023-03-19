package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class LevelCondition extends OperatorCondition {

	private int level = 0;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			level = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}
	
	@Override
	public boolean check(LivingEntity caster) {
		return level(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return level(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean level(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		if (equals) return pl.getLevel() == level;
		else if (moreThan) return pl.getLevel() > level;
		else if (lessThan) return pl.getLevel() < level;
		return false;
	}

}
