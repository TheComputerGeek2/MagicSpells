package com.nisovin.magicjutsus.jutsus.passive;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityToggleGlideEvent;

import java.util.ArrayList;
import java.util.List;

// No trigger variable is currently used
public class GlideListener extends PassiveListener {

	List<PassiveJutsu> glide = null;
	List<PassiveJutsu> stopGlide = null;
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (PassiveTrigger.START_GLIDE.contains(trigger)) {
			if (glide == null) glide = new ArrayList<>();
			glide.add(jutsu);
		} else if (PassiveTrigger.STOP_GLIDE.contains(trigger)) {
			if (stopGlide == null) stopGlide = new ArrayList<>();
			stopGlide.add(jutsu);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onGlide(EntityToggleGlideEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof Player)) return;
		Player player = (Player) entity;
		if (event.isGliding()) {
			if (glide != null) {
				Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
				for (PassiveJutsu jutsu : glide) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (!jutsubook.hasJutsu(jutsu, false)) continue;
					boolean casted = jutsu.activate(player);
					if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
					event.setCancelled(true);
				}
			}
		} else {
			if (stopGlide != null) {
				Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
				for (PassiveJutsu jutsu : stopGlide) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (!jutsubook.hasJutsu(jutsu, false)) continue;
					boolean casted = jutsu.activate(player);
					if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
					event.setCancelled(true);
				}
			}
		}
	}

}
