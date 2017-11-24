package com.nisovin.magicspells.castmodifiers.conditions;

import com.nisovin.magicspells.castmodifiers.Condition;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ReceivingRSStrongerThanCondition extends Condition {
	
	private int level = 0;
	
	@Override
	public boolean setVar(String var) {
		try {
			level = Integer.parseInt(var);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	@Override
	public boolean check(Player player) {
		// LivingEntities don't receive redstone signals
		return false;
	}
	
	@Override
	public boolean check(Player player, LivingEntity target) {
		// LivingEntities don't receive redstone signals
		return false;
	}
	
	@Override
	public boolean check(Player player, Location location) {
		return location.getBlock().getBlockPower() > level;
	}
	
}
