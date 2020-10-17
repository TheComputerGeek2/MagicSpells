package com.nisovin.magicspells.spells.passive;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is currently used
public class StartSneakListener extends PassiveListener {
		
	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event) {
		if (!event.isSneaking()) return;
		if (!hasSpell(event.getPlayer())) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(event.getPlayer());
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
