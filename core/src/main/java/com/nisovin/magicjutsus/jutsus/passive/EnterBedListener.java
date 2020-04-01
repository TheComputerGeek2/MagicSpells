package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerBedEnterEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// No trigger variable is currently used
public class EnterBedListener extends PassiveListener {

	List<PassiveJutsu> jutsus = new ArrayList<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		jutsus.add(jutsu);
	}
	
	@OverridePriority
	@EventHandler
	public void onDeath(PlayerBedEnterEvent event) {
		Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
		for (PassiveJutsu jutsu : jutsus) {
			if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
			if (jutsubook.hasJutsu(jutsu)) {
				jutsu.activate(event.getPlayer()); // TODO is this safe to cancel?
			}
		}
	}
	
}
