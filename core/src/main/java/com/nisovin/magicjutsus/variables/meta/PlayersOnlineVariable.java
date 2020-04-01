package com.nisovin.magicjutsus.variables.meta;

import org.bukkit.Bukkit;

import com.nisovin.magicjutsus.variables.MetaVariable;

public class PlayersOnlineVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		return Bukkit.getServer().getOnlinePlayers().size();
	}

}
