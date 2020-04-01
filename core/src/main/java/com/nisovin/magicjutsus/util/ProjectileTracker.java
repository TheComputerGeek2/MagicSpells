package com.nisovin.magicjutsus.util;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.castmodifiers.ModifierSet;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.events.ParticleProjectileHitEvent;
import com.nisovin.magicjutsus.jutsus.instant.ParticleProjectileJutsu;

public class ProjectileTracker implements Runnable {

	private Random rand = new Random();

	private LivingEntity caster;
	private float power;
	private long startTime;
	private Location startLocation;
	private Location previousLocation;
	private Location currentLocation;
	private Vector currentVelocity;
	private Vector startDirection;
	private int currentX;
	private int currentZ;
	private int counter;
	private int taskId;
	private BoundingBox hitBox;
	private List<Block> nearBlocks;
	private List<LivingEntity> inRange;
	private Set<LivingEntity> immune;
	private int maxHitLimit;
	private ValidTargetChecker entityJutsuChecker;
	private ProjectileTracker tracker;
	private ParticleProjectileJutsu jutsu;
	private Set<Material> groundMaterials;
	private ValidTargetList targetList;
	private ModifierSet projectileModifiers;
	private Map<String, Ninjutsu> interactionJutsus;

	private boolean stopped = false;

	// projectile options
	private double maxDuration;
	private double maxDistanceSquared;

	private boolean hugSurface;
	private boolean changePitch;
	private boolean controllable;
	private boolean stopOnHitGround;
	private boolean stopOnModifierFail;
	private boolean allowCasterInteract;
	private boolean powerAffectsVelocity;

	private boolean hitGround;
	private boolean hitAirAtEnd;
	private boolean hitAirDuring;
	private boolean hitAirAfterDuration;

	private float acceleration;
	private float startXOffset;
	private float startYOffset;
	private float startZOffset;
	private float targetYOffset;
	private float ticksPerSecond;
	private float heightFromSurface;
	private float verticalHitRadius;
	private float horizontalHitRadius;

	private float projectileTurn;
	private float projectileVelocity;
	private float projectileVertOffset;
	private float projectileVertSpread;
	private float projectileHorizOffset;
	private float projectileHorizSpread;
	private float projectileVertGravity;
	private float projectileHorizGravity;

	private int tickInterval;
	private int jutsuInterval;
	private int tickJutsuLimit;
	private int maxEntitiesHit;
	private int accelerationDelay;
	private int intermediateEffects;
	private int intermediateHitboxes;
	private int specialEffectInterval;
	private int groundVerticalHitRadius;
	private int groundHorizontalHitRadius;

	private Ninjutsu airJutsu;
	private Ninjutsu tickJutsu;
	private Ninjutsu entityJutsu;
	private Ninjutsu casterJutsu;
	private Ninjutsu groundJutsu;
	private Ninjutsu durationJutsu;
	private Ninjutsu modifierJutsu;
	private Ninjutsu entityLocationJutsu;

	private int ticks = 0;

	public ProjectileTracker() {

	}

	public ProjectileTracker(LivingEntity caster, float power) {
		this.caster = caster;
		this.power = power;
	}

	public void start(Location from) {
		startTime = System.currentTimeMillis();
		if (!changePitch) from.setPitch(0F);
		startLocation = from.clone();

		// Changing the start location
		startDirection = caster.getLocation().getDirection().normalize();
		Vector horizOffset = new Vector(-startDirection.getZ(), 0.0, startDirection.getX()).normalize();
		startLocation.add(horizOffset.multiply(startZOffset)).getBlock().getLocation();
		startLocation.add(startLocation.getDirection().multiply(startXOffset));
		startLocation.setY(startLocation.getY() + startYOffset);

		previousLocation = startLocation.clone();
		currentLocation = startLocation.clone();
		currentVelocity = from.getDirection();

		init();
	}

	public void startTarget(Location from, LivingEntity target) {
		startTarget(from, target.getLocation());
	}

