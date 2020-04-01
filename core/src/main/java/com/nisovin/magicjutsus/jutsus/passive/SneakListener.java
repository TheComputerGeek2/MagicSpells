package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// No trigger variable is currently used
public class SneakListener extends PassiveListener {

	List<PassiveJutsu> sneak = null;
	List<PassiveJutsu> stopSneak = null;
		
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (PassiveTrigger.SNEAK.contains(trigger)) {
			if (sneak == null) sneak = new ArrayList<>();
			sneak.add(jutsu);
		} else if (PassiveTrigger.STOP_SNEAK.contains(trigger)) {
			if (stopSneak == null) stopSneak = new ArrayList<>();
			stopSneak.add(jutsu);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event) {
		if (event.isSneaking()) {
			if (sneak != null) {
				Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
				for (PassiveJutsu jutsu : sneak) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (!jutsubook.hasJutsu(jutsu, false)) continue;
					boolean casted = jutsu.activate(event.getPlayer());
					if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
				}
			}
		} else {
			if (stopSneak != null) {
				Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
				for (PassiveJutsu jutsu : stopSneak) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (!jutsubook.hasJutsu(jutsu, false)) continue;
					boolean casted = jutsu.activate(event.getPlayer());
					if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
				}
			}
		}
	}

}
