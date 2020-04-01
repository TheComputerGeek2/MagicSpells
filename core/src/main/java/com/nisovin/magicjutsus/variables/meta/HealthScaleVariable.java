package com.nisovin.magicjutsus.variables.meta;

import org.bukkit.entity.Player;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.variables.MetaVariable;

public class HealthScaleVariable extends MetaVariable {
	
	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getHealthScale();
		return 0D;
	}
	
	@Override
	public void set(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) p.setHealthScale(amount);
	}
	
	@Override
	public boolean modify(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			p.setHealthScale(p.getHealthScale() + amount);
			return true;
		}
		return false;
	}
	
}
