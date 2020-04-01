package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// No trigger variable is currently used
public class QuitListener extends PassiveListener {

	List<PassiveJutsu> jutsus = new ArrayList<>();

	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		jutsus.add(jutsu);
	}
	
	@OverridePriority
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
		jutsus.stream().filter(jutsubook::hasJutsu).forEachOrdered(jutsu -> jutsu.activate(player));
	}

}
