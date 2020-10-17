package com.nisovin.magicspells.spells.passive;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class OffhandSwapListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}
	
	@OverridePriority
	@EventHandler
	public void onSwap(PlayerSwapHandItemsEvent event) {
		Player player = event.getPlayer();
		if (!hasSpell(player)) return;
		
		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(player);
		if (!cancelDefaultAction(casted)) return;
		event.setCancelled(true);
	}

}
