package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class GameModeCondition extends Condition {

	private GameMode mode;

	@Override
	public boolean initialize(String var) {
		try {
			mode = GameMode.valueOf(var.toUpperCase());
			return true;
		} catch (IllegalArgumentException e) {
			mode = null;
			DebugHandler.debugIllegalArgumentException(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return gameMode(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return gameMode(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean gameMode(LivingEntity target) {
		if (mode == null) return false;
		if (!(target instanceof Player pl)) return false;
		return pl.getGameMode() == mode;
	}

}
