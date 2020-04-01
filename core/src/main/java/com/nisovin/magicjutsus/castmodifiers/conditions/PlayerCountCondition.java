package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class PlayerCountCondition extends OperatorCondition {

	private int count;

	@Override
	public boolean setVar(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.setVar(var);

		try {
			count = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return playerCount();
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return playerCount();
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return playerCount();
	}

	private boolean playerCount() {
		if (equals) return Bukkit.getServer().getOnlinePlayers().size() == count;
		else if (moreThan) return Bukkit.getServer().getOnlinePlayers().size() > count;
		else if (lessThan) return Bukkit.getServer().getOnlinePlayers().size() < count;
		return false;
	}

}
