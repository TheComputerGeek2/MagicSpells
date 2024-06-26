package com.nisovin.magicspells.variables.meta;

import org.bukkit.Bukkit;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class VelocityZVariable extends MetaVariable {
	
	@Override
	public double getValue(String player) {
		Player p = Bukkit.getPlayerExact(player);
		if (p != null) return p.getVelocity().getZ();
		return 0D;
	}
	
	@Override
	public void set(String player, double amount) {
		Player p = Bukkit.getPlayerExact(player);
		if (p == null) return;

		Vector velocity = p.getVelocity();
		velocity.setZ(amount);
		p.setVelocity(velocity);
	}

}
