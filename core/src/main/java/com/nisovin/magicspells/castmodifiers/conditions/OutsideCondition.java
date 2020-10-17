package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OutsideCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return livingEntity.getWorld().getHighestBlockYAt(livingEntity.getLocation()) <= livingEntity.getEyeLocation().getY();
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return target.getWorld().getHighestBlockYAt(target.getLocation()) <= target.getEyeLocation().getY();
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return location.getWorld().getHighestBlockYAt(location) <= location.getY();
	}

}
