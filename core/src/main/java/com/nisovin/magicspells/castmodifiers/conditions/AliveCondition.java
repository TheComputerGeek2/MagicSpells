package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class AliveCondition extends OperatorCondition {

	private int time;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.initialize(var);

		try {
			time = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return alive(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return alive(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean alive(LivingEntity target) {
		if (!(target instanceof Player)) return false;
		if (equals) return MagicSpells.getLifeLengthTracker().getCurrentLifeLength((Player) target) == time;
		else if (moreThan) return MagicSpells.getLifeLengthTracker().getCurrentLifeLength((Player) target) > time;
		else if (lessThan) return MagicSpells.getLifeLengthTracker().getCurrentLifeLength((Player) target) < time;
		return false;
	}

}
