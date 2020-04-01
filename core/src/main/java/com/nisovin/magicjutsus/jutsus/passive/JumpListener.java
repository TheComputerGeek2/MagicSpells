package com.nisovin.magicjutsus.jutsus.passive;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// No trigger variable is currently used
public class JumpListener extends PassiveListener {

	List<PassiveJutsu> jutsus = new ArrayList<>();

	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		jutsus.add(jutsu);
	}

	@OverridePriority
	@EventHandler
	public void onJoin(PlayerStatisticIncrementEvent event) {
		Player player = event.getPlayer();
		if (event.getStatistic() != Statistic.JUMP) return;
		Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
		jutsus.stream().filter(jutsubook::hasJutsu).forEachOrdered(jutsu -> jutsu.activate(player));
	}

}
