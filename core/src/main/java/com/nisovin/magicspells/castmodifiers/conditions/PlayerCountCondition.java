package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class PlayerCountCondition extends OperatorCondition {

	private int count;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2 || !super.initialize(var)) return false;

		try {
			count = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return playerCount();
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return playerCount();
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return playerCount();
	}

	private boolean playerCount() {
		if (equals) return Bukkit.getServer().getOnlinePlayers().size() == count;
		else if (moreThan) return Bukkit.getServer().getOnlinePlayers().size() > count;
		else if (lessThan) return Bukkit.getServer().getOnlinePlayers().size() < count;
		return false;
	}

}
