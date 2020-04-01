package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

public class OffhandSwapListener extends PassiveListener {
	
	List<PassiveJutsu> jutsus = new ArrayList<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		if (jutsu != null) jutsus.add(jutsu);
	}
	
	@OverridePriority
	@EventHandler
	public void onSwap(PlayerSwapHandItemsEvent event) {
		Player player = event.getPlayer();
		if (player == null) return;
		
		Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
		if (jutsubook == null) return;
		
		for (PassiveJutsu jutsu: jutsus) {
			if (!isCancelStateOk(jutsu, event.isCancelled())) continue;
			if (!jutsubook.hasJutsu(jutsu)) continue;
			boolean casted = jutsu.activate(player);
			if (!PassiveListener.cancelDefaultAction(jutsu, casted)) continue;
			event.setCancelled(true);
		}
	}

}