	public void startTarget(Location from, Location target) {
		startTime = System.currentTimeMillis();
		if (!changePitch) from.setPitch(0F);
		startLocation = from.clone();

		// Changing the target location
		Location targetLoc = target.clone();
		targetLoc.add(0, targetYOffset,0);
		Vector dir = targetLoc.clone().subtract(from.clone()).toVector();

		// Changing the start location
		startDirection = dir.clone().normalize();
		Vector horizOffset = new Vector(-startDirection.getZ(), 0.0, startDirection.getX()).normalize();
		startLocation.add(horizOffset.multiply(startZOffset)).getBlock().getLocation();
		startLocation.add(startLocation.getDirection().multiply(startXOffset));
		startLocation.setY(startLocation.getY() + startYOffset);

		dir = targetLoc.clone().subtract(startLocation.clone()).toVector();

		previousLocation = startLocation.clone();
		currentLocation = startLocation.clone();
		currentVelocity = from.setDirection(dir).getDirection();

		init();
	}

	private void init() {
		counter = 0;
		if (projectileHorizOffset != 0) Util.rotateVector(currentVelocity, projectileHorizOffset);
		if (projectileVertOffset != 0) currentVelocity.add(new Vector(0, projectileVertOffset, 0)).normalize();
		if (projectileVertSpread > 0 || projectileHorizSpread > 0) {
			float rx = -1 + rand.nextFloat() * (1 + 1);
			float ry = -1 + rand.nextFloat() * (1 + 1);
			float rz = -1 + rand.nextFloat() * (1 + 1);
			currentVelocity.add(new Vector(rx * projectileHorizSpread, ry * projectileVertSpread, rz * projectileHorizSpread));
		}
		if (hugSurface) {
			currentLocation.setY(currentLocation.getY() + heightFromSurface);
			currentVelocity.setY(0).normalize();
			currentLocation.setPitch(0);
		}
		if (powerAffectsVelocity) currentVelocity.multiply(power);
		currentVelocity.multiply(projectileVelocity / ticksPerSecond);
		nearBlocks = new ArrayList<>();
		if (targetList != null) inRange = new ArrayList<>();
		immune = new HashSet<>();
		maxHitLimit = 0;
		hitBox = new BoundingBox(currentLocation, horizontalHitRadius, verticalHitRadius);
		currentLocation.setDirection(currentVelocity);
		tracker = this;
		if (jutsu != null) ParticleProjectileJutsu.getProjectileTrackers().add(tracker);
		taskId = MagicJutsus.scheduleRepeatingTask(this, 0, tickInterval);
	}

	@Override
	public void run() {
		currentVelocity = Util.makeFinite(currentVelocity);
		currentLocation = Util.makeFinite(currentLocation);
		previousLocation = Util.makeFinite(previousLocation);

		if (caster != null && !caster.isValid()) {
			stop(true);
			return;
		}

		if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
			if (hitAirAfterDuration && durationJutsu != null && durationJutsu.isTargetedLocationJutsu()) {
				durationJutsu.castAtLocation(caster, currentLocation, power);
				if (jutsu != null) jutsu.playEffects(EffectPosition.TARGET, currentLocation);
			}
			stop(true);
			return;
		}

		if (projectileModifiers != null && !projectileModifiers.check(caster)) {
			if (modifierJutsu != null) modifierJutsu.castAtLocation(caster, currentLocation, power);
			if (stopOnModifierFail) stop(true);
			return;
		}

		if (controllable) {
			currentVelocity = caster.getLocation().getDirection();
			if (hugSurface) currentVelocity.setY(0).normalize();
			currentVelocity.multiply(projectileVelocity / ticksPerSecond);
			currentLocation.setDirection(currentVelocity);
		}

		currentVelocity = Util.makeFinite(currentVelocity);

		// Move projectile and apply gravity
		previousLocation = currentLocation.clone();
		currentLocation.add(currentVelocity);

		currentLocation = Util.makeFinite(currentLocation);
		previousLocation = Util.makeFinite(previousLocation);

		if (hugSurface && (currentLocation.getBlockX() != currentX || currentLocation.getBlockZ() != currentZ)) {
			Block b = currentLocation.subtract(0, heightFromSurface, 0).getBlock();

			int attempts = 0;
			boolean ok = false;
			while (attempts++ < 10) {
				if (BlockUtils.isPathable(b)) {
					b = b.getRelative(BlockFace.DOWN);
					if (BlockUtils.isPathable(b)) currentLocation.add(0, -1, 0);
					else {
						ok = true;
						break;
					}
				} else {
					b = b.getRelative(BlockFace.UP);
					currentLocation.add(0, 1, 0);
					if (BlockUtils.isPathable(b)) {
						ok = true;
						break;
					}
				}
			}
			if (!ok) {
				stop(true);
				return;
			}

			currentLocation.setY((int) currentLocation.getY() + heightFromSurface);
			currentX = currentLocation.getBlockX();
			currentZ = currentLocation.getBlockZ();

			// Apply vertical gravity
		} else if (projectileVertGravity != 0) currentVelocity.setY(currentVelocity.getY() - (projectileVertGravity / ticksPerSecond));

