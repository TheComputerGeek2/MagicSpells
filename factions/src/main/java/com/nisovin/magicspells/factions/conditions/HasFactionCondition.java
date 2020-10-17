package com.nisovin.magicspells.factions.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.massivecraft.factions.entity.MPlayer;

import com.nisovin.magicspells.castmodifiers.Condition;

public class HasFactionCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return livingEntity != null && livingEntity instanceof Player && MPlayer.get(livingEntity).hasFaction();
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return target != null && target instanceof Player && MPlayer.get(target).hasFaction();
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
