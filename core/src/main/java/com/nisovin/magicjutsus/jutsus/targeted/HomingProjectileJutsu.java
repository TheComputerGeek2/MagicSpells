package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.BoundingBox;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.castmodifiers.ModifierSet;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.util.projectile.ProjectileManager;
import com.nisovin.magicjutsus.util.projectile.ProjectileManagers;
import com.nisovin.magicjutsus.jutsus.TargetedEntityFromLocationJutsu;

public class HomingProjectileJutsu extends TargetedJutsu implements TargetedEntityJutsu, TargetedEntityFromLocationJutsu {

	private HomingProjectileJutsu thisJutsu;

	private List<HomingProjectileMonitor> monitors;

	private ProjectileManager projectileManager;

	private Vector relativeOffset;
	private Vector targetRelativeOffset;

	private int tickInterval;
	private int airJutsuInterval;
	private int specialEffectInterval;
	private int intermediateSpecialEffects;

	private float velocity;
	private float hitRadius;
	private float verticalHitRadius;

	private boolean stopOnModifierFail;

	private double maxDuration;

	private String hitJutsuName;
	private String airJutsuName;
	private String projectileName;
	private String groundJutsuName;
	private String modifierJutsuName;
	private String durationJutsuName;

	private Ninjutsu hitJutsu;
	private Ninjutsu airJutsu;
	private Ninjutsu groundJutsu;
	private Ninjutsu modifierJutsu;
	private Ninjutsu durationJutsu;

	private ModifierSet homingModifiers;
	private List<String> homingModifiersStrings;

	public HomingProjectileJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		thisJutsu = this;

		monitors = new ArrayList<>();

		projectileManager = ProjectileManagers.getManager(getConfigString("projectile-type",  "arrow"));

		relativeOffset = getConfigVector("relative-offset", "0.5,0.5,0");
		targetRelativeOffset = getConfigVector("target-relative-offset", "0,0.5,0");

		tickInterval = getConfigInt("tick-interval", 1);
		airJutsuInterval = getConfigInt("jutsu-interval", 20);
		specialEffectInterval = getConfigInt("special-effect-interval", 0);
		intermediateSpecialEffects = getConfigInt("intermediate-special-effect-locations", 0);

		velocity = getConfigFloat("velocity", 1F);
		hitRadius = getConfigFloat("hit-radius", 2F);
		verticalHitRadius = getConfigFloat("vertical-hit-radius", 2F);

		stopOnModifierFail = getConfigBoolean("stop-on-modifier-fail", true);

		maxDuration = getConfigDouble("max-duration", 10) * (double) TimeUtil.MILLISECONDS_PER_SECOND;

		hitJutsuName = getConfigString("jutsu", "");
		airJutsuName = getConfigString("jutsu-on-hit-air", "");
		projectileName = ChatColor.translateAlternateColorCodes('&', getConfigString("projectile-name", ""));
		groundJutsuName = getConfigString("jutsu-on-hit-ground", "");
		modifierJutsuName = getConfigString("jutsu-on-modifier-fail", "");
		durationJutsuName = getConfigString("jutsu-after-duration", "");

		homingModifiersStrings = getConfigStringList("homing-modifiers", null);

