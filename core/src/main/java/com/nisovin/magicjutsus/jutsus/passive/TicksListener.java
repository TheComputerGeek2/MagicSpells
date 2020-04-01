package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.events.JutsuForgetEvent;
import com.nisovin.magicjutsus.events.JutsuLearnEvent;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.OverridePriority;

// Trigger argument is required
// Must be an integer.
// The value reflects how often the trigger runs
// Where the value of the trigger variable is x
// The trigger will activate every x ticks
public class TicksListener extends PassiveListener {

	Map<Integer, Ticker> tickers = new HashMap<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		try {
			int interval = Integer.parseInt(var);
			Ticker ticker = tickers.computeIfAbsent(interval, Ticker::new);
			ticker.add(jutsu);
		} catch (NumberFormatException e) {
			// No op
		}
	}
	
	@Override
	public void initialize() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!player.isValid()) continue;
			for (Ticker ticker : tickers.values()) {
				ticker.add(player);
			}
		}
	}
	
	@Override
	public void turnOff() {
		for (Ticker ticker : tickers.values()) {
			ticker.turnOff();
		}
		tickers.clear();
	}
	
	@OverridePriority
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		for (Ticker ticker : tickers.values()) {
			ticker.add(player);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		for (Ticker ticker : tickers.values()) {
			ticker.remove(player);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		for (Ticker ticker : tickers.values()) {
			ticker.remove(player);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		for (Ticker ticker : tickers.values()) {
			ticker.add(player);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onLearn(JutsuLearnEvent event) {
		Jutsu jutsu = event.getJutsu();
		if (!(jutsu instanceof PassiveJutsu)) return;
		for (Ticker ticker : tickers.values()) {
			if (!ticker.monitoringJutsu((PassiveJutsu)jutsu)) continue;
			ticker.add(event.getLearner(), (PassiveJutsu)jutsu);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onForget(JutsuForgetEvent event) {
		Jutsu jutsu = event.getJutsu();
		if (!(jutsu instanceof PassiveJutsu)) return;
		for (Ticker ticker : tickers.values()) {
			if (!ticker.monitoringJutsu((PassiveJutsu)jutsu)) continue;
			ticker.remove(event.getForgetter(), (PassiveJutsu)jutsu);
		}
	}
	
	static class Ticker implements Runnable {

		int taskId;
		Map<PassiveJutsu, Collection<Player>> jutsus = new HashMap<>();
		String profilingKey;
		
		public Ticker(int interval) {
			taskId = MagicJutsus.scheduleRepeatingTask(this, interval, interval);
			profilingKey = MagicJutsus.profilingEnabled() ? "PassiveTick:" + interval : null;
		}
		
		public void add(PassiveJutsu jutsu) {
			jutsus.put(jutsu, new HashSet<>());
		}
		
		public void add(Player player) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			for (Entry<PassiveJutsu, Collection<Player>> entry : jutsus.entrySet()) {
				if (jutsubook.hasJutsu(entry.getKey())) entry.getValue().add(player);
			}
		}
		
		public void add(Player player, PassiveJutsu jutsu) {
			jutsus.get(jutsu).add(player);
		}
		
		public void remove(Player player) {
			for (Collection<Player> players : jutsus.values()) {
				players.remove(player);
			}
		}
		
		public void remove(Player player, PassiveJutsu jutsu) {
			jutsus.get(jutsu).remove(player);
		}
		
		public boolean monitoringJutsu(PassiveJutsu jutsu) {
			return jutsus.containsKey(jutsu);
		}
		
		@Override
		public void run() {
			long start = System.nanoTime();
			for (Map.Entry<PassiveJutsu, Collection<Player>> entry : jutsus.entrySet()) {
				Collection<Player> players = entry.getValue();
				if (players.isEmpty()) continue;
				for (Player p : new ArrayList<>(players)) {
					if (p.isOnline() && p.isValid()) {
						entry.getKey().activate(p);
					} else {
						players.remove(p);
					}
				}
			}
			if (profilingKey != null) MagicJutsus.addProfile(profilingKey, System.nanoTime() - start);
		}
		
		public void turnOff() {
			MagicJutsus.cancelTask(taskId);
		}
		
	}
	
}
