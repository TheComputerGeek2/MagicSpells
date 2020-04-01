package com.nisovin.magicjutsus.variables.meta;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.variables.MetaVariable;

public class CompassTargetYVariable extends MetaVariable {
	
	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getCompassTarget().getY();
		return 0D;
	}
	
	@Override
	public void set(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Location to = p.getCompassTarget();
			to.setY((float)amount);
			p.setCompassTarget(to);
		}
	}
	
	@Override
	public boolean modify(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Location to = p.getCompassTarget();
			to.setY((float) (to.getY() + amount));
			p.setCompassTarget(to);
			return true;
		}
		return false;
	}
	
}
