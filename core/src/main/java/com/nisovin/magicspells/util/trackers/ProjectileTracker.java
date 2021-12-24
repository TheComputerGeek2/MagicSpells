package com.nisovin.magicspells.util.trackers;

import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.TrackerMoveEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.instant.ProjectileSpell;
import com.nisovin.magicspells.util.projectile.ProjectileManager;

public class ProjectileTracker implements Runnable, Tracker {

	private final Random rand = ThreadLocalRandom.current();

	private ProjectileSpell spell;

	private NoMagicZoneManager zoneManager;

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
	private boolean callEvents;
	private boolean stopOnModifierFail;

	private double maxDuration;

	private String projectileName;

	private Subspell hitSpell;
	private Subspell tickSpell;
	private Subspell groundSpell;
	private Subspell modifierSpell;
	private Subspell durationSpell;
	private Subspell entityLocationSpell;

	private ModifierSet projectileModifiers;

	private Projectile projectile;
	private Location previousLocation;
	private Location currentLocation;
	private Location startLocation;
	private LivingEntity caster;
	private Vector currentVelocity;
	private String[] args;
	private float power;
	private long startTime;

	private ValidTargetList targetList;

	private int taskId;
	private int counter = 0;

	private boolean stopped = false;

	public ProjectileTracker(LivingEntity caster, Location startLocation, float power, String[] args) {
		this.caster = caster;
		this.power = power;
		this.args = args;
		this.startLocation = startLocation;

	}

	public void start() {
		initialize();
	}

	@Override
	public void initialize() {
		zoneManager = MagicSpells.getNoMagicZoneManager();
		startTime = System.currentTimeMillis();
		taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);

		Vector startDir = startLocation.clone().getDirection().normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
		startLocation.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		startLocation.add(startLocation.getDirection().multiply(relativeOffset.getX()));
		startLocation.setY(startLocation.getY() + relativeOffset.getY());

		currentLocation = startLocation.clone();

		projectile = startLocation.getWorld().spawn(startLocation, projectileManager.getProjectileClass());
		currentVelocity = startLocation.getDirection();
		currentVelocity.multiply(velocity * power);
		if (rotation != 0) Util.rotateVector(currentVelocity, rotation);
		if (horizSpread > 0 || vertSpread > 0) {
			float rx = -1 + rand.nextFloat() * (1 + 1);
			float ry = -1 + rand.nextFloat() * (1 + 1);
			float rz = -1 + rand.nextFloat() * (1 + 1);
			currentVelocity.add(new Vector(rx * horizSpread, ry * vertSpread, rz * horizSpread));
		}
		projectile.setVelocity(currentVelocity);
		projectile.setGravity(gravity);
		projectile.setShooter(caster);
		if (!projectileName.isEmpty()) {
			projectile.setCustomName(projectileName);
			projectile.setCustomNameVisible(true);
		}
		if (projectile instanceof WitherSkull witherSkull) witherSkull.setCharged(charged);
		if (projectile instanceof Explosive explosive) explosive.setIsIncendiary(incendiary);

