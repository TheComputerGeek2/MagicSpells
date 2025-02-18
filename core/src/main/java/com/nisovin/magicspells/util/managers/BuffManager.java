package com.nisovin.magicspells.util.managers;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.function.Consumer;

import com.google.common.collect.SetMultimap;
import com.google.common.collect.HashMultimap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.events.BuffEndEvent;
import com.nisovin.magicspells.events.BuffStartEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;

public class BuffManager {

	private final SetMultimap<UUID, BuffSpell> activeBuffs;
	private final Map<UUID, LivingEntity> lastEntity;
	private final BuffMonitor buffMonitor;
	private final int interval;

	public BuffManager(int interval) {
		this.interval = interval;

		activeBuffs = HashMultimap.create();
		lastEntity = new HashMap<>();

		buffMonitor = interval > 0 ? new BuffMonitor() : null;
	}

	public void startBuff(LivingEntity entity, BuffSpell spell) {
		UUID uuid = entity.getUniqueId();
		activeBuffs.put(uuid, spell);
		lastEntity.put(uuid, entity);

		new BuffStartEvent(entity, spell).callEvent();
	}

	public void endBuff(LivingEntity entity, BuffSpell spell) {
		UUID uuid = entity.getUniqueId();
		activeBuffs.remove(uuid, spell);
		if (!activeBuffs.containsKey(uuid)) lastEntity.remove(uuid, entity);

		new BuffEndEvent(entity, spell).callEvent();
	}

	public Set<BuffSpell> getActiveBuffs(LivingEntity entity) {
		return activeBuffs.get(entity.getUniqueId());
	}

	public void turnOff() {
		activeBuffs.clear();
		if (buffMonitor != null) buffMonitor.stop();
	}

	private class BuffMonitor implements Consumer<ScheduledTask>, Listener {

		private final ScheduledTask task;

		private BuffMonitor() {
			if (interval > 0) task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(MagicSpells.getInstance(), this, interval, interval);
			else task = null;
			MagicSpells.registerEvents(this);
		}

		public void stop() {
			if (task != null) task.cancel();
		}

		@Override
		public void accept(ScheduledTask scheduledTask) {
			NoMagicZoneManager zoneManager = MagicSpells.getNoMagicZoneManager();

			activeBuffs.entries().removeIf(entry -> {
				UUID uuid = entry.getKey();
				LivingEntity entity = Bukkit.getEntity(uuid) instanceof LivingEntity le ? le : lastEntity.get(uuid);

				BuffSpell buff = entry.getValue();
				if ((entity instanceof Player || entity.isValid()) && !buff.isExpired(entity) && !zoneManager.willFizzle(entity, buff))
					return false;

				buff.turnOff(entity, false);
				new BuffEndEvent(entity, buff).callEvent();
				return true;
			});
		}

		@EventHandler
		public void onAdd(EntityAddToWorldEvent event) {
			if (event.getEntity() instanceof LivingEntity entity)
				lastEntity.replace(entity.getUniqueId(), entity);
		}

	}

}
