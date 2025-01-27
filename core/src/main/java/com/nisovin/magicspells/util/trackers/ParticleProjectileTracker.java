package com.nisovin.magicspells.util.trackers;

import java.util.*;
import java.util.function.Predicate;
import java.util.concurrent.ThreadLocalRandom;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.doubles.DoubleArraySet;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.BoundingBox;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.effect.ModifiedEffect;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.TrackerMoveEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.ParticleProjectileHitEvent;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;
import com.nisovin.magicspells.spells.instant.ParticleProjectileSpell;

public class ParticleProjectileTracker implements Runnable, Tracker {

	private static final Subspell.CastTargeting[] HIT_ORDERING = {
		Subspell.CastTargeting.ENTITY, Subspell.CastTargeting.LOCATION, Subspell.CastTargeting.NONE
	};

	private final Random rand = ThreadLocalRandom.current();

	private NoMagicZoneManager zoneManager;

	private Set<EffectlibSpellEffect> effectSet;
	private Map<SpellEffect, DelayableEntity<Entity>> entityMap;
	private Set<DelayableEntity<ArmorStand>> armorStandSet;

	private SpellData data;
	private long startTime;
	private Location startLocation;
	private Location previousLocation;
	private Location currentLocation;
	private Vector currentVelocity;
	private Vector effectOffset;
	private int counter;
	private ScheduledTask task;
	private BoundingBox hitBox;
	private BoundingBox groundHitBox;
	private Set<LivingEntity> immune;
	private int maxHitLimit;
	private ValidTargetChecker entitySpellChecker;
	private ParticleProjectileSpell spell;
	private Set<Material> groundMaterials;
	private Set<Material> disallowedGroundMaterials;
	private ValidTargetList targetList;
	private ModifierSet projectileModifiers;
	private List<Interaction> interactions;

	private boolean stopped = false;

	// projectile options
	private double maxDuration;
	private double maxDistanceSquared;

	private boolean hugSurface;
	private boolean callEvents;
	private boolean changePitch;
	private boolean controllable;
	private boolean stopOnHitGround;
	private boolean stopOnModifierFail;
	private boolean allowCasterInteract;
	private boolean ignorePassableBlocks;
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
	private float groundVerticalHitRadius;
	private float groundHorizontalHitRadius;

	private float projectileTurn;
	private float projectileVelocity;
	private double verticalRotation;
	private double horizontalRotation;
	private double xRotation;
	private float projectileVertOffset;
	private float projectileVertSpread;
	private float projectileHorizOffset;
	private float projectileHorizSpread;
	private float projectileVertGravity;
	private float projectileHorizGravity;

	private int tickInterval;
	private int spellInterval;
	private int tickSpellLimit;
	private int maxEntitiesHit;
	private double maxHeightCheck;
	private double startHeightCheck;
	private int accelerationDelay;
	private int intermediateEffects;
	private int intermediateHitboxes;
	private int specialEffectInterval;

	private FluidCollisionMode fluidCollisionMode;

	private Subspell airSpell;
	private Subspell tickSpell;
	private Subspell entitySpell;
	private Subspell casterSpell;
	private Subspell groundSpell;
	private Subspell durationSpell;
	private Subspell modifierSpell;
	private Subspell entityLocationSpell;

	private int ticks = 0;

	public ParticleProjectileTracker(SpellData data) {
		this.data = data;
	}

	public void start(Location from) {
		startTime = System.currentTimeMillis();
		startLocation = from.clone();
		if (!changePitch) startLocation.setPitch(0F);

		// Changing the start location
		Util.applyRelativeOffset(startLocation, new Vector(startXOffset, 0, startZOffset));
		startLocation.add(0, startYOffset, 0);

		previousLocation = startLocation.clone();
		currentLocation = startLocation.clone();
		currentVelocity = startLocation.getDirection();

		initialize();
	}

	public void startTarget(Location from, LivingEntity target) {
		startTarget(from, target.getLocation());
	}

