package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// No trigger variable is used here
public class SprintListener extends PassiveListener {

	List<PassiveJutsu> sprint = null;
	List<PassiveJutsu> stopSprint = null;
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (PassiveTrigger.SPRINT.contains(trigger)) {
			if (sprint == null) sprint = new ArrayList<>();
			sprint.add(jutsu);
		} else if (PassiveTrigger.STOP_SPRINT.contains(trigger)) {
			if (sprint == null) stopSprint = new ArrayList<>();
			stopSprint.add(jutsu);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onSprint(PlayerToggleSprintEvent event) {
		if (event.isSprinting()) {
			if (sprint != null) {
				Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
				for (PassiveJutsu jutsu : sprint) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (jutsubook.hasJutsu(jutsu, false)) {
						boolean casted = jutsu.activate(event.getPlayer());
						if (PassiveListener.cancelDefaultAction(jutsu, casted)) {
							event.setCancelled(true);
						}
					}
				}
			}
		} else {
			if (stopSprint != null) {
				Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
				for (PassiveJutsu jutsu : stopSprint) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (jutsubook.hasJutsu(jutsu, false)) {
						boolean casted = jutsu.activate(event.getPlayer());
						if (PassiveListener.cancelDefaultAction(jutsu, casted)) {
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}

}
