package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;
import com.nisovin.magicjutsus.util.Util;

// Trigger variable is optional
// If not specified, it will trigger on any entity type
// If specified, it should be a comma separated list of entity types to trigger on
public class KillListener extends PassiveListener {

	Map<EntityType, List<PassiveJutsu>> entityTypes = new HashMap<>();
	List<PassiveJutsu> allTypes = new ArrayList<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			allTypes.add(jutsu);
		} else {
			String[] split = var.replace(" ", "").split(",");
			for (String s : split) {
				EntityType t = Util.getEntityType(s);
				if (t == null) continue;
				List<PassiveJutsu> jutsus = entityTypes.computeIfAbsent(t, type -> new ArrayList<>());
				jutsus.add(jutsu);
			}
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onDeath(EntityDeathEvent event) {
		Player killer = event.getEntity().getKiller();
		if (killer != null) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(killer);
			if (!allTypes.isEmpty()) {
				for (PassiveJutsu jutsu : allTypes) {
					if (!jutsubook.hasJutsu(jutsu)) continue;
					jutsu.activate(killer, event.getEntity());
				}
			}
			if (entityTypes.containsKey(event.getEntityType())) {
				List<PassiveJutsu> list = entityTypes.get(event.getEntityType());
				for (PassiveJutsu jutsu : list) {
					if (!jutsubook.hasJutsu(jutsu)) continue;
					jutsu.activate(killer, event.getEntity());
				}
			}
		}
	}
	
}
