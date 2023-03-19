package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class LastLifeCondition extends OperatorCondition {

	private int time;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			time = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return lifeLength(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return lifeLength(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean lifeLength(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		if (equals) return MagicSpells.getLifeLengthTracker().getLastLifeLength(pl) == time;
		else if (moreThan) return MagicSpells.getLifeLengthTracker().getLastLifeLength(pl) > time;
		else if (lessThan) return MagicSpells.getLifeLengthTracker().getLastLifeLength(pl) < time;
		return false;
	}

}
