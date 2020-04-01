package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Creature;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;

public class InvisibilityJutsu extends BuffJutsu {

	private Set<UUID> invisibles;

	private double mobRadius;
	private boolean preventPickups;

	public InvisibilityJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		mobRadius = getConfigDouble("mob-radius", 30);
		preventPickups = getConfigBoolean("prevent-pickups", true);

		invisibles = new HashSet<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		if (!(entity instanceof Player)) return false;
		makeInvisible(entity);
		invisibles.add(entity.getUniqueId());
		return true;
	}
	
	@Override
	public boolean recastBuff(LivingEntity entity, float power, String[] args) {
		if (!(entity instanceof Player)) return false;
		makeInvisible(entity);
		if (invisibles.contains(entity.getUniqueId())) invisibles.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return invisibles.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		invisibles.remove(entity.getUniqueId());
		if (entity instanceof Player) Util.forEachPlayerOnline(p -> p.showPlayer(MagicJutsus.getInstance(), (Player) entity));
	}

	@Override
	protected void turnOff() {
		invisibles.clear();
	}

	private void makeInvisible(LivingEntity entity) {
		Util.forEachPlayerOnline(p -> p.hidePlayer(MagicJutsus.getInstance(), (Player) entity));
		
		Creature creature;
		for (Entity e : entity.getNearbyEntities(mobRadius, mobRadius, mobRadius)) {
			if (!(e instanceof Creature)) continue;
			
			creature = (Creature) e;
			LivingEntity target = creature.getTarget();
			if (target == null) continue;
			if (!target.equals(entity)) continue;
			
			creature.setTarget(null);
		}
	}
	
	@EventHandler
	public void onEntityItemPickup(EntityPickupItemEvent event) {
		if (!preventPickups) return;
		LivingEntity entity = event.getEntity();
		if (!isActive(entity)) return;
		event.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onEntityTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();
		if (!(target instanceof LivingEntity)) return;
		if (!isActive((LivingEntity) target)) return;

		event.setCancelled(true);
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		for (UUID id : invisibles) {
			Entity entity = Bukkit.getEntity(id);
			if (entity == null) continue;
			if (!(entity instanceof Player)) continue;
			player.hidePlayer(MagicJutsus.getInstance(), (Player) entity);
		}

		if (isActive(player)) {
			Util.forEachPlayerOnline(p -> p.hidePlayer(MagicJutsus.getInstance(), player));
		}

	}

}
