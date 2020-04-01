package com.nisovin.magicjutsus.variables.meta;

import com.nisovin.magicjutsus.util.Util;
import org.bukkit.entity.Player;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.variables.MetaVariable;

public class MaxHealthVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return Util.getMaxHealth(p);
		return 0D;
	}
	
	@Override
	public void set(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) Util.setMaxHealth(p, amount);
	}
	
	@Override
	public boolean modify(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Util.setMaxHealth(p, Util.getMaxHealth(p) + amount);
			return true;
		}
		return false;
	}

}
