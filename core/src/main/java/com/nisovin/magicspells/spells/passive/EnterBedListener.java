package com.nisovin.magicspells.spells.passive;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerBedEnterEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is currently used
public class EnterBedListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}
	
	@OverridePriority
	@EventHandler
	public void onDeath(PlayerBedEnterEvent event) {
		Player player = event.getPlayer();
		if (!hasSpell(player)) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(player);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}
	
}