		// Apply turn
		if (projectileTurn != 0) Util.rotateVector(currentVelocity, projectileTurn);

		// Apply horizontal gravity
		if (projectileHorizGravity != 0) Util.rotateVector(currentVelocity, (projectileHorizGravity / ticksPerSecond) * counter);

		// Rotate effects properly
		currentLocation.setDirection(currentVelocity);

		// Play effects
		if (jutsu != null && specialEffectInterval > 0 && counter % specialEffectInterval == 0) jutsu.playEffects(EffectPosition.SPECIAL, currentLocation);

		// Acceleration
		if (acceleration != 0 && accelerationDelay > 0 && counter % accelerationDelay == 0) currentVelocity.multiply(acceleration);

		// Intermediate effects
		if (intermediateEffects > 0) playIntermediateEffects(previousLocation, currentVelocity);

		// Intermediate hitboxes
		if (intermediateHitboxes > 0) checkIntermediateHitboxes(previousLocation, currentVelocity);

		if (stopped) return;

		counter++;

		// Cast jutsu mid air
		if (hitAirDuring && counter % jutsuInterval == 0 && tickJutsu != null) {
			if (tickJutsuLimit <= 0 || ticks < tickJutsuLimit) {
				tickJutsu.castAtLocation(caster, currentLocation.clone(), power);
				ticks++;
			}
		}

		if (groundHorizontalHitRadius == 0 || groundVerticalHitRadius == 0) {
			nearBlocks = new ArrayList<>();
			nearBlocks.add(currentLocation.getBlock());
		} else nearBlocks = BlockUtils.getNearbyBlocks(currentLocation, groundHorizontalHitRadius, groundVerticalHitRadius);

		for (Block b : nearBlocks) {
			if (!groundMaterials.contains(b.getType())) continue;
			if (hitGround && groundJutsu != null) {
				Util.setLocationFacingFromVector(previousLocation, currentVelocity);
				groundJutsu.castAtLocation(caster, previousLocation, power);
				if (jutsu != null) jutsu.playEffects(EffectPosition.TARGET, currentLocation);
			}
			if (stopOnHitGround) {
				stop(true);
				return;
			}
		}

		if (currentLocation.distanceSquared(startLocation) >= maxDistanceSquared) {
			if (hitAirAtEnd && airJutsu != null) {
				airJutsu.castAtLocation(caster, currentLocation.clone(), power);
				if (jutsu != null) jutsu.playEffects(EffectPosition.TARGET, currentLocation);
			}
			stop(true);
			return;
		}

		checkHitbox(currentLocation);
		if (stopped) return;

		if (jutsu == null || interactionJutsus == null || interactionJutsus.isEmpty()) return;
		Set<ProjectileTracker> toRemove = new HashSet<>();
		Set<ProjectileTracker> trackers = new HashSet<>(ParticleProjectileJutsu.getProjectileTrackers());
		for (ProjectileTracker collisionTracker : trackers) {
			if (collisionTracker == null) continue;
			if (tracker == null) continue;
			if (tracker.caster == null) continue;
			if (collisionTracker.caster == null) continue;
			if (collisionTracker.equals(tracker)) continue;
			if (!interactionJutsus.containsKey(collisionTracker.jutsu.getInternalName())) continue;
			if (!collisionTracker.currentLocation.getWorld().equals(tracker.currentLocation.getWorld())) continue;
			if (!collisionTracker.hitBox.contains(tracker.currentLocation) && !tracker.hitBox.contains(collisionTracker.currentLocation)) continue;
			if (!allowCasterInteract && collisionTracker.caster.equals(tracker.caster)) continue;

			Ninjutsu collisionJutsu = interactionJutsus.get(collisionTracker.jutsu.getInternalName());
			if (collisionJutsu == null) {
				toRemove.add(collisionTracker);
				toRemove.add(tracker);
				collisionTracker.stop(false);
				tracker.stop(false);
				continue;
			}

			double x = (tracker.currentLocation.getX() + collisionTracker.currentLocation.getX()) / 2D;
			double y = (tracker.currentLocation.getY() + collisionTracker.currentLocation.getY()) / 2D;
			double z = (tracker.currentLocation.getZ() + collisionTracker.currentLocation.getZ()) / 2D;

			Location middleLoc = new Location(tracker.currentLocation.getWorld(), x, y, z);
			collisionJutsu.castAtLocation(tracker.caster, middleLoc, tracker.power);
			toRemove.add(collisionTracker);
			toRemove.add(tracker);
			collisionTracker.stop(false);
			tracker.stop(false);
		}

