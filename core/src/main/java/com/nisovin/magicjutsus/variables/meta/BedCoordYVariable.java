package com.nisovin.magicjutsus.variables.meta;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.variables.MetaVariable;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BedCoordYVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getBedSpawnLocation().getY();
		return 0D;
	}
	
	@Override
	public void set(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Location to = p.getBedSpawnLocation();
			to.setY(amount);
			p.setBedSpawnLocation(to, true);
		}
	}
	
	@Override
	public boolean modify(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Location to = p.getBedSpawnLocation();
			to.setY(to.getY() + amount);
			p.setBedSpawnLocation(to, true);
			return true;
		}
		return false;
	}

}
