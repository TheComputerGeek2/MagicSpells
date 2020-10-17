package com.nisovin.magicspells.factions.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.massivecraft.factions.entity.MPlayer;

import com.nisovin.magicspells.castmodifiers.Condition;

public class PowerEqualsCondition extends Condition {

	private double power;
	
	@Override
	public boolean initialize(String var) {
		try {
			power = Double.parseDouble(var);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return livingEntity != null && livingEntity instanceof Player && MPlayer.get(livingEntity).getPower() == power;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return target != null && target instanceof Player && MPlayer.get(target).getPower() == power;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
