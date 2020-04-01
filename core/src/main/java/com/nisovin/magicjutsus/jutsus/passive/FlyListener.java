package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// No trigger variable is currently used
public class FlyListener extends PassiveListener {

	List<PassiveJutsu> fly = null;
	List<PassiveJutsu> stopFly = null;
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (PassiveTrigger.FLY.contains(trigger)) {
			if (fly == null) fly = new ArrayList<>();
			fly.add(jutsu);
		} else if (PassiveTrigger.STOP_FLY.contains(trigger)) {
			if (stopFly == null) stopFly = new ArrayList<>();
			stopFly.add(jutsu);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onFly(PlayerToggleFlightEvent event) {
		if (event.isFlying()) {
			if (fly != null) {
				Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
				for (PassiveJutsu jutsu : fly) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (!jutsubook.hasJutsu(jutsu, false)) continue;
					boolean casted = jutsu.activate(event.getPlayer());
					if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
					event.setCancelled(true);
				}
			}
		} else {
			if (stopFly != null) {
				Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
				for (PassiveJutsu jutsu : stopFly) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (!jutsubook.hasJutsu(jutsu, false)) continue;
					boolean casted = jutsu.activate(event.getPlayer());
					if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
					event.setCancelled(true);
				}
			}
		}
	}

}
