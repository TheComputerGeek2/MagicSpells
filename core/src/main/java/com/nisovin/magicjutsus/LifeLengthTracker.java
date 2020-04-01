package com.nisovin.magicjutsus;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.TimeUtil;

public class LifeLengthTracker implements Listener {

	private Map<String, Long> lastSpawn = new HashMap<>();
	private Map<String, Integer> lastLifeLength = new HashMap<>();
	
	LifeLengthTracker() {
		Util.forEachPlayerOnline(player -> lastSpawn.put(player.getName(), System.currentTimeMillis()));
		MagicJutsus.registerEvents(this);
	}
	
	public int getCurrentLifeLength(Player player) {
		if (lastSpawn.containsKey(player.getName())) {
			long spawn = lastSpawn.get(player.getName());
			return (int) ((System.currentTimeMillis() - spawn) / TimeUtil.MILLISECONDS_PER_SECOND);
		}
		return 0;
	}
	
	public int getLastLifeLength(Player player) {
		if (lastLifeLength.containsKey(player.getName())) return lastLifeLength.get(player.getName());
		return 0;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		lastSpawn.put(event.getPlayer().getName(), System.currentTimeMillis());
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Long spawn = lastSpawn.remove(event.getPlayer().getName());
		if (spawn != null) lastLifeLength.put(event.getPlayer().getName(), (int) ((System.currentTimeMillis() - spawn) / TimeUtil.MILLISECONDS_PER_SECOND));
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Long spawn = lastSpawn.remove(event.getEntity().getName());
		if (spawn != null) lastLifeLength.put(event.getEntity().getName(), (int) ((System.currentTimeMillis() - spawn) / TimeUtil.MILLISECONDS_PER_SECOND));
	}
	
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		lastSpawn.put(event.getPlayer().getName(), System.currentTimeMillis());
	}
	
}
