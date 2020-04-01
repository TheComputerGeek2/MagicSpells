package com.nisovin.magicjutsus.jutsus.instant;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.castmodifiers.ModifierSet;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.util.projectile.ProjectileManager;
import com.nisovin.magicjutsus.util.projectile.ProjectileManagers;

public class ProjectileJutsu extends InstantJutsu implements TargetedLocationJutsu {

	private List<ProjectileMonitor> monitors;

	private Random random;

	private ProjectileManager projectileManager;

	private Vector relativeOffset;

	private int tickInterval;
	private int tickJutsuInterval;
	private int specialEffectInterval;

	private float rotation;
	private float velocity;
	private float hitRadius;
	private float vertSpread;
	private float horizSpread;
	private float verticalHitRadius;

	private boolean gravity;
	private boolean charged;
	private boolean incendiary;
	private boolean stopOnModifierFail;

	private double maxDuration;

	private String hitJutsuName;
	private String tickJutsuName;
	private String projectileName;
	private String groundJutsuName;
	private String modifierJutsuName;
	private String durationJutsuName;

	private Ninjutsu hitJutsu;
	private Ninjutsu tickJutsu;
	private Ninjutsu groundJutsu;
	private Ninjutsu modifierJutsu;
	private Ninjutsu durationJutsu;

	private ModifierSet projectileModifiers;
	private List<String> projectileModifiersStrings;

	public ProjectileJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		monitors = new ArrayList<>();

		random = new Random();

		projectileManager = ProjectileManagers.getManager(getConfigString("projectile-type",  "arrow"));

		relativeOffset = getConfigVector("relative-offset", "0,1.5,0");

		tickInterval = getConfigInt("tick-interval", 1);
		tickJutsuInterval = getConfigInt("jutsu-interval", 20);
		specialEffectInterval = getConfigInt("special-effect-interval", 0);

		rotation = getConfigFloat("rotation", 0F);
		velocity = getConfigFloat("velocity", 1F);
		hitRadius = getConfigFloat("hit-radius", 2F);
		vertSpread = getConfigFloat("vertical-spread", 0F);
		horizSpread = getConfigFloat("horizontal-spread", 0F);
		verticalHitRadius = getConfigFloat("vertical-hit-radius", 2F);

		gravity = getConfigBoolean("gravity", true);
		charged = getConfigBoolean("charged", false);
		incendiary = getConfigBoolean("incendiary", false);
		stopOnModifierFail = getConfigBoolean("stop-on-modifier-fail", true);

		maxDuration = getConfigDouble("max-duration", 10) * (double) TimeUtil.MILLISECONDS_PER_SECOND;

		hitJutsuName = getConfigString("jutsu", "");
		tickJutsuName = getConfigString("jutsu-on-tick", "");
		projectileName = ChatColor.translateAlternateColorCodes('&', getConfigString("projectile-name", ""));
		groundJutsuName = getConfigString("jutsu-on-hit-ground", "");
		modifierJutsuName = getConfigString("jutsu-on-modifier-fail", "");
		durationJutsuName = getConfigString("jutsu-after-duration", "");

		projectileModifiersStrings = getConfigStringList("projectile-modifiers", null);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (projectileModifiersStrings != null && !projectileModifiersStrings.isEmpty()) {
			projectileModifiers = new ModifierSet(projectileModifiersStrings);
			projectileModifiersStrings = null;
		}

		hitJutsu = new Ninjutsu(hitJutsuName);
		if (!hitJutsu.process()) {
			hitJutsu = null;
			if (!hitJutsuName.isEmpty()) MagicJutsus.error("ProjectileJutsu '" + internalName + "' has an invalid jutsu defined!");
		}

		groundJutsu = new Ninjutsu(groundJutsuName);
		if (!groundJutsu.process() || !groundJutsu.isTargetedLocationJutsu()) {
			groundJutsu = null;
			if (!groundJutsuName.isEmpty()) MagicJutsus.error("ProjectileJutsu '" + internalName + "' has an invalid jutsu-on-hit-ground defined!");
		}