		if (intermediateSpecialEffects < 0) intermediateSpecialEffects = 0;
	}

	@Override
	public void initialize() {
		super.initialize();

		if (homingModifiersStrings != null && !homingModifiersStrings.isEmpty()) {
			homingModifiers = new ModifierSet(homingModifiersStrings);
			homingModifiersStrings = null;
		}

		hitJutsu = new Ninjutsu(hitJutsuName);
		if (!hitJutsu.process()) {
			hitJutsu = null;
			if (!hitJutsuName.isEmpty()) MagicJutsus.error("HomingMissileJutsu '" + internalName + "' has an invalid jutsu defined!");
		}

		groundJutsu = new Ninjutsu(groundJutsuName);
		if (!groundJutsu.process() || !groundJutsu.isTargetedLocationJutsu()) {
			groundJutsu = null;
			if (!groundJutsuName.isEmpty()) MagicJutsus.error("HomingMissileJutsu '" + internalName + "' has an invalid jutsu-on-hit-ground defined!");
		}

		airJutsu = new Ninjutsu(airJutsuName);
		if (!airJutsu.process() || !airJutsu.isTargetedLocationJutsu()) {
			airJutsu = null;
			if (!airJutsuName.isEmpty()) MagicJutsus.error("HomingMissileJutsu '" + internalName + "' has an invalid jutsu-on-hit-air defined!");
		}

		durationJutsu = new Ninjutsu(durationJutsuName);
		if (!durationJutsu.process() || !durationJutsu.isTargetedLocationJutsu()) {
			durationJutsu = null;
			if (!durationJutsuName.isEmpty()) MagicJutsus.error("HomingMissileJutsu '" + internalName + "' has an invalid jutsu-after-duration defined!");
		}

		modifierJutsu = new Ninjutsu(modifierJutsuName);
		if (!modifierJutsu.process() || !modifierJutsu.isTargetedLocationJutsu()) {
			if (!modifierJutsuName.isEmpty()) MagicJutsus.error("HomingMissileJutsu '" + internalName + "' has an invalid jutsu-on-modifier-fail defined!");
			modifierJutsu = null;
		}
	}

	@Override
	public void turnOff() {
		for (HomingProjectileMonitor monitor : monitors) {
			monitor.stop();
		}

		monitors.clear();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);
			new HomingProjectileMonitor(livingEntity, targetInfo.getTarget(), targetInfo.getPower());
			sendMessages(livingEntity, targetInfo.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		new HomingProjectileMonitor(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		new HomingProjectileMonitor(caster, from, target, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return false;
	}

	@EventHandler
	public void onProjectileHit(EntityDamageByEntityEvent event) {
		if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) return;
		if (!(event.getEntity() instanceof LivingEntity)) return;
		LivingEntity entity = (LivingEntity) event.getEntity();
		Entity damagerEntity = event.getDamager();
		if (!(damagerEntity instanceof Projectile)) return;

		Projectile projectile = (Projectile) damagerEntity;
		for (HomingProjectileMonitor monitor : monitors) {
			if (monitor.projectile == null) continue;
			if (!monitor.projectile.equals(projectile)) continue;
			if (monitor.target == null) continue;
			if (!monitor.target.equals(entity)) continue;

			if (hitJutsu.isTargetedEntityJutsu()) hitJutsu.castAtEntity(monitor.caster, entity, monitor.power);
			else if (hitJutsu.isTargetedLocationJutsu()) hitJutsu.castAtLocation(monitor.caster, entity.getLocation(), monitor.power);
			playJutsuEffects(EffectPosition.TARGET, entity);
			event.setCancelled(true);

			monitor.stop();
			break;
		}
	}

	@EventHandler
	public void onProjectileBlockHit(ProjectileHitEvent e) {
		Projectile projectile = e.getEntity();
		Block block = e.getHitBlock();
		if (block == null) return;
		for (HomingProjectileMonitor monitor : monitors) {
			if (monitor.projectile == null) continue;
			if (!monitor.projectile.equals(projectile)) continue;
			if (monitor.caster == null) continue;
			if (groundJutsu != null) groundJutsu.castAtLocation(monitor.caster, projectile.getLocation(), monitor.power);
			monitor.stop();
		}

	}

	private class HomingProjectileMonitor implements Runnable {

		private Projectile projectile;
		private Location currentLocation;
		private Location previousLocation;
		private Location startLocation;
		private LivingEntity caster;
		private LivingEntity target;
		private BoundingBox hitBox;
		private Vector currentVelocity;
		private float power;
		private long startTime;

		private int taskId;
		private int counter = 0;

		private HomingProjectileMonitor(LivingEntity caster, LivingEntity target, float power) {
			this.caster = caster;
			this.target = target;
			this.power = power;
			startLocation = caster.getLocation();

			initialize();
		}

		private HomingProjectileMonitor(LivingEntity caster, Location startLocation, LivingEntity target, float power) {
			this.caster = caster;
			this.target = target;
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

			hitBox = new BoundingBox(startLocation, hitRadius, verticalHitRadius);

			playJutsuEffects(EffectPosition.CASTER, startLocation);

			projectile = startLocation.getWorld().spawn(startLocation, projectileManager.getProjectileClass());
			if (!projectileName.isEmpty()) {
				projectile.setCustomName(projectileName);
				projectile.setCustomNameVisible(true);
			}

			currentVelocity = target.getLocation().add(0, 0.75, 0).toVector().subtract(projectile.getLocation().toVector()).normalize();
			currentVelocity.multiply(velocity * power);
			currentVelocity.setY(currentVelocity.getY() + 0.15);
			projectile.setVelocity(currentVelocity);

			playJutsuEffects(EffectPosition.PROJECTILE, projectile);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, startLocation, projectile.getLocation(), caster, projectile);
			monitors.add(this);
		}

		@Override
		public void run() {
			if ((caster != null && !caster.isValid()) || !target.isValid()) {
				stop();
				return;
			}

			if (projectile == null || projectile.isDead()) {
				stop();
				return;
			}

			if (!projectile.getLocation().getWorld().equals(target.getWorld())) {
				stop();
				return;
			}

			if (homingModifiers != null && !homingModifiers.check(caster)) {
				if (modifierJutsu != null) modifierJutsu.castAtLocation(caster, currentLocation, power);
				if (stopOnModifierFail) stop();
				return;
			}

			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				if (durationJutsu != null) durationJutsu.castAtLocation(caster, currentLocation, power);
				stop();
				return;
			}

			previousLocation = projectile.getLocation();

			Vector oldVelocity = new Vector(currentVelocity.getX(), currentVelocity.getY(), currentVelocity.getZ());

			Location targetLoc = target.getLocation().clone();
			Vector startDir = targetLoc.clone().getDirection().normalize();
			Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
			targetLoc.add(horizOffset.multiply(targetRelativeOffset.getZ())).getBlock().getLocation();
			targetLoc.add(targetLoc.getDirection().multiply(targetRelativeOffset.getX()));
			targetLoc.setY(target.getLocation().getY() + targetRelativeOffset.getY());

			currentVelocity = targetLoc.toVector().subtract(projectile.getLocation().toVector()).normalize();
			currentVelocity.multiply(velocity * power);
			currentVelocity.setY(currentVelocity.getY() + 0.15);
			projectile.setVelocity(currentVelocity);
			currentLocation = projectile.getLocation();

			if (counter % airJutsuInterval == 0 && airJutsu != null) airJutsu.castAtLocation(caster, currentLocation, power);

			if (intermediateSpecialEffects > 0) playIntermediateEffectLocations(previousLocation, oldVelocity);

			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) playJutsuEffects(EffectPosition.SPECIAL, currentLocation);

			counter++;

			hitBox.setCenter(currentLocation);
			if (hitBox.contains(targetLoc)) {
				JutsuTargetEvent targetEvent = new JutsuTargetEvent(thisJutsu, caster, target, power);
				EventUtil.call(targetEvent);
				if (targetEvent.isCancelled()) return;
				playJutsuEffects(EffectPosition.TARGET, target);
				if (hitJutsu.isTargetedEntityJutsu()) hitJutsu.castAtEntity(caster, target, power);
				else if (hitJutsu.isTargetedLocationJutsu()) hitJutsu.castAtLocation(caster, target.getLocation(), power);
				stop();
			}
		}

		private void playIntermediateEffectLocations(Location old, Vector movement) {
			int divideFactor = intermediateSpecialEffects + 1;
			movement.setX(movement.getX() / divideFactor);
			movement.setY(movement.getY() / divideFactor);
			movement.setZ(movement.getZ() / divideFactor);
			for (int i = 0; i < intermediateSpecialEffects; i++) {
				old = old.add(movement).setDirection(movement);
				playJutsuEffects(EffectPosition.SPECIAL, old);
			}
		}

		private void stop() {
			playJutsuEffects(EffectPosition.DELAYED, currentLocation);
			MagicJutsus.cancelTask(taskId);
			caster = null;
			target = null;
			currentLocation = null;
			if (projectile != null) projectile.remove();
			projectile = null;
		}

	}

}
