package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class YawAboveCondition extends Condition{
	
	float yaw;
	
	@Override
	public boolean setVar(String var) {
		try {
			yaw = Float.parseFloat(var);
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		return player.getLocation().getYaw() >= yaw;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return target.getLocation().getYaw() >= yaw;
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}
}
