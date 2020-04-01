package com.nisovin.magicjutsus.variables.meta;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.variables.MetaVariable;
import org.bukkit.entity.Player;

public class SleepTicksVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getSleepTicks();
		return 0;
	}

}
