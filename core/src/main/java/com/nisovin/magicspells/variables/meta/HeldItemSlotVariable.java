package com.nisovin.magicspells.variables.meta;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class HeldItemSlotVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		Player p = Bukkit.getPlayerExact(player);
		return p != null ? p.getInventory().getHeldItemSlot() : 0;
	}

	@Override
	public void set(String player, double amount) {
		Player p = Bukkit.getPlayerExact(player);
		if (p == null) return;

		try {
			p.getInventory().setHeldItemSlot((int) amount);
		} catch (IllegalArgumentException _) {}
	}

}