		tickJutsu = new Ninjutsu(tickJutsuName);
		if (!tickJutsu.process() || !tickJutsu.isTargetedLocationJutsu()) {
			tickJutsu = null;
			if (!tickJutsuName.isEmpty()) MagicJutsus.error("ProjectileJutsu '" + internalName + "' has an invalid jutsu-on-tick defined!");
		}

		durationJutsu = new Ninjutsu(durationJutsuName);
		if (!durationJutsu.process() || !durationJutsu.isTargetedLocationJutsu()) {
			durationJutsu = null;
			if (!durationJutsuName.isEmpty()) MagicJutsus.error("ProjectileJutsu '" + internalName + "' has an invalid jutsu-after-duration defined!");
		}

		modifierJutsu = new Ninjutsu(modifierJutsuName);
		if (!modifierJutsu.process() || !modifierJutsu.isTargetedLocationJutsu()) {
			if (!modifierJutsuName.isEmpty()) MagicJutsus.error("ProjectileJutsu '" + internalName + "' has an invalid jutsu-on-modifier-fail defined!");
			modifierJutsu = null;
		}
	}

	@Override
	public void turnOff() {
		for (ProjectileMonitor monitor : monitors) {
			monitor.stop();
		}

		monitors.clear();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			new ProjectileMonitor(livingEntity, livingEntity.getLocation(), power);
			return PostCastAction.HANDLE_NORMALLY;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		new ProjectileMonitor(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof WitherSkull)) return;
		Projectile projectile = (Projectile) entity;
		for (ProjectileMonitor monitor : monitors) {
			if (monitor.projectile == null) continue;
			if (!monitor.projectile.equals(projectile)) continue;

			event.setCancelled(true);
			monitor.stop();
			break;
		}
	}

	@EventHandler
	public void onProjectileHit(EntityDamageByEntityEvent event) {
		if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) return;
		if (!(event.getEntity() instanceof LivingEntity)) return;
		LivingEntity entity = (LivingEntity) event.getEntity();
		Entity damagerEntity = event.getDamager();
		if (!(damagerEntity instanceof Projectile)) return;

		Projectile projectile = (Projectile) damagerEntity;
		for (ProjectileMonitor monitor : monitors) {
			if (monitor.projectile == null) continue;
			if (!monitor.projectile.equals(projectile)) continue;

			if (hitJutsu != null && hitJutsu.isTargetedEntityJutsu()) hitJutsu.castAtEntity(monitor.caster, entity, monitor.power);
			else if (hitJutsu != null && hitJutsu.isTargetedLocationJutsu()) hitJutsu.castAtLocation(monitor.caster, entity.getLocation(), monitor.power);
			playJutsuEffects(EffectPosition.TARGET, entity);
			event.setCancelled(true);
			event.setDamage(0);
			monitor.stop();
			break;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEnderTeleport(PlayerTeleportEvent event) {
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
		for (ProjectileMonitor monitor : monitors) {
			if (event.getTo() == null) continue;
			if (monitor.projectile == null) continue;
			if (!locationsEqual(monitor.projectile.getLocation(), event.getTo())) continue;
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPotionSplash(PotionSplashEvent event) {
		for (ProjectileMonitor monitor : monitors) {
			if (monitor.projectile == null) continue;
			if (!monitor.projectile.equals(event.getPotion())) continue;
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.EGG) return;
		for (ProjectileMonitor monitor : monitors) {
			if (monitor.projectile == null) continue;
			if (!locationsEqual(monitor.projectile.getLocation(), event.getLocation())) continue;
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler
	public void onProjectileBlockHit(ProjectileHitEvent e) {
		Projectile projectile = e.getEntity();
		Block block = e.getHitBlock();
		if (block == null) return;
		for (ProjectileMonitor monitor : monitors) {
			if (monitor.projectile == null) continue;
			if (!monitor.projectile.equals(projectile)) continue;
			if (monitor.caster != null && groundJutsu != null) groundJutsu.castAtLocation(monitor.caster, projectile.getLocation(), monitor.power);
			monitor.stop();
		}
	}

	private boolean locationsEqual(Location loc1, Location loc2) {
		return Math.abs(loc1.getX() - loc2.getX()) < 0.1
				&& Math.abs(loc1.getY() - loc2.getY()) < 0.1
				&& Math.abs(loc1.getZ() - loc2.getZ()) < 0.1;
	}

	private class ProjectileMonitor implements Runnable {

		private Projectile projectile;
		private Location currentLocation;
		private Location startLocation;
		private LivingEntity caster;
		private Vector currentVelocity;
		private float power;
		private long startTime;

		private int taskId;
		private int counter = 0;

		private ProjectileMonitor(LivingEntity caster, Location startLocation, float power) {
			this.caster = caster;
			this.power = power;
			this.startLocation = startLocation;

			initialize();
		}

		private void initialize() {
			startTime = System.currentTimeMillis();
			taskId = MagicJutsus.scheduleRepeatingTask(this, 0, tickInterval);

			Vector startDir = startLocation.clone().getDirection().normalize();
			Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
			startLocation.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
			startLocation.add(startLocation.getDirection().multiply(relativeOffset.getX()));
			startLocation.setY(startLocation.getY() + relativeOffset.getY());

			playJutsuEffects(EffectPosition.CASTER, startLocation);

			projectile = startLocation.getWorld().spawn(startLocation, projectileManager.getProjectileClass());
			currentVelocity = startLocation.getDirection();
			currentVelocity.multiply(velocity * power);
			if (rotation != 0) Util.rotateVector(currentVelocity, rotation);
			if (horizSpread > 0 || vertSpread > 0) {
				float rx = -1 + random.nextFloat() * (1 + 1);
				float ry = -1 + random.nextFloat() * (1 + 1);
				float rz = -1 + random.nextFloat() * (1 + 1);
				currentVelocity.add(new Vector(rx * horizSpread, ry * vertSpread, rz * horizSpread));
			}
			projectile.setVelocity(currentVelocity);
			projectile.setGravity(gravity);
			projectile.setShooter(caster);
			if (!projectileName.isEmpty()) {
				projectile.setCustomName(projectileName);
				projectile.setCustomNameVisible(true);
			}
			if (projectile instanceof WitherSkull) ((WitherSkull) projectile).setCharged(charged);
			if (projectile instanceof Explosive) ((Explosive) projectile).setIsIncendiary(incendiary);

			playJutsuEffects(EffectPosition.PROJECTILE, projectile);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, startLocation, projectile.getLocation(), caster, projectile);
			monitors.add(this);
		}

		@Override
		public void run() {
			if ((caster != null && !caster.isValid())) {
				stop();
				return;
			}

			if (projectile == null || projectile.isDead()) {
				stop();
				return;
			}

			if (projectileModifiers != null && caster instanceof Player && !projectileModifiers.check(caster)) {
				if (modifierJutsu != null) modifierJutsu.castAtLocation(caster, currentLocation, power);
				if (stopOnModifierFail) stop();
				return;
			}

			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				if (durationJutsu != null) durationJutsu.castAtLocation(caster, currentLocation, power);
				stop();
				return;
			}

			currentLocation = projectile.getLocation();
			currentLocation.setDirection(projectile.getVelocity());

			if (counter % tickJutsuInterval == 0 && tickJutsu != null) tickJutsu.castAtLocation(caster, currentLocation, power);

			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) playJutsuEffects(EffectPosition.SPECIAL, currentLocation);

			counter++;

			for (Entity e : projectile.getNearbyEntities(hitRadius, verticalHitRadius, hitRadius)) {
				if (!(e instanceof LivingEntity)) continue;
				if (!validTargetList.canTarget(caster, e)) continue;

				JutsuTargetEvent event = new JutsuTargetEvent(ProjectileJutsu.this, caster, (LivingEntity) e, power);
				EventUtil.call(event);
				if (!event.isCancelled()) {
					if (hitJutsu != null) hitJutsu.castAtEntity(caster, (LivingEntity) e, event.getPower());
					stop();
					return;
				}
			}
		}

		private void stop() {
			playJutsuEffects(EffectPosition.DELAYED, currentLocation);
			MagicJutsus.cancelTask(taskId);
			caster = null;
			currentLocation = null;
			if (projectile != null) projectile.remove();
			projectile = null;
		}

	}

}
