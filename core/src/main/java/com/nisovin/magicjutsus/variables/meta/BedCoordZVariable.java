package com.nisovin.magicjutsus.variables.meta;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.variables.MetaVariable;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BedCoordZVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getBedSpawnLocation().getZ();
		return 0D;
	}
	
	@Override
	public void set(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Location to = p.getBedSpawnLocation();
			to.setZ(amount);
			p.setBedSpawnLocation(to, true);
		}
	}
	
	@Override
	public boolean modify(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Location to = p.getBedSpawnLocation();
			to.setZ(to.getZ() + amount);
			p.setBedSpawnLocation(to, true);
			return true;
		}
		return false;
	}

}
