package com.nisovin.magicspells.variables.meta;

import org.bukkit.util.Vector;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

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
		if (p == null) return;

		Vector velocity = p.getVelocity();
		velocity.setX(amount);
		p.setVelocity(velocity);
	}

}
