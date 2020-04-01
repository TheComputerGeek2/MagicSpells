package com.nisovin.magicjutsus;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.zones.NoMagicZoneManager;

public class BuffManager {

	private Map<LivingEntity, Set<BuffJutsu>> activeBuffs;

	private int interval;
	private Monitor monitor;

	public BuffManager(int interval) {
		this.interval = interval;
		activeBuffs = new ConcurrentHashMap<>();
		monitor = new Monitor();
	}

	public void addBuff(LivingEntity entity, BuffJutsu jutsu) {
		Set<BuffJutsu> buffs = activeBuffs.computeIfAbsent(entity, s -> new HashSet<>());
		// Sanity Check
		if (buffs == null) throw new IllegalStateException("buffs should not be null here");
		buffs.add(jutsu);

		monitor.run();
	}

	public void removeBuff(LivingEntity entity, BuffJutsu jutsu) {
		Set<BuffJutsu> buffs = activeBuffs.get(entity);
		if (buffs == null) return;
		buffs.remove(jutsu);
		if (buffs.isEmpty()) activeBuffs.remove(entity);
	}

	public Map<LivingEntity, Set<BuffJutsu>> getActiveBuffs() {
		return activeBuffs;
	}

	public Set<BuffJutsu> getActiveBuffs(LivingEntity entity) {
		return activeBuffs.get(entity);
	}

	public void turnOff() {
		MagicJutsus.cancelTask(monitor.taskId);
		monitor = null;
		activeBuffs.clear();
		activeBuffs = null;
	}

	private class Monitor implements Runnable {

		private int taskId;

		private Monitor() {
			taskId = MagicJutsus.scheduleRepeatingTask(this, interval, interval);
		}

		@Override
		public void run() {
			NoMagicZoneManager zoneManager = MagicJutsus.getNoMagicZoneManager();
			if (zoneManager == null) return;

			for (LivingEntity entity : activeBuffs.keySet()) {
				if (entity == null) continue;
				if (!entity.isValid()) continue;

				Set<BuffJutsu> buffs = new HashSet<>(activeBuffs.get(entity));

				for (BuffJutsu jutsu : buffs) {
					if (jutsu.isExpired(entity)) jutsu.turnOff(entity);
					if (zoneManager.willFizzle(entity, jutsu)) jutsu.turnOff(entity);
				}
			}
		}

		public void stop() {
			MagicJutsus.cancelTask(taskId);
		}

	}

}