	public void startTarget(Location from, Location target) {
		startTime = System.currentTimeMillis();
		startLocation = from.clone();
		if (!changePitch) startLocation.setPitch(0F);

		// Changing the target location
		Location targetLoc = target.clone();
		targetLoc.add(0, targetYOffset, 0);

		Vector direction = targetLoc.clone().subtract(startLocation).toVector();
		if (!direction.isZero()) startLocation.setDirection(direction);

		Util.applyRelativeOffset(startLocation, new Vector(startXOffset, 0, startZOffset));
		startLocation.add(0, startYOffset, 0);

		direction = targetLoc.clone().subtract(startLocation).toVector();

		previousLocation = startLocation.clone();
		currentLocation = startLocation.clone();
		currentVelocity = direction.isZero() ? startLocation.getDirection() : direction.normalize();

		initialize();
	}

	@Override
	public void initialize() {
		zoneManager = MagicSpells.getNoMagicZoneManager();
		counter = 0;

		float yaw = currentLocation.getYaw(), pitch = currentLocation.getPitch();

		if (verticalRotation != 0) {
			Vector angleZ = Util.rotateVector(new Vector(0, 0, 1), yaw, pitch);
			currentVelocity.rotateAroundAxis(angleZ, verticalRotation);
		}

		if (horizontalRotation != 0) {
			Vector angleY = Util.rotateVector(new Vector(0, -1, 0), yaw, pitch);
			currentVelocity.rotateAroundAxis(angleY, horizontalRotation);
		}

		if (xRotation != 0) {
			Vector angleX = Util.rotateVector(new Vector(1, 0, 0), yaw, pitch);
			currentVelocity.rotateAroundAxis(angleX, xRotation);
		}

		if (projectileHorizOffset != 0) Util.rotateVector(currentVelocity, projectileHorizOffset);
		if (projectileVertOffset != 0) currentVelocity.add(new Vector(0, projectileVertOffset, 0)).normalize();
		if (projectileVertSpread > 0 || projectileHorizSpread > 0) {
			float rx = -1 + rand.nextFloat() * 2;
			float ry = -1 + rand.nextFloat() * 2;
			float rz = -1 + rand.nextFloat() * 2;
			currentVelocity.add(new Vector(rx * projectileHorizSpread, ry * projectileVertSpread, rz * projectileHorizSpread));
		}

		if (!currentVelocity.isZero()) currentLocation.setDirection(currentVelocity);

		if (hugSurface) {
			currentLocation.setPitch(0);

			if (!checkGround(true)) {
				stop();
				return;
			}

			currentLocation.setY(currentLocation.getY() + heightFromSurface);
		}

		currentVelocity = currentLocation.getDirection();

		if (powerAffectsVelocity) currentVelocity.multiply(data.power());
		currentVelocity.multiply(projectileVelocity / ticksPerSecond);

		immune = new HashSet<>();

		maxHitLimit = 0;
		hitBox = BoundingBox.of(currentLocation, horizontalHitRadius, verticalHitRadius, horizontalHitRadius);
		groundHitBox = BoundingBox.of(currentLocation, groundHorizontalHitRadius, groundVerticalHitRadius, groundHorizontalHitRadius);
		data = data.retarget(null, currentLocation);

		if (spell != null) {
			effectSet = spell.playEffectsProjectile(EffectPosition.PROJECTILE, currentLocation, data);
			entityMap = spell.playEntityEffectsProjectile(EffectPosition.PROJECTILE, currentLocation, data);
			armorStandSet = spell.playArmorStandEffectsProjectile(EffectPosition.PROJECTILE, currentLocation, data);
			ParticleProjectileSpell.getProjectileTrackers().add(this);
		}

		task = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval, currentLocation);
	}

	@Override
	public void run() {
		currentVelocity = Util.makeFinite(currentVelocity);
		currentLocation = Util.makeFinite(currentLocation);
		previousLocation = Util.makeFinite(previousLocation);

		if (data.hasCaster() && !data.caster().isValid()) {
			stop();
			return;
		}

		if (zoneManager.willFizzle(currentLocation, spell)) {
			stop();
			return;
		}

		if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
			if (hitAirAfterDuration && durationSpell != null) {
				durationSpell.subcast(data);
				if (spell != null) spell.playEffects(EffectPosition.TARGET, currentLocation, data);
			}
			stop();
			return;
		}

		if (projectileModifiers != null) {
			ModifierResult result = projectileModifiers.apply(data.caster(), data);
			data = result.data();

			if (!result.check()) {
				if (modifierSpell != null) modifierSpell.subcast(data);
				if (stopOnModifierFail) stop();
				return;
			}
		}

		if (controllable && data.hasCaster()) {
			Location location = data.caster().getLocation();
			if (hugSurface) location.setPitch(0);

			Vector direction = location.getDirection();
			currentLocation.setDirection(direction);
			currentVelocity = direction.multiply(projectileVelocity / ticksPerSecond);
		}

		currentVelocity = Util.makeFinite(currentVelocity);

		// Move projectile and apply gravity
		previousLocation = currentLocation.clone();
		currentLocation.add(currentVelocity);
		data = data.location(currentLocation);

		currentLocation = Util.makeFinite(currentLocation);
		previousLocation = Util.makeFinite(previousLocation);

		if (callEvents) {
			TrackerMoveEvent trackerMoveEvent = new TrackerMoveEvent(this, previousLocation, currentLocation);
			EventUtil.call(trackerMoveEvent);
			if (stopped) {
				return;
			}
		}

		if (hugSurface) {
			if (!checkGround(false)) {
				stop();
				return;
			}

			currentLocation.setY(currentLocation.getY() + heightFromSurface);
			// Apply vertical gravity
		} else if (projectileVertGravity != 0)
			currentVelocity.setY(currentVelocity.getY() - (projectileVertGravity / ticksPerSecond));

		// Apply turn
		if (projectileTurn != 0) Util.rotateVector(currentVelocity, projectileTurn);

		// Apply horizontal gravity
		if (projectileHorizGravity != 0)
			Util.rotateVector(currentVelocity, (projectileHorizGravity / ticksPerSecond) * counter);

		// Rotate effects properly
		if (!currentVelocity.isZero()) currentLocation.setDirection(currentVelocity);

		if (effectSet != null) {
			Effect effect;
			Location effectLoc;
			for (EffectlibSpellEffect spellEffect : effectSet) {
				effect = spellEffect.getEffect();

				effectLoc = spellEffect.getSpellEffect().applyOffsets(currentLocation.clone(), data);
				effect.setLocation(effectLoc);

				if (effect instanceof ModifiedEffect mod) {
					Effect modifiedEffect = mod.getInnerEffect();
					if (modifiedEffect != null) modifiedEffect.setLocation(effectLoc);
				}
			}
		}

		if (armorStandSet != null || entityMap != null) {
			// Changing the effect location
			Location effectLoc = currentLocation.clone();
			Util.applyRelativeOffset(effectLoc, effectOffset.clone().setY(0));
			effectLoc.add(0, effectOffset.getY(), 0);

			EulerAngle angle = EulerAngle.ZERO.setX(AccurateMath.toRadians(effectLoc.getPitch()));

			if (armorStandSet != null) {
				for (DelayableEntity<ArmorStand> armorStand : armorStandSet) {
					armorStand.teleport(effectLoc);
					armorStand.accept(stand -> {
						stand.setHeadPose(angle);
						stand.setLeftArmPose(angle);
						stand.setRightArmPose(angle);
					});
				}
			}

			if (entityMap != null) {
				for (var entry : entityMap.entrySet()) {
					entry.getValue().teleport(entry.getKey().applyOffsets(effectLoc.clone(), data));
				}
			}
		}

		// Play effects
		if (spell != null && specialEffectInterval > 0 && counter % specialEffectInterval == 0)
			spell.playEffects(EffectPosition.SPECIAL, currentLocation, data);

		// Acceleration
		if (acceleration != 0 && accelerationDelay > 0 && counter % accelerationDelay == 0)
			currentVelocity.multiply(acceleration);

		// Intermediate effects
		if (intermediateEffects > 0) playIntermediateEffects();

		// Intermediate hitboxes
		if (intermediateHitboxes > 0) checkIntermediateHitboxes();

		if (stopped) return;

		counter++;

		// Cast spell mid air
		if (hitAirDuring && counter % spellInterval == 0 && tickSpell != null) {
			if (tickSpellLimit <= 0 || ticks < tickSpellLimit) {
				tickSpell.subcast(data);
				ticks++;
			}
		}

		if (currentLocation.distanceSquared(startLocation) >= maxDistanceSquared) {
			if (hitAirAtEnd && airSpell != null) {
				airSpell.subcast(data);
				if (spell != null) spell.playEffects(EffectPosition.TARGET, currentLocation, data);
			}
			stop();
			return;
		}

		checkHitbox(currentLocation);
		if (stopped) return;

		if (spell == null || interactions == null || interactions.isEmpty()) return;
		Set<ParticleProjectileTracker> toRemove = new HashSet<>();
		Set<ParticleProjectileTracker> trackers = new HashSet<>(ParticleProjectileSpell.getProjectileTrackers());
		for (ParticleProjectileTracker collisionTracker : trackers) {
			boolean isCaster = Objects.equals(data.caster(), collisionTracker.data.caster());

			for (Interaction interaction : interactions) {
				if (!canInteractWith(collisionTracker)) continue;
				if (!interaction.interactsWith().check(collisionTracker.spell)) continue;

				if (interaction.canInteractList() == null && isCaster && !allowCasterInteract) continue;
				if (interaction.canInteractList() != null && !interaction.canInteractList().canTarget(data.caster(), collisionTracker.data.caster()))
					continue;

				if (interaction.collisionSpell() != null) {
					Location middleLoc = currentLocation.clone().add(collisionTracker.currentLocation).multiply(0.5);
					interaction.collisionSpell().subcast(data.location(middleLoc));
				}

				if (interaction.stopCausing()) {
					toRemove.add(collisionTracker);
					collisionTracker.stop(false);
				}

				if (interaction.stopWith()) {
					toRemove.add(this);
					stop(false);
				}
			}
		}

		ParticleProjectileSpell.getProjectileTrackers().removeAll(toRemove);
		toRemove.clear();
		trackers.clear();
	}

	private boolean canInteractWith(ParticleProjectileTracker collisionTracker) {
		if (collisionTracker == null) return false;
		if (isStopped() || collisionTracker.isStopped()) return false;
		if (!data.hasCaster() || !collisionTracker.data.hasCaster()) return false;
		if (collisionTracker.equals(this)) return false;
		if (!collisionTracker.currentLocation.getWorld().equals(currentLocation.getWorld())) return false;
		return hitBox.overlaps(collisionTracker.hitBox);
	}

	private void playIntermediateEffects() {
		if (!(spell != null && specialEffectInterval > 0 && counter % specialEffectInterval == 0))
			return;

		int divideFactor = intermediateEffects + 1;

		Vector v = new Vector(
			(currentLocation.getX() - previousLocation.getX()) / divideFactor,
			(currentLocation.getY() - previousLocation.getY()) / divideFactor,
			(currentLocation.getZ() - previousLocation.getZ()) / divideFactor
		);
		if (v.isZero()) return;

		Location old = previousLocation.clone();
		old.setDirection(v);

		for (int i = 0; i < intermediateEffects; i++)
			spell.playEffects(EffectPosition.SPECIAL, old.add(v), data.location(old));
	}

	private void checkIntermediateHitboxes() {
		int divideFactor = intermediateHitboxes + 1;

		Vector v = new Vector(
			(currentLocation.getX() - previousLocation.getX()) / divideFactor,
			(currentLocation.getY() - previousLocation.getY()) / divideFactor,
			(currentLocation.getZ() - previousLocation.getZ()) / divideFactor
		);
		if (v.isZero()) return;

		Location old = previousLocation.clone();
		old.setDirection(v);

		for (int i = 0; i < intermediateHitboxes; i++) {
			checkHitbox(old.add(v));
			if (stopped) return;
		}
	}

	private void checkHitbox(Location currentLoc) {
		if (currentLoc == null || currentLoc.getWorld() == null) return;

		if (hitGround && groundSpell != null || stopOnHitGround) {
			groundHitBox.resize(
				currentLoc.getX() - groundHorizontalHitRadius,
				currentLoc.getY() - groundVerticalHitRadius,
				currentLoc.getZ() - groundHorizontalHitRadius,
				currentLoc.getX() + groundHorizontalHitRadius,
				currentLoc.getY() + groundVerticalHitRadius,
				currentLoc.getZ() + groundHorizontalHitRadius
			);

			if (Util.hasCollisionsIn(currentLoc.getWorld(), groundHitBox, ignorePassableBlocks, fluidCollisionMode, block -> {
				Material type = block.getType();
				return !disallowedGroundMaterials.contains(type) && (groundMaterials.isEmpty() || groundMaterials.contains(type));
			})) {
				if (hitGround && groundSpell != null) {
					groundSpell.subcast(data.location(currentLoc));
					if (spell != null) spell.playEffects(EffectPosition.TARGET, currentLoc, data);
				}

				if (stopOnHitGround) {
					stop(currentLoc);
					return;
				}
			}
		}

		hitBox.resize(
			currentLoc.getX() - horizontalHitRadius,
			currentLoc.getY() - verticalHitRadius,
			currentLoc.getZ() - horizontalHitRadius,
			currentLoc.getX() + horizontalHitRadius,
			currentLoc.getY() + verticalHitRadius,
			currentLoc.getZ() + horizontalHitRadius
		);

		for (LivingEntity target : currentLoc.getNearbyLivingEntities(horizontalHitRadius, verticalHitRadius)) {
			if (!target.isValid() || immune.contains(target) || !targetList.canTarget(data.caster(), target)) continue;

			ParticleProjectileHitEvent hitEvent = new ParticleProjectileHitEvent(data.caster(), target, this, spell, data.power());
			hitEvent.callEvent();

			if (stopped) return;
			if (hitEvent.isCancelled()) continue;

			target = hitEvent.getTarget();
			SpellData subData = data.builder().target(target).location(currentLoc).power(hitEvent.getPower()).build();

			SpellTargetEvent targetEvent = new SpellTargetEvent(spell, subData);
			if (!targetEvent.callEvent()) continue;

			subData = targetEvent.getSpellData();
			target = targetEvent.getTarget();

			if (casterSpell != null && target.equals(data.caster())) casterSpell.subcast(subData, false, false, HIT_ORDERING);
			if (entitySpell != null && !target.equals(data.caster())) entitySpell.subcast(subData, false, false, HIT_ORDERING);
			if (entityLocationSpell != null) entityLocationSpell.subcast(subData.noTarget());

			if (spell != null) spell.playEffects(EffectPosition.TARGET, currentLoc, subData);

			immune.add(target);
			maxHitLimit++;

			if (maxEntitiesHit > 0 && maxHitLimit >= maxEntitiesHit) stop(currentLoc);
			break;
		}
	}

	private boolean checkGround(boolean start) {
		double heightOffset = start ? 0 : heightFromSurface;
		double maxHeight = start ? startHeightCheck : maxHeightCheck;
		double tolerance = 1e-7;

		Predicate<Block> canCollide = block -> {
			Material type = block.getType();
			return !disallowedGroundMaterials.contains(type) && (groundMaterials.isEmpty() || groundMaterials.contains(type));
		};

		double x = currentLocation.getX(), y = currentLocation.getY(), z = currentLocation.getZ();
		BoundingBox collisionBox = new BoundingBox(
			x, y - maxHeight - tolerance - heightOffset, z,
			x, y + maxHeight + tolerance - heightOffset, z
		);

		List<BoundingBox> boxes = Util.getCollidingBoxes(currentLocation.getWorld(), collisionBox, ignorePassableBlocks, fluidCollisionMode, canCollide);

		DoubleArraySet stepHeights = new DoubleArraySet();
		for (BoundingBox b : boxes) {
			double maxY = b.getMaxY();
			if (Math.abs(y - heightOffset - maxY) - tolerance <= maxHeight)
				stepHeights.add(maxY);

			stepHeights.remove(b.getMinY());
		}

		double[] heights = stepHeights.toDoubleArray();
		double bound = start ? Math.ceil(y) : y - heightOffset;
		DoubleArrays.unstableSort(heights, (first, second) -> {
			int compare = Double.compare(first, second);
			return first <= bound && second <= bound ? -compare : compare;
		});

		for (double height : heights) {
			if (collides(x, height, z, boxes)) continue;
			currentLocation.setY(height);
			return true;
		}

		return false;
	}

	private boolean collides(double x, double y, double z, List<BoundingBox> boxes) {
		for (BoundingBox box : boxes)
			if (box.contains(x, y, z))
				return true;

		return false;
	}

	@Override
	public void stop() {
		stop(currentLocation, true);
	}

	public void stop(boolean removeTracker) {
		stop(currentLocation, removeTracker);
	}

	public void stop(Location location) {
		stop(location, true);
	}

	public void stop(Location location, boolean removeTracker) {
		if (removeTracker && spell != null) ParticleProjectileSpell.getProjectileTrackers().remove(this);
		if (spell != null) spell.playEffects(EffectPosition.DELAYED, location, data.location(location));
		MagicSpells.cancelTask(task);
		if (effectSet != null) {
			for (EffectlibSpellEffect spellEffect : effectSet) {
				spellEffect.getEffect().cancel();
			}
			effectSet.clear();
		}
		if (armorStandSet != null) {
			armorStandSet.forEach(DelayableEntity::remove);
			armorStandSet.clear();
		}
		if (entityMap != null) {
			entityMap.values().forEach(DelayableEntity::remove);
			entityMap.clear();
		}
		startLocation = null;
		previousLocation = null;
		currentLocation = null;
		currentVelocity = null;
		if (immune != null) {
			immune.clear();
			immune = null;
		}
		stopped = true;
	}

	public boolean isStopped() {
		return stopped;
	}

	public LivingEntity getCaster() {
		return data.caster();
	}

	public void setCaster(LivingEntity caster) {
		data = data.caster(caster);
	}

	public float getPower() {
		return data.power();
	}

	public void setPower(float power) {
		data = data.power(power);
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

	public ScheduledTask getTask() {
		return task;
	}

	public void setTask(ScheduledTask task) {
		this.task = task;
	}

	public BoundingBox getHitBox() {
		return hitBox;
	}

	public void setHitBox(BoundingBox hitBox) {
		this.hitBox = hitBox;
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

	public ValidTargetChecker getEntitySpellChecker() {
		return entitySpellChecker;
	}

	public void setEntitySpellChecker(ValidTargetChecker entitySpellChecker) {
		this.entitySpellChecker = entitySpellChecker;
	}

	public ParticleProjectileSpell getSpell() {
		return spell;
	}

	public void setSpell(ParticleProjectileSpell spell) {
		this.spell = spell;
	}

	public Set<Material> getGroundMaterials() {
		return groundMaterials;
	}

	public void setGroundMaterials(Set<Material> groundMaterials) {
		this.groundMaterials = groundMaterials;
	}

	public Set<Material> getDisallowedGroundMaterials() {
		return disallowedGroundMaterials;
	}

	public void setDisallowedGroundMaterials(Set<Material> disallowedGroundMaterials) {
		this.disallowedGroundMaterials = disallowedGroundMaterials;
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

	public List<Interaction> getInteractions() {
		return interactions;
	}

	public void setInteractions(List<Interaction> interactions) {
		this.interactions = interactions;
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

	public boolean shouldHugSurface() {
		return hugSurface;
	}

	public void setHugSurface(boolean hugSurface) {
		this.hugSurface = hugSurface;
	}

	public boolean shouldChangePitch() {
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

	public boolean shouldCallEvents() {
		return callEvents;
	}

	public void setCallEvents(boolean callEvents) {
		this.callEvents = callEvents;
	}

	public boolean shouldStopOnHitGround() {
		return stopOnHitGround;
	}

	public void setStopOnHitGround(boolean stopOnHitGround) {
		this.stopOnHitGround = stopOnHitGround;
	}

	public boolean shouldStopOnModifierFail() {
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

	public void setVerticalRotation(double verticalRotation) {
		this.verticalRotation = verticalRotation;
	}

	public double getVerticalRotation() {
		return verticalRotation;
	}

	public void setHorizontalRotation(double horizontalRotation) {
		this.horizontalRotation = horizontalRotation;
	}

	public double getHorizontalRotation() {
		return horizontalRotation;
	}

	public void setXRotation(double xRotation) {
		this.xRotation = xRotation;
	}

	public double getXRotation() {
		return xRotation;
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

	public int getSpellInterval() {
		return spellInterval;
	}

	public void setSpellInterval(int spellInterval) {
		this.spellInterval = spellInterval;
	}

	public int getTickSpellLimit() {
		return tickSpellLimit;
	}

	public void setTickSpellLimit(int tickSpellLimit) {
		this.tickSpellLimit = tickSpellLimit;
	}

	public int getMaxEntitiesHit() {
		return maxEntitiesHit;
	}

	public void setMaxEntitiesHit(int maxEntitiesHit) {
		this.maxEntitiesHit = maxEntitiesHit;
	}

	public double getMaxHeightCheck() {
		return maxHeightCheck;
	}

	public void setMaxHeightCheck(double maxHeightCheck) {
		this.maxHeightCheck = maxHeightCheck;
	}

	public double getStartHeightCheck() {
		return startHeightCheck;
	}

	public void setStartHeightCheck(double startHeightCheck) {
		this.startHeightCheck = startHeightCheck;
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

	public float getGroundVerticalHitRadius() {
		return groundVerticalHitRadius;
	}

	public void setGroundVerticalHitRadius(float groundVerticalHitRadius) {
		this.groundVerticalHitRadius = groundVerticalHitRadius;
	}

	public float getGroundHorizontalHitRadius() {
		return groundHorizontalHitRadius;
	}

	public void setGroundHorizontalHitRadius(float groundHorizontalHitRadius) {
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

	public Vector getEffectOffset() {
		return effectOffset;
	}

	public void setEffectOffset(Vector effectOffset) {
		this.effectOffset = effectOffset;
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

	public Subspell getAirSpell() {
		return airSpell;
	}

	public void setAirSpell(Subspell airSpell) {
		this.airSpell = airSpell;
	}

	public Subspell getTickSpell() {
		return tickSpell;
	}

	public void setTickSpell(Subspell tickSpell) {
		this.tickSpell = tickSpell;
	}

	public Subspell getEntitySpell() {
		return entitySpell;
	}

	public void setEntitySpell(Subspell entitySpell) {
		this.entitySpell = entitySpell;
	}

	public Subspell getCasterSpell() {
		return casterSpell;
	}

	public void setCasterSpell(Subspell casterSpell) {
		this.casterSpell = casterSpell;
	}

	public Subspell getGroundSpell() {
		return groundSpell;
	}

	public void setGroundSpell(Subspell groundSpell) {
		this.groundSpell = groundSpell;
	}

	public Subspell getDurationSpell() {
		return durationSpell;
	}

	public void setDurationSpell(Subspell durationSpell) {
		this.durationSpell = durationSpell;
	}

	public Subspell getModifierSpell() {
		return modifierSpell;
	}

	public void setModifierSpell(Subspell modifierSpell) {
		this.modifierSpell = modifierSpell;
	}

	public Subspell getEntityLocationSpell() {
		return entityLocationSpell;
	}

	public void setEntityLocationSpell(Subspell entityLocationSpell) {
		this.entityLocationSpell = entityLocationSpell;
	}

	public String[] getArgs() {
		return data.args();
	}

	public SpellData getSpellData() {
		return data;
	}

	public boolean isIgnorePassableBlocks() {
		return ignorePassableBlocks;
	}

	public void setIgnorePassableBlocks(boolean ignorePassableBlocks) {
		this.ignorePassableBlocks = ignorePassableBlocks;
	}

	public FluidCollisionMode getFluidCollisionMode() {
		return fluidCollisionMode;
	}

	public void setFluidCollisionMode(FluidCollisionMode fluidCollisionMode) {
		this.fluidCollisionMode = fluidCollisionMode;
	}

}
