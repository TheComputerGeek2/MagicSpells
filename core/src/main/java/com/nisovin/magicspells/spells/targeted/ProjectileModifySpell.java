package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.util.NumberConversions;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.instant.ParticleProjectileSpell;
import com.nisovin.magicspells.util.trackers.ParticleProjectileTracker;

public class ProjectileModifySpell extends TargetedSpell implements TargetedLocationSpell {

	private Subspell projectileSpell;
	private final String projectileSpellName;

	private final ConfigData<Boolean> stop;
	private final ConfigData<Boolean> circleShape;
	private final ConfigData<Boolean> affectOwnedProjectiles;
	private final ConfigData<Boolean> affectEnemyProjectiles;

	private final ConfigData<Integer> cone;
	private final ConfigData<Integer> vRadius;
	private final ConfigData<Integer> hRadius;
	private final ConfigData<Integer> maxTargets;

	private final ConfigData<Boolean> pointBlank;
	private final ConfigData<Boolean> claimProjectiles;

	private SpellFilter filter;

	private final ConfigData<Float> velocity;

	private final ConfigData<Float> acceleration;
	private final ConfigData<Integer> accelerationDelay;

	private final ConfigData<Float> projectileTurn;
	private final ConfigData<Float> projectileVertGravity;
	private final ConfigData<Float> projectileHorizGravity;

	private final ConfigData<Integer> tickInterval;
	private final ConfigData<Integer> spellInterval;
	private final ConfigData<Integer> tickSpellLimit;
	private final ConfigData<Integer> maxEntitiesHit;
	private final ConfigData<Integer> intermediateEffects;
	private final ConfigData<Integer> intermediateHitboxes;
	private final ConfigData<Integer> specialEffectInterval;

	private final ConfigData<Float> hitRadius;
	private final ConfigData<Float> verticalHitRadius;
	private final ConfigData<Integer> groundHitRadius;
	private final ConfigData<Integer> groundVerticalHitRadius;

	private final ConfigData<Double> maxDuration;
	private final ConfigData<Double> maxDistance;

	private final ConfigData<Boolean> hugSurface;
	private final ConfigData<Float> heightFromSurface;

	private final ConfigData<Boolean> controllable;
	private final ConfigData<Boolean> hitGround;
	private final ConfigData<Boolean> hitAirAtEnd;
	private final ConfigData<Boolean> hitAirDuring;
	private final ConfigData<Boolean> hitAirAfterDuration;
	private final ConfigData<Boolean> stopOnHitGround;
	private final ConfigData<Boolean> stopOnModifierFail;

	private Subspell airSpell;
	private Subspell selfSpell;
	private Subspell tickSpell;
	private Subspell entitySpell;
	private Subspell groundSpell;
	private Subspell durationSpell;
	private Subspell modifierSpell;
	private Subspell entityLocationSpell;
	private final String airSpellName;
	private final String selfSpellName;
	private final String tickSpellName;
	private final String entitySpellName;
	private final String groundSpellName;
	private final String durationSpellName;
	private final String modifierSpellName;
	private final String entityLocationSpellName;

	private ModifierSet projModifiers;
	private List<String> projModifiersStrings;

	public ProjectileModifySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		stop = getConfigDataBoolean("stop", false);
		circleShape = getConfigDataBoolean("circle-shape", false);
		affectOwnedProjectiles = getConfigDataBoolean("affect-owned-projectiles", true);
		affectEnemyProjectiles = getConfigDataBoolean("affect-enemy-projectiles", true);

		projectileSpellName = getConfigString("spell", "");

		cone = getConfigDataInt("cone", 0);
		vRadius = getConfigDataInt("vertical-radius", 5);
		hRadius = getConfigDataInt("horizontal-radius", 10);
		maxTargets = getConfigDataInt("max-targets", 0);

		pointBlank = getConfigDataBoolean("point-blank", true);
		claimProjectiles = getConfigDataBoolean("claim-projectiles", false);

		velocity = getConfigDataFloat("projectile-velocity", 1F);
		acceleration = getConfigDataFloat("projectile-acceleration", 0F);
		accelerationDelay = getConfigDataInt("projectile-acceleration-delay", 0);

		projectileTurn = getConfigDataFloat("projectile-turn", 0);
		projectileVertGravity = getConfigDataFloat("projectile-vert-gravity", 0F);
		projectileHorizGravity = getConfigDataFloat("projectile-horiz-gravity", 0F);

