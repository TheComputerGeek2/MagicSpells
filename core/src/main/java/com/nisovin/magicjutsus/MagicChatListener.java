package com.nisovin.magicjutsus;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class MagicChatListener implements Listener {

	private MagicJutsus plugin;
	
	MagicChatListener(MagicJutsus plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerChat(final AsyncPlayerChatEvent event) {
		MagicJutsus.scheduleDelayedTask(() -> handleIncantation(event.getPlayer(), event.getMessage()), 0);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
		boolean casted = handleIncantation(event.getPlayer(), event.getMessage());
		if (casted) event.setCancelled(true);
	}
	
	private boolean handleIncantation(Player player, String message) {
		if (message.contains(" ")) {
			String[] split = message.split(" ");
			Jutsu jutsu = MagicJutsus.getIncantations().get(split[0].toLowerCase() + " *");
			if (jutsu != null) {
				Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
				if (jutsubook.hasJutsu(jutsu)) {
					String[] args = new String[split.length - 1];
					System.arraycopy(split, 1, args, 0, args.length);
					jutsu.cast(player, args);
					return true;
				}
				return false;
			}
		}
		Jutsu jutsu = MagicJutsus.getIncantations().get(message.toLowerCase());
		if (jutsu != null) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			if (jutsubook.hasJutsu(jutsu)) {
				jutsu.cast(player);
				return true;
			}
		}
		return false;
	}
	
}
