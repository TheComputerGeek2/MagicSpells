package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// Optional trigger variable of comma separated list of teleport causes to accept
public class TeleportListener extends PassiveListener {

	Map<TeleportCause, List<PassiveJutsu>> types = new HashMap<>();
	List<PassiveJutsu> allTypes = new ArrayList<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			allTypes.add(jutsu);
		} else {
			String[] split = var.replace(" ", "").split(",");
			for (String s : split) {
				s = s.trim().replace("_", "");
				for (TeleportCause cause : TeleportCause.values()) {
					if (cause.name().replace("_", "").equalsIgnoreCase(s)) {
						List<PassiveJutsu> list = types.computeIfAbsent(cause, c -> new ArrayList<>());
						list.add(jutsu);
						break;
					}
				}
			}
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		Player player = event.getPlayer();
		
		if (!allTypes.isEmpty()) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			for (PassiveJutsu jutsu : allTypes) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (jutsubook.hasJutsu(jutsu)) {
					boolean casted = jutsu.activate(player);
					if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
				}
			}
		}
		
		if (!types.isEmpty() && types.containsKey(event.getCause())) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			for (PassiveJutsu jutsu : types.get(event.getCause())) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu)) continue;
				boolean casted = jutsu.activate(player);
				if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
			}
		}
	}

}