		ParticleProjectileJutsu.getProjectileTrackers().removeAll(toRemove);
		toRemove.clear();
		trackers.clear();
	}

	private void playIntermediateEffects(Location old, Vector movement) {
		int divideFactor = intermediateEffects + 1;
		Vector v = movement.clone();

		v.setX(v.getX() / divideFactor);
		v.setY(v.getY() / divideFactor);
		v.setZ(v.getZ() / divideFactor);

		for (int i = 0; i < intermediateEffects; i++) {
			old = old.add(v).setDirection(v);
			if (jutsu != null && specialEffectInterval > 0 && counter % specialEffectInterval == 0) jutsu.playEffects(EffectPosition.SPECIAL, old);
		}
	}

	private void checkIntermediateHitboxes(Location old, Vector movement) {
		int divideFactor = intermediateHitboxes + 1;
		Vector v = movement.clone();

		v.setX(v.getX() / divideFactor);
		v.setY(v.getY() / divideFactor);
		v.setZ(v.getZ() / divideFactor);

		for (int i = 0; i < intermediateHitboxes; i++) {
			old = old.add(v).setDirection(v);
			checkHitbox(old);
		}
	}

	private void checkHitbox(Location currentLoc) {
		if (inRange == null) return;
		if (currentLoc == null) return;
		if (currentLoc.getWorld() == null) return;

		hitBox.setCenter(currentLoc);
		inRange.clear();

		double x = horizontalHitRadius / 2D;
		double y = verticalHitRadius / 2D;
		double z = horizontalHitRadius / 2D;

		for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, x, y, z)) {
			if (!(entity instanceof LivingEntity)) continue;
			if (!targetList.canTarget(caster, entity)) continue;
			if (immune.contains(entity)) continue;
			inRange.add((LivingEntity) entity);
		}

		for (int i = 0; i < inRange.size(); i++) {
			LivingEntity e = inRange.get(i);
			if (e.isDead()) continue;

			ParticleProjectileHitEvent projectileEvent = new ParticleProjectileHitEvent(caster, e, tracker, jutsu, power);
			EventUtil.call(projectileEvent);
			if (projectileEvent.isCancelled()) {
				inRange.remove(i);
				break;
			}

			if (entityJutsu != null && entityJutsu.isTargetedEntityJutsu()) {
				entityJutsuChecker = entityJutsu.getJutsu().getValidTargetChecker();
				if (entityJutsuChecker != null && !entityJutsuChecker.isValidTarget(e)) {
					inRange.remove(i);
					break;
				}
				JutsuTargetEvent event = new JutsuTargetEvent(jutsu, caster, e, power);
				EventUtil.call(event);
				if (event.isCancelled()) {
					inRange.remove(i);
					break;
				} else {
					e = event.getTarget();
					power = event.getPower();
				}

				entityJutsu.castAtEntity(caster, e, power);
				if (jutsu != null) jutsu.playEffects(EffectPosition.TARGET, e);
			} else if (entityJutsu != null && entityJutsu.isTargetedLocationJutsu()) {
				entityJutsu.castAtLocation(caster, currentLoc.clone(), power);
				if (jutsu != null) jutsu.playEffects(EffectPosition.TARGET, currentLoc);
			}

			if (entityLocationJutsu != null) entityLocationJutsu.castAtLocation(caster, currentLoc, power);

			inRange.remove(i);
			immune.add(e);
			maxHitLimit++;

			if (maxEntitiesHit > 0 && maxHitLimit >= maxEntitiesHit) stop(true);
			break;
		}
	}

	public void stop(boolean removeTracker) {
		if (removeTracker && jutsu != null) ParticleProjectileJutsu.getProjectileTrackers().remove(tracker);
		if (jutsu != null) jutsu.playEffects(EffectPosition.DELAYED, currentLocation);
		MagicJutsus.cancelTask(taskId);
		caster = null;
		startLocation = null;
		previousLocation = null;
		currentLocation = null;
		currentVelocity = null;
		if (immune != null) {
			immune.clear();
			immune = null;
		}
		if (inRange != null) {
			inRange.clear();
			inRange = null;
		}
		stopped = true;
	}

	public LivingEntity getCaster() {
		return caster;
	}

	public void setCaster(LivingEntity caster) {
		this.caster = caster;
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

	public Location getStartLocation() {
		return startLocation;
	}

	public void setStartLocation(Location startLocation) {
		this.startLocation = startLocation;
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

	public Vector getCurrentVelocity() {
		return currentVelocity;
	}

	public void setCurrentVelocity(Vector currentVelocity) {
		this.currentVelocity = currentVelocity;
	}

	public Vector getStartDirection() {
		return startDirection;
	}

	public void setStartDirection(Vector startDirection) {
		this.startDirection = startDirection;
	}

	public int getCurrentX() {
		return currentX;
	}

	public void setCurrentX(int currentX) {
		this.currentX = currentX;
	}

	public int getCurrentZ() {
		return currentZ;
	}

	public void setCurrentZ(int currentZ) {
		this.currentZ = currentZ;
	}

	public int getTaskId() {
		return taskId;
	}

	public void setTaskId(int taskId) {
		this.taskId = taskId;
	}

	public BoundingBox getHitBox() {
		return hitBox;
	}

	public void setHitBox(BoundingBox hitBox) {
		this.hitBox = hitBox;
	}

	public List<LivingEntity> getInRange() {
		return inRange;
	}

	public void setInRange(List<LivingEntity> inRange) {
		this.inRange = inRange;
	}

	public int getMaxHitLimit() {
		return maxHitLimit;
	}

	public void setMaxHitLimit(int maxHitLimit) {
		this.maxHitLimit = maxHitLimit;
	}

	public Set<LivingEntity> getImmune() {
		return immune;
	}

	public void setImmune(Set<LivingEntity> immune) {
		this.immune = immune;
	}

	public ValidTargetChecker getEntityJutsuChecker() {
		return entityJutsuChecker;
	}

	public void setEntityJutsuChecker(ValidTargetChecker entityJutsuChecker) {
		this.entityJutsuChecker = entityJutsuChecker;
	}

	public ProjectileTracker getTracker() {
		return tracker;
	}

	public void setTracker(ProjectileTracker tracker) {
		this.tracker = tracker;
	}

	public ParticleProjectileJutsu getJutsu() {
		return jutsu;
	}

	public void setJutsu(ParticleProjectileJutsu jutsu) {
		this.jutsu = jutsu;
	}

	public Set<Material> getGroundMaterials() {
		return groundMaterials;
	}

	public void setGroundMaterials(Set<Material> groundMaterials) {
		this.groundMaterials = groundMaterials;
	}

	public ValidTargetList getTargetList() {
		return targetList;
	}

	public void setTargetList(ValidTargetList targetList) {
		this.targetList = targetList;
	}

	public ModifierSet getProjectileModifiers() {
		return projectileModifiers;
	}

	public void setProjectileModifiers(ModifierSet projectileModifiers) {
		this.projectileModifiers = projectileModifiers;
	}

	public Map<String, Ninjutsu> getInteractionJutsus() {
		return interactionJutsus;
	}

	public void setInteractionJutsus(Map<String, Ninjutsu> interactionJutsus) {
		this.interactionJutsus = interactionJutsus;
	}

	public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}

	public double getMaxDuration() {
		return maxDuration;
	}

	public void setMaxDuration(double maxDuration) {
		this.maxDuration = maxDuration;
	}

	public double getMaxDistanceSquared() {
		return maxDistanceSquared;
	}

	public void setMaxDistanceSquared(double maxDistanceSquared) {
		this.maxDistanceSquared = maxDistanceSquared;
	}

	public boolean isHugSurface() {
		return hugSurface;
	}

	public void setHugSurface(boolean hugSurface) {
		this.hugSurface = hugSurface;
	}

	public boolean isChangePitch() {
		return changePitch;
	}

	public void setChangePitch(boolean changePitch) {
		this.changePitch = changePitch;
	}

	public boolean isControllable() {
		return controllable;
	}

	public void setControllable(boolean controllable) {
		this.controllable = controllable;
	}

	public boolean isStopOnHitGround() {
		return stopOnHitGround;
	}

	public void setStopOnHitGround(boolean stopOnHitGround) {
		this.stopOnHitGround = stopOnHitGround;
	}

	public boolean isStopOnModifierFail() {
		return stopOnModifierFail;
	}

	public void setStopOnModifierFail(boolean stopOnModifierFail) {
		this.stopOnModifierFail = stopOnModifierFail;
	}

	public boolean isCasterAllowedToInteract() {
		return allowCasterInteract;
	}

	public void setAllowCasterInteract(boolean allowCasterInteract) {
		this.allowCasterInteract = allowCasterInteract;
	}

	public boolean isPowerAffectedByVelocity() {
		return powerAffectsVelocity;
	}

	public void setPowerAffectsVelocity(boolean powerAffectsVelocity) {
		this.powerAffectsVelocity = powerAffectsVelocity;
	}

	public boolean canHitGround() {
		return hitGround;
	}

	public void setHitGround(boolean hitGround) {
		this.hitGround = hitGround;
	}

	public boolean canHitAirAtEnd() {
		return hitAirAtEnd;
	}

	public void setHitAirAtEnd(boolean hitAirAtEnd) {
		this.hitAirAtEnd = hitAirAtEnd;
	}

	public boolean canHitAirDuring() {
		return hitAirDuring;
	}

	public void setHitAirDuring(boolean hitAirDuring) {
		this.hitAirDuring = hitAirDuring;
	}

	public boolean canHitAirAfterDuration() {
		return hitAirAfterDuration;
	}

	public void setHitAirAfterDuration(boolean hitAirAfterDuration) {
		this.hitAirAfterDuration = hitAirAfterDuration;
	}

	public float getAcceleration() {
		return acceleration;
	}

	public void setAcceleration(float acceleration) {
		this.acceleration = acceleration;
	}

	public float getProjectileTurn() {
		return projectileTurn;
	}

	public void setProjectileTurn(float projectileTurn) {
		this.projectileTurn = projectileTurn;
	}

	public float getProjectileVelocity() {
		return projectileVelocity;
	}

	public void setProjectileVelocity(float projectileVelocity) {
		this.projectileVelocity = projectileVelocity;
	}

	public float getProjectileVertOffset() {
		return projectileVertOffset;
	}

	public void setProjectileVertOffset(float projectileVertOffset) {
		this.projectileVertOffset = projectileVertOffset;
	}

	public float getProjectileVertSpread() {
		return projectileVertSpread;
	}

	public void setProjectileVertSpread(float projectileVertSpread) {
		this.projectileVertSpread = projectileVertSpread;
	}

	public float getProjectileHorizOffset() {
		return projectileHorizOffset;
	}

	public void setProjectileHorizOffset(float projectileHorizOffset) {
		this.projectileHorizOffset = projectileHorizOffset;
	}

	public float getProjectileHorizSpread() {
		return projectileHorizSpread;
	}

	public void setProjectileHorizSpread(float projectileHorizSpread) {
		this.projectileHorizSpread = projectileHorizSpread;
	}

	public float getProjectileVertGravity() {
		return projectileVertGravity;
	}

	public void setProjectileVertGravity(float projectileVertGravity) {
		this.projectileVertGravity = projectileVertGravity;
	}

	public float getProjectileHorizGravity() {
		return projectileHorizGravity;
	}

	public void setProjectileHorizGravity(float projectileHorizGravity) {
		this.projectileHorizGravity = projectileHorizGravity;
	}

	public int getTickInterval() {
		return tickInterval;
	}

	public void setTickInterval(int tickInterval) {
		this.tickInterval = tickInterval;
	}

	public int getJutsuInterval() {
		return jutsuInterval;
	}

	public void setJutsuInterval(int jutsuInterval) {
		this.jutsuInterval = jutsuInterval;
	}

	public int getTickJutsuLimit() {
		return tickJutsuLimit;
	}

	public void setTickJutsuLimit(int tickJutsuLimit) {
		this.tickJutsuLimit = tickJutsuLimit;
	}

	public int getMaxEntitiesHit() {
		return maxEntitiesHit;
	}

	public void setMaxEntitiesHit(int maxEntitiesHit) {
		this.maxEntitiesHit = maxEntitiesHit;
	}

	public int getAccelerationDelay() {
		return accelerationDelay;
	}

	public void setAccelerationDelay(int accelerationDelay) {
		this.accelerationDelay = accelerationDelay;
	}

	public int getIntermediateEffects() {
		return intermediateEffects;
	}

	public void setIntermediateEffects(int intermediateEffects) {
		this.intermediateEffects = intermediateEffects;
	}

	public int getIntermediateHitboxes() {
		return intermediateHitboxes;
	}

	public void setIntermediateHitboxes(int intermediateHitboxes) {
		this.intermediateHitboxes = intermediateHitboxes;
	}

	public int getSpecialEffectInterval() {
		return specialEffectInterval;
	}

	public void setSpecialEffectInterval(int specialEffectInterval) {
		this.specialEffectInterval = specialEffectInterval;
	}

	public int getGroundVerticalHitRadius() {
		return groundVerticalHitRadius;
	}

	public void setGroundVerticalHitRadius(int groundVerticalHitRadius) {
		this.groundVerticalHitRadius = groundVerticalHitRadius;
	}

	public int getGroundHorizontalHitRadius() {
		return groundHorizontalHitRadius;
	}

	public void setGroundHorizontalHitRadius(int groundHorizontalHitRadius) {
		this.groundHorizontalHitRadius = groundHorizontalHitRadius;
	}

	public float getStartXOffset() {
		return startXOffset;
	}

	public void setStartXOffset(float startXOffset) {
		this.startXOffset = startXOffset;
	}

	public float getStartYOffset() {
		return startYOffset;
	}

	public void setStartYOffset(float startYOffset) {
		this.startYOffset = startYOffset;
	}

	public float getStartZOffset() {
		return startZOffset;
	}

	public void setStartZOffset(float startZOffset) {
		this.startZOffset = startZOffset;
	}

	public float getTargetYOffset() {
		return targetYOffset;
	}

	public void setTargetYOffset(float targetYOffset) {
		this.targetYOffset = targetYOffset;
	}

	public float getTicksPerSecond() {
		return ticksPerSecond;
	}

	public void setTicksPerSecond(float ticksPerSecond) {
		this.ticksPerSecond = ticksPerSecond;
	}

	public float getHeightFromSurface() {
		return heightFromSurface;
	}

	public void setHeightFromSurface(float heightFromSurface) {
		this.heightFromSurface = heightFromSurface;
	}

	public float getVerticalHitRadius() {
		return verticalHitRadius;
	}

	public void setVerticalHitRadius(float verticalHitRadius) {
		this.verticalHitRadius = verticalHitRadius;
	}

	public float getHorizontalHitRadius() {
		return horizontalHitRadius;
	}

	public void setHorizontalHitRadius(float horizontalHitRadius) {
		this.horizontalHitRadius = horizontalHitRadius;
	}

	public Ninjutsu getAirJutsu() {
		return airJutsu;
	}

	public void setAirJutsu(Ninjutsu airJutsu) {
		this.airJutsu = airJutsu;
	}

	public Ninjutsu getTickJutsu() {
		return tickJutsu;
	}

	public void setTickJutsu(Ninjutsu tickJutsu) {
		this.tickJutsu = tickJutsu;
	}

	public Ninjutsu getEntityJutsu() {
		return entityJutsu;
	}

	public void setEntityJutsu(Ninjutsu entityJutsu) {
		this.entityJutsu = entityJutsu;
	}

	public Ninjutsu getCasterJutsu() {
		return casterJutsu;
	}

	public void setCasterJutsu(Ninjutsu casterJutsu) {
		this.casterJutsu = casterJutsu;
	}

	public Ninjutsu getGroundJutsu() {
		return groundJutsu;
	}

	public void setGroundJutsu(Ninjutsu groundJutsu) {
		this.groundJutsu = groundJutsu;
	}

	public Ninjutsu getDurationJutsu() {
		return durationJutsu;
	}

	public void setDurationJutsu(Ninjutsu durationJutsu) {
		this.durationJutsu = durationJutsu;
	}

	public Ninjutsu getModifierJutsu() {
		return modifierJutsu;
	}

	public void setModifierJutsu(Ninjutsu modifierJutsu) {
		this.modifierJutsu = modifierJutsu;
	}

	public Ninjutsu getEntityLocationJutsu() {
		return entityLocationJutsu;
	}

	public void setEntityLocationJutsu(Ninjutsu entityLocationJutsu) {
		this.entityLocationJutsu = entityLocationJutsu;
	}

}
