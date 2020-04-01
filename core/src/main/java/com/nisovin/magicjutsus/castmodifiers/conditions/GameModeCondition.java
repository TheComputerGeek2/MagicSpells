package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class GameModeCondition extends Condition {

	private GameMode mode;

	@Override
	public boolean setVar(String var) {
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
	public boolean check(LivingEntity livingEntity) {
		return gameMode(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return gameMode(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean gameMode(LivingEntity livingEntity) {
		if (mode == null) return false;
		if (!(livingEntity instanceof Player)) return false;
		return ((Player) livingEntity).getGameMode() == mode;
	}

}
