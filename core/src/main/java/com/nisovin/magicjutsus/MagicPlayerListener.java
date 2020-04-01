package com.nisovin.magicjutsus;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

class MagicPlayerListener implements Listener {
	
	private MagicJutsus plugin;
	
	MagicPlayerListener(MagicJutsus plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		// set up jutsu book
		Jutsubook jutsubook = new Jutsubook(player, plugin);
		MagicJutsus.getJutsubooks().put(player.getName(), jutsubook);
		
		// set up mana bar
		if (MagicJutsus.getManaHandler() != null) MagicJutsus.getManaHandler().createManaBar(player);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Jutsubook jutsubook = MagicJutsus.getJutsubooks().remove(event.getPlayer().getName());
		if (jutsubook != null) jutsubook.destroy();
	}
	
	// DEBUG INFO: level 2, player changed from world to world, reloading jutsus
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
		if (!plugin.separatePlayerJutsusPerWorld) return;
		Player player = event.getPlayer();
		MagicJutsus.debug(2, "Player '" + player.getName() + "' changed from world '" + event.getFrom().getName() + "' to '" + player.getWorld().getName() + "', reloading jutsus");
		MagicJutsus.getJutsubook(player).reload();
	}
	
}
