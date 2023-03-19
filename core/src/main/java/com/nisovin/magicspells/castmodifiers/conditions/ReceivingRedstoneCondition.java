package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class ReceivingRedstoneCondition extends OperatorCondition {
	
	private int level = 0;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			level = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	@Override
	public boolean check(LivingEntity caster) {
		return signal(caster.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return signal(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return signal(location);
	}

	private boolean signal(Location location) {
		if (equals) return location.getBlock().getBlockPower() == level;
		else if (moreThan) return location.getBlock().getBlockPower() > level;
		else if (lessThan) return location.getBlock().getBlockPower() < level;
		return false;
	}
	
}
