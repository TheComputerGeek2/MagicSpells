package com.nisovin.magicjutsus.variables;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Bukkit;

import com.nisovin.magicjutsus.util.PlayerNameUtils;

public class PlayerVariable extends Variable {

	private Map<String, Double> map = new HashMap<>();
	
	@Override
	public boolean modify(String player, double amount) {
		double value = getValue(player);
		double newvalue = value + amount;

		if (newvalue > maxValue) newvalue = maxValue;
		else if (newvalue < minValue) newvalue = minValue;

		if (value != newvalue) {
			map.put(player, newvalue);
			if (objective != null) objective.getScore(PlayerNameUtils.getOfflinePlayer(player)).setScore((int) newvalue);
			return true;
		}
		return false;
	}

	@Override
	public void set(String player, double amount) {
		map.put(player, amount);
		if (objective != null) objective.getScore(PlayerNameUtils.getOfflinePlayer(player)).setScore((int) amount);
	}

	@Override
	public double getValue(String player) {
		if (map.containsKey(player)) return map.get(player);
		return defaultValue;
	}

	@Override
	public void reset(String player) {
		map.remove(player);
		if (objective != null) objective.getScore(Bukkit.getOfflinePlayer(player)).setScore((int) defaultValue);
	}

}
