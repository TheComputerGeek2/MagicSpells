package com.nisovin.magicjutsus.variables.meta;

import org.bukkit.entity.Player;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.variables.MetaVariable;

public class EntityIDVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getEntityId();
		return 0;
	}

}
