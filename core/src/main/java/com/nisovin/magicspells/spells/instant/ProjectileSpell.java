package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

import org.bukkit.Location;
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

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.projectile.ProjectileManager;
import com.nisovin.magicspells.util.projectile.ProjectileManagers;

public class ProjectileSpell extends InstantSpell implements TargetedLocationSpell {

	private List<ProjectileMonitor> monitors;

	private Random random;

	private ProjectileManager projectileManager;

	private Vector relativeOffset;

	private int tickInterval;
	private int tickSpellInterval;
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

	private String hitSpellName;
	private String tickSpellName;
	private String projectileName;
	private String groundSpellName;
	private String modifierSpellName;
	private String durationSpellName;

	private Subspell hitSpell;
	private Subspell tickSpell;
	private Subspell groundSpell;
	private Subspell modifierSpell;
	private Subspell durationSpell;

	private ModifierSet projectileModifiers;
	private List<String> projectileModifiersStrings;

	public ProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		monitors = new ArrayList<>();

		random = new Random();

		projectileManager = ProjectileManagers.getManager(getConfigString("projectile-type",  "arrow"));

		relativeOffset = getConfigVector("relative-offset", "0,1.5,0");

		tickInterval = getConfigInt("tick-interval", 1);
		tickSpellInterval = getConfigInt("spell-interval", 20);
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

		hitSpellName = getConfigString("spell", "");
		tickSpellName = getConfigString("spell-on-tick", "");
		projectileName = Util.colorize(getConfigString("projectile-name", ""));
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		modifierSpellName = getConfigString("spell-on-modifier-fail", "");
		durationSpellName = getConfigString("spell-after-duration", "");

		projectileModifiersStrings = getConfigStringList("projectile-modifiers", null);
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		if (projectileModifiersStrings != null && !projectileModifiersStrings.isEmpty()) {
			projectileModifiers = new ModifierSet(projectileModifiersStrings);
			projectileModifiersStrings = null;
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		hitSpell = new Subspell(hitSpellName);
		if (!hitSpell.process()) {
			hitSpell = null;
			if (!hitSpellName.isEmpty()) MagicSpells.error("ProjectileSpell '" + internalName + "' has an invalid spell defined!");
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process() || !groundSpell.isTargetedLocationSpell()) {
			groundSpell = null;
			if (!groundSpellName.isEmpty()) MagicSpells.error("ProjectileSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
		}

		tickSpell = new Subspell(tickSpellName);
		if (!tickSpell.process() || !tickSpell.isTargetedLocationSpell()) {
			tickSpell = null;
			if (!tickSpellName.isEmpty()) MagicSpells.error("ProjectileSpell '" + internalName + "' has an invalid spell-on-tick defined!");
		}

		durationSpell = new Subspell(durationSpellName);
		if (!durationSpell.process() || !durationSpell.isTargetedLocationSpell()) {
			durationSpell = null;
			if (!durationSpellName.isEmpty()) MagicSpells.error("ProjectileSpell '" + internalName + "' has an invalid spell-after-duration defined!");
		}

		modifierSpell = new Subspell(modifierSpellName);
		if (!modifierSpell.process() || !modifierSpell.isTargetedLocationSpell()) {
			if (!modifierSpellName.isEmpty()) MagicSpells.error("ProjectileSpell '" + internalName + "' has an invalid spell-on-modifier-fail defined!");
			modifierSpell = null;
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
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
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

			if (hitSpell != null && hitSpell.isTargetedEntitySpell()) hitSpell.castAtEntity(monitor.caster, entity, monitor.power);
			else if (hitSpell != null && hitSpell.isTargetedLocationSpell()) hitSpell.castAtLocation(monitor.caster, entity.getLocation(), monitor.power);
			playSpellEffects(EffectPosition.TARGET, entity);
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
			if (monitor.caster != null && groundSpell != null) groundSpell.castAtLocation(monitor.caster, projectile.getLocation(), monitor.power);
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
			taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);

			Vector startDir = startLocation.clone().getDirection().normalize();
			Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
			startLocation.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
			startLocation.add(startLocation.getDirection().multiply(relativeOffset.getX()));
			startLocation.setY(startLocation.getY() + relativeOffset.getY());

			playSpellEffects(EffectPosition.CASTER, startLocation);

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

			playSpellEffects(EffectPosition.PROJECTILE, projectile);
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
				if (modifierSpell != null) modifierSpell.castAtLocation(caster, currentLocation, power);
				if (stopOnModifierFail) stop();
				return;
			}

			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				if (durationSpell != null) durationSpell.castAtLocation(caster, currentLocation, power);
				stop();
				return;
			}

			currentLocation = projectile.getLocation();
			currentLocation.setDirection(projectile.getVelocity());

			if (counter % tickSpellInterval == 0 && tickSpell != null) tickSpell.castAtLocation(caster, currentLocation, power);

			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) playSpellEffects(EffectPosition.SPECIAL, currentLocation);

			counter++;

			for (Entity e : projectile.getNearbyEntities(hitRadius, verticalHitRadius, hitRadius)) {
				if (!(e instanceof LivingEntity)) continue;
				if (!validTargetList.canTarget(caster, e)) continue;

				SpellTargetEvent event = new SpellTargetEvent(ProjectileSpell.this, caster, (LivingEntity) e, power);
				EventUtil.call(event);
				if (!event.isCancelled()) {
					if (hitSpell != null) hitSpell.castAtEntity(caster, (LivingEntity) e, event.getPower());
					stop();
					return;
				}
			}
		}

		private void stop() {
			playSpellEffects(EffectPosition.DELAYED, currentLocation);
			MagicSpells.cancelTask(taskId);
			caster = null;
			currentLocation = null;
			if (projectile != null) projectile.remove();
			projectile = null;
		}

	}

}
