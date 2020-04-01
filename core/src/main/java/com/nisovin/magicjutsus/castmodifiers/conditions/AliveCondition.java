package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;

public class AliveCondition extends OperatorCondition {

	private int time;

	@Override
	public boolean setVar(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.setVar(var);

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
		if (equals) return MagicJutsus.getLifeLengthTracker().getCurrentLifeLength((Player) target) == time;
		else if (moreThan) return MagicJutsus.getLifeLengthTracker().getCurrentLifeLength((Player) target) > time;
		else if (lessThan) return MagicJutsus.getLifeLengthTracker().getCurrentLifeLength((Player) target) < time;
		return false;
	}

}
