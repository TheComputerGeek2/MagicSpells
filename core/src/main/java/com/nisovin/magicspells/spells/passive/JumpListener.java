package com.nisovin.magicspells.spells.passive;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is currently used
public class JumpListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	public void onJoin(PlayerStatisticIncrementEvent event) {
		Player player = event.getPlayer();
		if (event.getStatistic() != Statistic.JUMP) return;
		if (!hasSpell(player)) return;
		passiveSpell.activate(player);
	}

}
