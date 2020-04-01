package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;
import com.nisovin.magicjutsus.util.Util;

// Trigger variable is optional
// If not specified, it triggers in all forms
// The trigger variable may be a comma separated list containing any of the following
// ground, fish, fail, <entity type>
public class FishListener extends PassiveListener {

	Map<EntityType, List<PassiveJutsu>> types = new HashMap<>();
	List<PassiveJutsu> ground = new ArrayList<>();
	List<PassiveJutsu> fish = new ArrayList<>();
	List<PassiveJutsu> fail = new ArrayList<>();
	List<PassiveJutsu> allTypes = new ArrayList<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			allTypes.add(jutsu);
		} else {
			String[] split = var.replace(" ", "").toUpperCase().split(",");
			for (String s : split) {
				if (s.equalsIgnoreCase("ground")) {
					ground.add(jutsu);
				} else if (s.equalsIgnoreCase("fish")) {
					fish.add(jutsu);
				} else if (s.equalsIgnoreCase("fail")) {
					fail.add(jutsu);
				} else {
					EntityType t = Util.getEntityType(s);
					if (t != null) {
						List<PassiveJutsu> list = types.computeIfAbsent(t, type -> new ArrayList<>());
						list.add(jutsu);
					}
				}
			}
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onFish(PlayerFishEvent event) {
		PlayerFishEvent.State state = event.getState();
		Player player = event.getPlayer();
		
		if (!allTypes.isEmpty()) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			Entity entity = event.getCaught();
			for (PassiveJutsu jutsu : allTypes) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu)) continue;
				boolean casted = jutsu.activate(player, entity instanceof LivingEntity ? (LivingEntity)entity : null);
				if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
				event.setCancelled(true);
			}
		}
		
		if (state == State.IN_GROUND && !ground.isEmpty()) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			for (PassiveJutsu jutsu : ground) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu)) continue;
				boolean casted = jutsu.activate(player, event.getHook().getLocation());
				if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
				event.setCancelled(true);
			}
		} else if (state == State.CAUGHT_FISH && !fish.isEmpty()) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			for (PassiveJutsu jutsu : fish) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu)) continue;
				boolean casted = jutsu.activate(player, event.getHook().getLocation());
				if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
				event.setCancelled(true);
			}
		} else if (state == State.FAILED_ATTEMPT && !fail.isEmpty()) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			for (PassiveJutsu jutsu : fail) {
				if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
				if (!jutsubook.hasJutsu(jutsu)) continue;
				boolean casted = jutsu.activate(player, event.getHook().getLocation());
				if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
				event.setCancelled(true);
			}
		} else if (state == State.CAUGHT_ENTITY && !types.isEmpty()) {
			Entity entity = event.getCaught();
			if (entity != null && types.containsKey(entity.getType())) {
				Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
				for (PassiveJutsu jutsu : fail) {
					if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
					if (!jutsubook.hasJutsu(jutsu)) continue;
					boolean casted = jutsu.activate(player, entity instanceof LivingEntity ? (LivingEntity)entity : null);
					if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
					event.setCancelled(true);
				}
			}
		}
	}
	
}
