package com.nisovin.magicspells.spells.passive;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is used here
public class StartSprintListener extends PassiveListener {
	
	@Override
	public void initialize(String var) {

	}
	
	@OverridePriority
	@EventHandler
	public void onSprint(PlayerToggleSprintEvent event) {
		Player player = event.getPlayer();
		if (!event.isSprinting()) return;
		if (!hasSpell(player)) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(player);
		if (!cancelDefaultAction(casted)) return;
		event.setCancelled(true);
	}

}
