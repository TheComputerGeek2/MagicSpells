package com.nisovin.magicspells.variables.variabletypes;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Bukkit;

import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.util.PlayerNameUtils;

public class PlayerVariable extends Variable {

	private final Map<String, Double> map = new HashMap<>();

	@Override
	public void set(String player, double amount) {
		if (amount > maxValue) amount = maxValue;
		else if (amount < minValue) amount = minValue;
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
