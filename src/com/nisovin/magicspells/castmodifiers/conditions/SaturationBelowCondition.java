package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class SaturationBelowCondition extends Condition {

	float saturation;
	
	@Override
	public boolean setVar(String var) {
		try {
			saturation = Float.parseFloat(var);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		return player.getSaturation() < saturation;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return target instanceof Player ? check((Player)target): false;
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
