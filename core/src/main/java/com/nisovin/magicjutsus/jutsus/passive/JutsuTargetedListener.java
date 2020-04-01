package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// Optional trigger variable of comma separated list of internal jutsu names to accept
public class JutsuTargetedListener extends PassiveListener {

	Map<Jutsu, List<PassiveJutsu>> jutsus = new HashMap<>();
	List<PassiveJutsu> anyJutsu = new ArrayList<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			anyJutsu.add(jutsu);
		} else {
			String[] split = var.split(",");
			for (String s : split) {
				Jutsu sp = MagicJutsus.getJutsuByInternalName(s.trim());
				if (sp == null) continue;
				List<PassiveJutsu> passives = jutsus.computeIfAbsent(sp, p -> new ArrayList<>());
				passives.add(jutsu);
			}
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onJutsuTarget(JutsuTargetEvent event) {
		if (!(event.getTarget() instanceof Player)) return;
		Player player = (Player)event.getTarget();
		Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
		for (PassiveJutsu jutsu : anyJutsu) {
			if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
			if (!jutsubook.hasJutsu(jutsu, false)) continue;
			boolean casted = jutsu.activate(player, event.getCaster());
			if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
		}
		List<PassiveJutsu> list = jutsus.get(event.getJutsu());
		if (list != null) {
			for (PassiveJutsu jutsu : list) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu, false)) continue;
				boolean casted = jutsu.activate(player, event.getCaster());
				if (PassiveListener.cancelDefaultAction(jutsu, casted)) event.setCancelled(true);
			}
		}
	}

}
