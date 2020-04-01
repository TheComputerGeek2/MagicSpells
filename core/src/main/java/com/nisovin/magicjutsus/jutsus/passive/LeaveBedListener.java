package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerBedLeaveEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// No trigger variable is used here
public class LeaveBedListener extends PassiveListener {

	List<PassiveJutsu> jutsus = new ArrayList<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		jutsus.add(jutsu);
	}
	
	@OverridePriority
	@EventHandler
	public void onDeath(PlayerBedLeaveEvent event) {
		Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
		jutsus.stream().filter(jutsubook::hasJutsu).forEachOrdered(jutsu -> jutsu.activate(event.getPlayer()));
	}

}