		if (spell != null) {
			spell.playEffects(EffectPosition.CASTER, startLocation);
			spell.playEffects(EffectPosition.PROJECTILE, projectile);
			spell.playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, startLocation, projectile.getLocation(), caster, projectile);
		}
		ProjectileSpell.getProjectileTrackers().add(this);
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

		if (zoneManager.willFizzle(currentLocation, spell)) {
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

		previousLocation = currentLocation.clone();
		currentLocation = projectile.getLocation().clone();
		currentLocation.setDirection(projectile.getVelocity());

		if (callEvents) {
			TrackerMoveEvent trackerMoveEvent = new TrackerMoveEvent(this, previousLocation, currentLocation);
			EventUtil.call(trackerMoveEvent);
			if (stopped) return;
		}

		if (counter % tickSpellInterval == 0 && tickSpell != null) tickSpell.castAtLocation(caster, currentLocation, power);

		if (spell != null && specialEffectInterval > 0 && counter % specialEffectInterval == 0) spell.playEffects(EffectPosition.SPECIAL, currentLocation);

		counter++;

		for (Entity e : projectile.getNearbyEntities(hitRadius, verticalHitRadius, hitRadius)) {
			if (!(e instanceof LivingEntity livingEntity)) continue;
			if (!targetList.canTarget(caster, e)) continue;

			SpellTargetEvent event = new SpellTargetEvent(spell, caster, livingEntity, power, args);
			EventUtil.call(event);
			if (event.isCancelled()) continue;

			if (hitSpell != null) hitSpell.castAtEntity(caster, livingEntity, event.getPower());
			if (entityLocationSpell != null) entityLocationSpell.castAtLocation(caster, currentLocation, event.getPower());

			stop();
			return;
		}
	}

	public void stop() {
		stop(true);
	}

	public void stop(boolean removeTracker) {
		if (spell != null) {
			spell.playEffects(EffectPosition.DELAYED, currentLocation);
			if (removeTracker) ProjectileSpell.getProjectileTrackers().remove(this);
		}
		MagicSpells.cancelTask(taskId);
		caster = null;
		currentLocation = null;
		if (projectile != null) projectile.remove();
		projectile = null;
		stopped = true;
	}

	public ProjectileSpell getSpell() {
		return spell;
	}

	public void setSpell(ProjectileSpell spell) {
		this.spell = spell;
	}

	public NoMagicZoneManager getZoneManager() {
		return zoneManager;
	}

	public void setZoneManager(NoMagicZoneManager zoneManager) {
		this.zoneManager = zoneManager;
	}

	public ProjectileManager getProjectileManager() {
		return projectileManager;
	}

	public void setProjectileManager(ProjectileManager projectileManager) {
		this.projectileManager = projectileManager;
	}

	public Vector getRelativeOffset() {
		return relativeOffset;
	}

	public void setRelativeOffset(Vector relativeOffset) {
		this.relativeOffset = relativeOffset;
	}

	public int getTickInterval() {
		return tickInterval;
	}

	public void setTickInterval(int tickInterval) {
		this.tickInterval = tickInterval;
	}

	public int getTickSpellInterval() {
		return tickSpellInterval;
	}

	public void setTickSpellInterval(int tickSpellInterval) {
		this.tickSpellInterval = tickSpellInterval;
	}

	public int getSpecialEffectInterval() {
		return specialEffectInterval;
	}

	public void setSpecialEffectInterval(int specialEffectInterval) {
		this.specialEffectInterval = specialEffectInterval;
	}

	public float getRotation() {
		return rotation;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public float getVelocity() {
		return velocity;
	}

	public void setVelocity(float velocity) {
		this.velocity = velocity;
	}

	public float getHitRadius() {
		return hitRadius;
	}

	public void setHitRadius(float hitRadius) {
		this.hitRadius = hitRadius;
	}

	public float getVertSpread() {
		return vertSpread;
	}

	public void setVertSpread(float vertSpread) {
		this.vertSpread = vertSpread;
	}

	public float getHorizSpread() {
		return horizSpread;
	}

	public void setHorizSpread(float horizSpread) {
		this.horizSpread = horizSpread;
	}

	public float getVerticalHitRadius() {
		return verticalHitRadius;
	}

	public void setVerticalHitRadius(float verticalHitRadius) {
		this.verticalHitRadius = verticalHitRadius;
	}

	public boolean hasGravity() {
		return gravity;
	}

	public void setGravity(boolean gravity) {
		this.gravity = gravity;
	}

	public boolean isCharged() {
		return charged;
	}

	public void setCharged(boolean charged) {
		this.charged = charged;
	}

	public boolean isIncendiary() {
		return incendiary;
	}

	public void setIncendiary(boolean incendiary) {
		this.incendiary = incendiary;
	}

	public boolean shouldStopOnModifierFail() {
		return stopOnModifierFail;
	}

	public void setStopOnModifierFail(boolean stopOnModifierFail) {
		this.stopOnModifierFail = stopOnModifierFail;
	}

	public double getMaxDuration() {
		return maxDuration;
	}

	public void setMaxDuration(double maxDuration) {
		this.maxDuration = maxDuration;
	}

	public String getProjectileName() {
		return projectileName;
	}

	public void setProjectileName(String projectileName) {
		this.projectileName = projectileName;
	}

	public Subspell getHitSpell() {
		return hitSpell;
	}

	public void setHitSpell(Subspell hitSpell) {
		this.hitSpell = hitSpell;
	}

	public Subspell getTickSpell() {
		return tickSpell;
	}

	public void setTickSpell(Subspell tickSpell) {
		this.tickSpell = tickSpell;
	}

	public Subspell getGroundSpell() {
		return groundSpell;
	}

	public void setGroundSpell(Subspell groundSpell) {
		this.groundSpell = groundSpell;
	}

	public Subspell getModifierSpell() {
		return modifierSpell;
	}

	public void setModifierSpell(Subspell modifierSpell) {
		this.modifierSpell = modifierSpell;
	}

	public Subspell getDurationSpell() {
		return durationSpell;
	}

	public void setDurationSpell(Subspell durationSpell) {
		this.durationSpell = durationSpell;
	}

	public Subspell getEntityLocationSpell() {
		return entityLocationSpell;
	}

	public void setEntityLocationSpell(Subspell entityLocationSpell) {
		this.entityLocationSpell = entityLocationSpell;
	}

	public ModifierSet getProjectileModifiers() {
		return projectileModifiers;
	}

	public void setProjectileModifiers(ModifierSet projectileModifiers) {
		this.projectileModifiers = projectileModifiers;
	}

	public Projectile getProjectile() {
		return projectile;
	}

	public void setProjectile(Projectile projectile) {
		this.projectile = projectile;
	}

	public Location getPreviousLocation() {
		return previousLocation;
	}

	public void setPreviousLocation(Location previousLocation) {
		this.previousLocation = previousLocation;
	}

	public Location getCurrentLocation() {
		return currentLocation;
	}

	public void setCurrentLocation(Location currentLocation) {
		this.currentLocation = currentLocation;
	}

	public Location getStartLocation() {
		return startLocation;
	}

	public void setStartLocation(Location startLocation) {
		this.startLocation = startLocation;
	}

	public LivingEntity getCaster() {
		return caster;
	}

	public void setCaster(LivingEntity caster) {
		this.caster = caster;
	}

	public Vector getCurrentVelocity() {
		return currentVelocity;
	}

	public void setCurrentVelocity(Vector currentVelocity) {
		this.currentVelocity = currentVelocity;
	}

	public float getPower() {
		return power;
	}

	public void setPower(float power) {
		this.power = power;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public ValidTargetList getTargetList() {
		return targetList;
	}

	public void setTargetList(ValidTargetList targetList) {
		this.targetList = targetList;
	}

	public boolean shouldCallEvents() {
		return callEvents;
	}

	public void setCallEvents(boolean callEvents) {
		this.callEvents = callEvents;
	}

	public String[] getArgs() {
		return args;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}
}