		tickInterval = getConfigDataInt("tick-interval", 2);
		spellInterval = getConfigDataInt("spell-interval", 20);
		intermediateEffects = getConfigDataInt("intermediate-effects", 0);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 1);

		maxDuration = getConfigDataDouble("max-duration", 0);
		maxDistance = getConfigDataDouble("max-distance", 15);

		intermediateHitboxes = getConfigDataInt("intermediate-hitboxes", 0);
		tickSpellLimit = getConfigDataInt("tick-spell-limit", 0);
		maxEntitiesHit = getConfigDataInt("max-entities-hit", 0);
		hitRadius = getConfigDataFloat("hit-radius", 1.5F);
		verticalHitRadius = getConfigDataFloat("vertical-hit-radius", hitRadius);
		groundHitRadius = getConfigDataInt("ground-hit-radius", 1);
		groundVerticalHitRadius = getConfigDataInt("ground-vertical-hit-radius", groundHitRadius);

		hugSurface = getConfigDataBoolean("hug-surface", false);
		heightFromSurface = getConfigDataFloat("height-from-surface", 0.6F);

		controllable = getConfigDataBoolean("controllable", false);
		hitGround = getConfigDataBoolean("hit-ground", true);
		hitAirAtEnd = getConfigDataBoolean("hit-air-at-end", false);
		hitAirDuring = getConfigDataBoolean("hit-air-during", false);
		hitAirAfterDuration = getConfigDataBoolean("hit-air-after-duration", false);
		stopOnHitGround = getConfigDataBoolean("stop-on-hit-ground", true);
		stopOnModifierFail = getConfigDataBoolean("stop-on-modifier-fail", true);

		airSpellName = getConfigString("spell-on-hit-air", "");
		selfSpellName = getConfigString("spell-on-hit-self", "");
		tickSpellName = getConfigString("spell-on-tick", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		entitySpellName = getConfigString("spell-on-hit-entity", "");
		durationSpellName = getConfigString("spell-on-duration-end", "");
		modifierSpellName = getConfigString("spell-on-modifier-fail", "");
		entityLocationSpellName = getConfigString("spell-on-entity-location", "");

		projModifiersStrings = getConfigStringList("projectile-modifiers", null);
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		if (projModifiersStrings != null && !projModifiersStrings.isEmpty()) {
			projModifiers = new ModifierSet(projModifiersStrings, this);
			projModifiersStrings = null;
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "ProjectileModifySpell '" + internalName + "' has an invalid '%s' defined!";
		projectileSpell = initSubspell(projectileSpellName,
				error.formatted("spell"),
				true);
		airSpell = initSubspell(airSpellName,
				error.formatted("spell-on-hit-air"),
				true);
		selfSpell = initSubspell(selfSpellName,
				error.formatted("spell-on-hit-self"),
				true);
		tickSpell = initSubspell(tickSpellName,
				error.formatted("spell-on-tick"),
				true);
		groundSpell = initSubspell(groundSpellName,
				error.formatted("spell-on-hit-ground"),
				true);
		entitySpell = initSubspell(entitySpellName,
				error.formatted("spell-on-hit-entity"),
				true);
		durationSpell = initSubspell(durationSpellName,
				error.formatted("spell-on-duration-end"),
				true);
		modifierSpell = initSubspell(modifierSpellName,
				error.formatted("spell-on-modifier-fail"),
				true);
		entityLocationSpell = initSubspell(entityLocationSpellName,
				error.formatted("spell-on-entity-location"),
				true);

		filter = getConfigSpellFilter();
	}

	@Override
	public CastResult cast(SpellData data) {
		if (pointBlank.get(data)) {
			SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, data.caster().getLocation());
			if (!targetEvent.callEvent()) return noTarget(targetEvent);
			data = targetEvent.getSpellData();
		} else {
			TargetInfo<Location> info = getTargetedBlockLocation(data, false);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();
		}

		return castAtLocation(data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		int count = 0;

		Location location = data.location();

		Location fromLoc = data.hasCaster() ? data.caster().getLocation() : location;
		Vector facing = fromLoc.getDirection();
		Vector vLoc = fromLoc.toVector();

		BoundingBox box = new BoundingBox(location, hRadius.get(data), vRadius.get(data));
		double hRadiusSquared = box.getHorizontalRadius() * box.getHorizontalRadius();
		double vRadiusSquared = box.getVerticalRadius() * box.getVerticalRadius();

		Iterator<ParticleProjectileTracker> iterator = new HashSet<>(ParticleProjectileSpell.getProjectileTrackers()).iterator();

		int maxTargets = this.maxTargets.get(data);
		int cone = this.cone.get(data);

		double maxDistanceSquared = this.maxDistance.get(data);
		maxDistanceSquared *= maxDistanceSquared;

		boolean stop = this.stop.get(data);
		boolean hugSurface = this.hugSurface.get(data);
		boolean circleShape = this.circleShape.get(data);
		boolean claimProjectiles = this.claimProjectiles.get(data);
		boolean affectOwnedProjectiles = this.affectOwnedProjectiles.get(data);
		boolean affectEnemyProjectiles = this.affectEnemyProjectiles.get(data);

		Location currentLoc;
		while (iterator.hasNext()) {
			ParticleProjectileTracker tracker = iterator.next();
			if (tracker == null || tracker.isStopped()) continue;
			currentLoc = tracker.getCurrentLocation();
			if (currentLoc == null) continue;
			if (!currentLoc.getWorld().equals(location.getWorld())) continue;
			if (!box.contains(currentLoc)) continue;
			if (tracker.getSpell() != null && !filter.check(tracker.getSpell())) continue;

			if (!affectOwnedProjectiles && tracker.getCaster() != null && tracker.getCaster().equals(data.caster())) continue;
			if (!affectEnemyProjectiles && (tracker.getCaster() == null || !tracker.getCaster().equals(data.caster()))) continue;

			if (circleShape) {
				double hDistance = NumberConversions.square(currentLoc.getX() - location.getX()) + NumberConversions.square(currentLoc.getZ() - location.getZ());
				if (hDistance > hRadiusSquared) continue;
				double vDistance = NumberConversions.square(currentLoc.getY() - location.getY());
				if (vDistance > vRadiusSquared) continue;
			}

			if (cone > 0) {
				Vector dir = currentLoc.toVector().subtract(vLoc);
				if (AccurateMath.abs(dir.angle(facing)) > cone) continue;
			}

			SpellData subData = data.location(currentLoc);

			if (projectileSpell != null) projectileSpell.subcast(subData);

			if (stop) {
				playSpellEffects(EffectPosition.TARGET, currentLoc, subData);
				playSpellEffectsTrail(location, currentLoc, subData);
				if (subData.hasCaster()) playSpellEffectsTrail(subData.caster().getLocation(), currentLoc, subData);

				count++;

				tracker.stop(false);
				iterator.remove();
				if (maxTargets > 0 && count >= maxTargets) break;
				continue;
			}

			if (claimProjectiles) tracker.setCaster(subData.caster());

			tracker.setAcceleration(acceleration.get(subData));
			tracker.setAccelerationDelay(accelerationDelay.get(subData));

			tracker.setProjectileTurn(projectileTurn.get(subData));
			tracker.setProjectileVertGravity(projectileVertGravity.get(subData));
			tracker.setProjectileHorizGravity(projectileHorizGravity.get(subData));
			tracker.setTickInterval(tickInterval.get(subData));
			tracker.setSpellInterval(spellInterval.get(subData));
			tracker.setIntermediateEffects(intermediateEffects.get(subData));
			tracker.setIntermediateHitboxes(intermediateHitboxes.get(subData));
			tracker.setSpecialEffectInterval(specialEffectInterval.get(subData));
			tracker.setMaxDistanceSquared(maxDistanceSquared);
			tracker.setMaxDuration(maxDuration.get(subData) * TimeUtil.MILLISECONDS_PER_SECOND);
			tracker.setMaxEntitiesHit(maxEntitiesHit.get(subData));
			tracker.setHorizontalHitRadius(hitRadius.get(subData));
			tracker.setVerticalHitRadius(verticalHitRadius.get(subData));
			tracker.setGroundHorizontalHitRadius(groundHitRadius.get(subData));
			tracker.setGroundVerticalHitRadius(groundVerticalHitRadius.get(subData));
			tracker.setHugSurface(hugSurface);
			tracker.setHeightFromSurface(hugSurface ? heightFromSurface.get(subData) : 0);
			tracker.setControllable(controllable.get(subData));
			tracker.setHitGround(hitGround.get(subData));
			tracker.setHitAirAtEnd(hitAirAtEnd.get(subData));
			tracker.setHitAirDuring(hitAirDuring.get(subData));
			tracker.setHitAirAfterDuration(hitAirAfterDuration.get(subData));
			tracker.setStopOnHitGround(stopOnHitGround.get(subData));
			tracker.setStopOnModifierFail(stopOnModifierFail.get(subData));
			tracker.setProjectileModifiers(projModifiers);
			tracker.setTickSpellLimit(tickSpellLimit.get(subData));
			if (airSpell != null) tracker.setAirSpell(airSpell);
			if (tickSpell != null) tracker.setTickSpell(tickSpell);
			if (selfSpell != null) tracker.setCasterSpell(selfSpell);
			if (groundSpell != null) tracker.setGroundSpell(groundSpell);
			if (entitySpell != null) tracker.setEntitySpell(entitySpell);
			if (durationSpell != null) tracker.setDurationSpell(durationSpell);
			if (modifierSpell != null) tracker.setModifierSpell(modifierSpell);
			if (entityLocationSpell != null) tracker.setEntityLocationSpell(entityLocationSpell);

			tracker.getCurrentVelocity().multiply(velocity.get(subData));

			playSpellEffects(EffectPosition.TARGET, currentLoc, subData);
			playSpellEffectsTrail(location, currentLoc, subData);
			if (subData.hasCaster()) playSpellEffectsTrail(subData.caster().getLocation(), currentLoc, subData);

			count++;

			if (maxTargets > 0 && count >= maxTargets) break;
		}

		if (data.hasCaster()) playSpellEffects(EffectPosition.CASTER, data.caster(), data);
		playSpellEffects(EffectPosition.SPECIAL, location, data);

		return new CastResult(count > 0 ? PostCastAction.HANDLE_NORMALLY : PostCastAction.ALREADY_HANDLED, data);
	}

}
