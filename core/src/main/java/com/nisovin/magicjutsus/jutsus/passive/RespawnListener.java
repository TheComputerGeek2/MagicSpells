package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// No trigger variable is used here
public class RespawnListener extends PassiveListener {

	List<PassiveJutsu> jutsus = new ArrayList<>();

	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		jutsus.add(jutsu);
	}
	
	@OverridePriority
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		if (jutsus.isEmpty()) return;
		final Player player = event.getPlayer();
		final Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
		MagicJutsus.scheduleDelayedTask(() -> jutsus.stream().filter(jutsubook::hasJutsu).forEachOrdered(jutsu -> jutsu.activate(player)), 1);
	}

}
