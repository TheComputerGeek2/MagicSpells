package com.nisovin.magicjutsus.variables.meta;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.variables.MetaVariable;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BedCoordXVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getBedSpawnLocation().getX();
		return 0D;
	}
	
	@Override
	public void set(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Location to = p.getBedSpawnLocation();
			to.setX(amount);
			p.setBedSpawnLocation(to, true);
		}
	}
	
	@Override
	public boolean modify(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Location to = p.getBedSpawnLocation();
			to.setX(to.getX() + amount);
			p.setBedSpawnLocation(to, true);
			return true;
		}
		return false;
	}

}
