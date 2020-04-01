package com.nisovin.magicjutsus.variables.meta;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.variables.MetaVariable;
import org.bukkit.entity.Player;

public class MaximumNoDamageTicksVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getMaximumNoDamageTicks();
		return 0;
	}
	
	@Override
	public void set(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) p.setMaximumNoDamageTicks((int) amount);
	}
	
	@Override
	public boolean modify(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) {
			p.setMaximumNoDamageTicks(p.getMaximumNoDamageTicks() + (int) amount);
			return true;
		}
		return false;
	}

}
