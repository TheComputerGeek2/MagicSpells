package com.nisovin.magicjutsus.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PlayerNameUtils {

	public static Player getPlayerExact(String playerName) {
		//TODO come up with a non depreciated system
		return Bukkit.getPlayerExact(playerName);
	}
	
	public static Player getPlayer(String playername) {
		//TODO come up with a non depreciated system
		return Bukkit.getPlayer(playername);
	}
	
	public static OfflinePlayer getOfflinePlayer(String name) {
		//TODO come up with a non depreciated system
		return Bukkit.getOfflinePlayer(name);
	}
	
}
