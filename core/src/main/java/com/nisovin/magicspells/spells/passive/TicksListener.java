package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellForgetEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger argument is required
// Must be an integer.
// The value reflects how often the trigger runs
// Where the value of the trigger variable is x
// The trigger will activate every x ticks
public class TicksListener extends PassiveListener {

	private Ticker ticker;

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;
		try {
			int interval = Integer.parseInt(var);
			ticker = new Ticker(passiveSpell, interval);
		} catch (NumberFormatException e) {
			// ignored
		}

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!player.isValid()) continue;
			if (!hasSpell(player)) continue;
			if (!canTrigger(player)) continue;
			ticker.add(player);
		}

		for (World world : Bukkit.getWorlds()) {
			for (LivingEntity livingEntity : world.getLivingEntities()) {
				if (!canTrigger(livingEntity)) continue;
				ticker.add(livingEntity);
			}
		}
	}
	
	@Override
	public void turnOff() {
		ticker.turnOff();
	}

	@OverridePriority
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (!(entity instanceof LivingEntity)) continue;
			if (!canTrigger((LivingEntity) entity)) continue;
			ticker.add((LivingEntity) entity);
		}
	}

	@OverridePriority
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (!(entity instanceof LivingEntity)) continue;
			if (!canTrigger((LivingEntity) entity)) continue;
			ticker.remove((LivingEntity) entity);
		}
	}

	@OverridePriority
	@EventHandler
	public void onEntitySpawn(EntitySpawnEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Player) return;
		if (!(entity instanceof LivingEntity)) return;
		if (!canTrigger((LivingEntity) entity)) return;
		ticker.add((LivingEntity) entity);
	}
	
	@OverridePriority
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (!hasSpell(player)) return;
		if (!canTrigger(player)) return;
		ticker.add(player);
	}
	
	@OverridePriority
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (!canTrigger(player)) return;
		ticker.remove(player);
	}
	
	@OverridePriority
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (!canTrigger(player)) return;
		ticker.remove(player);
	}
	
	@OverridePriority
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		if (!hasSpell(player)) return;
		if (!canTrigger(player)) return;
		ticker.add(player);
	}
	
	@OverridePriority
	@EventHandler
	public void onLearn(SpellLearnEvent event) {
		Spell spell = event.getSpell();
		if (!(spell instanceof PassiveSpell)) return;
		if (!spell.getInternalName().equals(passiveSpell.getInternalName())) return;
		ticker.add(event.getLearner());
	}
	
	@OverridePriority
	@EventHandler
	public void onForget(SpellForgetEvent event) {
		Spell spell = event.getSpell();
		if (!(spell instanceof PassiveSpell)) return;
		if (!spell.getInternalName().equals(passiveSpell.getInternalName())) return;
		ticker.remove(event.getForgetter());
	}
	
	private static class Ticker implements Runnable {

		private final Collection<LivingEntity> entities;

		private final PassiveSpell passiveSpell;

		private final int taskId;
		private final String profilingKey;
		
		public Ticker(PassiveSpell passiveSpell, int interval) {
			this.passiveSpell = passiveSpell;
			taskId = MagicSpells.scheduleRepeatingTask(this, interval, interval);
			profilingKey = MagicSpells.profilingEnabled() ? "PassiveTick:" + interval : null;
			entities = new ArrayList<>();
		}
		
		public void add(LivingEntity livingEntity) {
			entities.add(livingEntity);
		}

		public void remove(LivingEntity livingEntity) {
			entities.remove(livingEntity);
		}

		@Override
		public void run() {
			long start = System.nanoTime();

			for (LivingEntity entity : new ArrayList<>(entities)) {
				if (entity == null || !entity.isValid()) {
					entities.remove(entity);
					continue;
				}
				passiveSpell.activate(entity);
			}

			if (profilingKey != null) MagicSpells.addProfile(profilingKey, System.nanoTime() - start);
		}
		
		public void turnOff() {
			MagicSpells.cancelTask(taskId);
		}
		
	}
	
}
