package com.nisovin.magicjutsus.variables.meta;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.variables.MetaVariable;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class VelocityXVariable extends MetaVariable {
	
	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getVelocity().getX();
		return 0D;
	}
	
	@Override
	public void set(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Vector velocity = p.getVelocity();
			velocity.setX(amount);
			p.setVelocity(velocity);
		}
	}
	
	@Override
	public boolean modify(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			Vector velocity = p.getVelocity();
			velocity.setX(velocity.getX() + amount);
			p.setVelocity(velocity);
			return true;
		}
		return false;
	}
	
}
