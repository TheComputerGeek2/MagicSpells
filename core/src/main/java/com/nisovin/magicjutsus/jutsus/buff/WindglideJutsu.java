package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class WindglideJutsu extends BuffJutsu {

	private Set<UUID> gliders;

	private Ninjutsu glideJutsu;
	private Ninjutsu collisionJutsu;
	private String glideJutsuName;
	private String collisionJutsuName;

	private boolean cancelOnCollision;
	private boolean blockCollisionDmg;

	private int interval;
	private float height;
	private float velocity;

	private GlideMonitor monitor;

	public WindglideJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		glideJutsuName = getConfigString("jutsu", "");
		collisionJutsuName = getConfigString("collision-jutsu", "");

		blockCollisionDmg = getConfigBoolean("block-collision-dmg", true);
		cancelOnCollision = getConfigBoolean("cancel-on-collision", false);

		height = getConfigFloat("height", 0F);
		interval = getConfigInt("interval", 4);
		velocity = getConfigFloat("velocity", 20F) / 10;
		if (interval <= 0) interval = 4;

		gliders = new HashSet<>();
		monitor = new GlideMonitor();
	}

	@Override
	public void initialize() {
		super.initialize();

		glideJutsu = new Ninjutsu(glideJutsuName);
		if (!glideJutsu.process() || !glideJutsu.isTargetedLocationJutsu()) {
			glideJutsu = null;
			if (!glideJutsuName.isEmpty()) MagicJutsus.error("WindglideJutsu " + internalName + " has an invalid jutsu defined");
		}

		collisionJutsu = new Ninjutsu(collisionJutsuName);
		if (!collisionJutsu.process() || !collisionJutsu.isTargetedLocationJutsu()) {
			collisionJutsu = null;
			if (!collisionJutsuName.isEmpty()) MagicJutsus.error("WindglideJutsu " + internalName + " has an invalid collision-jutsu defined");
		}
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		gliders.add(entity.getUniqueId());
		entity.setGliding(true);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return gliders.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		gliders.remove(entity.getUniqueId());
		entity.setGliding(false);
	}

	@Override
	protected void turnOff() {
		for (EffectPosition pos: EffectPosition.values()) {
			cancelEffectForAllPlayers(pos);
		}

		for (UUID id : gliders) {
			Entity entity = Bukkit.getEntity(id);
			if (entity == null) continue;
			if (!(entity instanceof LivingEntity)) continue;
			if (!entity.isValid()) continue;

			((LivingEntity) entity).setGliding(false);
			turnOffBuff((LivingEntity) entity);
		}

		gliders.clear();
	}

	@EventHandler
	public void onEntityGlide(EntityToggleGlideEvent e) {
		Entity entity = e.getEntity();
		if (!(entity instanceof LivingEntity)) return;
		LivingEntity livingEntity = (LivingEntity) entity;
		if (!isActive(livingEntity)) return;
		if (livingEntity.isGliding()) e.setCancelled(true);
	}

	@EventHandler
	public void onEntityCollision(EntityDamageEvent e) {
		if (e.getCause() != EntityDamageEvent.DamageCause.FLY_INTO_WALL) return;
		if (!(e.getEntity() instanceof LivingEntity)) return;
		LivingEntity livingEntity = (LivingEntity) e.getEntity();
		if (!isActive(livingEntity)) return;

		if (blockCollisionDmg) e.setCancelled(true);
		if (cancelOnCollision) turnOff(livingEntity);
		if (collisionJutsu != null) collisionJutsu.castAtLocation(livingEntity, livingEntity.getLocation(), 1F);
	}

	private class GlideMonitor implements Runnable {

		private int taskId;

		private GlideMonitor() {
			taskId = MagicJutsus.scheduleRepeatingTask(this, interval, interval);
		}

		@Override
		public void run() {
			for (UUID id : gliders) {
				Entity entity = Bukkit.getEntity(id);
				if (entity == null || !entity.isValid()) continue;
				if (!(entity instanceof LivingEntity)) continue;

				Location eLoc = entity.getLocation();
				Vector v = eLoc.getDirection().normalize().multiply(velocity).add(new Vector(0, height, 0));
				entity.setVelocity(v);

				if (glideJutsu != null) glideJutsu.castAtLocation((LivingEntity) entity, eLoc, 1F);
				playJutsuEffects(EffectPosition.SPECIAL, eLoc);
				addUseAndChargeCost((LivingEntity) entity);
			}
		}

		public void stop() {
			MagicJutsus.cancelTask(taskId);
		}

	}

}
