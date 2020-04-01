package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class ReceivingRedstoneCondition extends OperatorCondition {
	
	private int level = 0;

	@Override
	public boolean setVar(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.setVar(var);

		try {
			level = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return signal(livingEntity.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return signal(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return signal(location);
	}

	private boolean signal(Location location) {
		if (equals) return location.getBlock().getBlockPower() == level;
		else if (moreThan) return location.getBlock().getBlockPower() > level;
		else if (lessThan) return location.getBlock().getBlockPower() < level;
		return false;
	}
	
}
