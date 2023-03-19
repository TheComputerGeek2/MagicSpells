package com.nisovin.magicspells.variables.meta;

import org.bukkit.entity.Player;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public abstract class WorldVariable extends MetaVariable {

	@Override
	public String getStringValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getLocation().getWorld().getName();
		return null;
	}

}
