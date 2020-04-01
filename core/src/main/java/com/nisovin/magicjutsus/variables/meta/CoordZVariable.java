package com.nisovin.magicjutsus.variables.meta;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.variables.MetaVariable;

public class CoordZVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getLocation().getZ();
		return 0D;
	}
	
	@Override
	public void set(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Location to = p.getLocation();
			to.setZ(amount);
			p.teleport(to, TeleportCause.PLUGIN);
		}
	}
	
	@Override
	public boolean modify(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Location to = p.getLocation();
			to.setZ(to.getZ() + amount);
			p.teleport(to, TeleportCause.PLUGIN);
			return true;
		}
		return false;
	}

}
