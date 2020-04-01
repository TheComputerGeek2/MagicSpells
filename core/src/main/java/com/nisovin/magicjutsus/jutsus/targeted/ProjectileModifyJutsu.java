package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Set;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.BoundingBox;
import com.nisovin.magicjutsus.util.JutsuFilter;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.ProjectileTracker;
import com.nisovin.magicjutsus.castmodifiers.ModifierSet;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.jutsus.instant.ParticleProjectileJutsu;

public class ProjectileModifyJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private int cone;
	private int vRadius;
	private int hRadius;
	private int maxTargets;

	private boolean pointBlank;
	private boolean claimProjectiles;

	private JutsuFilter filter;

	private float velocity;

	private float acceleration;
	private int accelerationDelay;

	private float projectileTurn;
	private float projectileVertGravity;
	private float projectileHorizGravity;

	private int tickInterval;
	private int jutsuInterval;
	private int tickJutsuLimit;
	private int maxEntitiesHit;
	private int intermediateEffects;
	private int intermediateHitboxes;
	private int specialEffectInterval;

	private float hitRadius;
	private float verticalHitRadius;
	private int groundHitRadius;
	private int groundVerticalHitRadius;

	private double maxDuration;
	private double maxDistanceSquared;

	private boolean hugSurface;
	private float heightFromSurface;

	private boolean controllable;
	private boolean hitGround;
	private boolean hitAirAtEnd;
	private boolean hitAirDuring;
	private boolean hitAirAfterDuration;
	private boolean stopOnHitGround;
	private boolean stopOnModifierFail;

	private Ninjutsu airJutsu;
	private Ninjutsu selfJutsu;
	private Ninjutsu tickJutsu;
	private Ninjutsu entityJutsu;
	private Ninjutsu groundJutsu;
	private Ninjutsu durationJutsu;
	private Ninjutsu modifierJutsu;
	private Ninjutsu entityLocationJutsu;
	private String airJutsuName;
	private String selfJutsuName;
	private String tickJutsuName;
	private String entityJutsuName;
	private String groundJutsuName;
	private String durationJutsuName;
	private String modifierJutsuName;
	private String entityLocationJutsuName;

	private ModifierSet projModifiers;
	private List<String> projModifiersStrings;

	public ProjectileModifyJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		cone = getConfigInt("cone", 0);
		vRadius = getConfigInt("vertical-radius", 5);
		hRadius = getConfigInt("horizontal-radius", 10);
		maxTargets = getConfigInt("max-targets", 0);

		pointBlank = getConfigBoolean("point-blank", true);
		claimProjectiles = getConfigBoolean("claim-projectiles", false);

		List<String> jutsus = getConfigStringList("jutsus", null);
		List<String> deniedJutsus = getConfigStringList("denied-jutsus", null);
		List<String> jutsuTags = getConfigStringList("jutsu-tags", null);
		List<String> deniedJutsuTags = getConfigStringList("denied-jutsu-tags", null);

		filter = new JutsuFilter(jutsus, deniedJutsus, jutsuTags, deniedJutsuTags);

		velocity = getConfigFloat("projectile-velocity", 1F);
		acceleration = getConfigFloat("projectile-acceleration", 0F);
		accelerationDelay = getConfigInt("projectile-acceleration-delay", 0);

		projectileTurn = getConfigFloat("projectile-turn", 0);
		projectileVertGravity = getConfigFloat("projectile-vert-gravity", 0F);
		projectileHorizGravity = getConfigFloat("projectile-horiz-gravity", 0F);

		tickInterval = getConfigInt("tick-interval", 2);
		jutsuInterval = getConfigInt("jutsu-interval", 20);
		intermediateEffects = getConfigInt("intermediate-effects", 0);
		specialEffectInterval = getConfigInt("special-effect-interval", 1);

		maxDistanceSquared = getConfigDouble("max-distance", 15);
		maxDistanceSquared *= maxDistanceSquared;
		maxDuration = getConfigDouble("max-duration", 0) * TimeUtil.MILLISECONDS_PER_SECOND;

		intermediateHitboxes = getConfigInt("intermediate-hitboxes", 0);
		tickJutsuLimit = getConfigInt("tick-jutsu-limit", 0);
		maxEntitiesHit = getConfigInt("max-entities-hit", 0);
		hitRadius = getConfigFloat("hit-radius", 1.5F);
		verticalHitRadius = getConfigFloat("vertical-hit-radius", hitRadius);
		groundHitRadius = getConfigInt("ground-hit-radius", 1);
		groundVerticalHitRadius = getConfigInt("ground-vertical-hit-radius", groundHitRadius);

		hugSurface = getConfigBoolean("hug-surface", false);
		if (hugSurface) heightFromSurface = getConfigFloat("height-from-surface", 0.6F);

		controllable = getConfigBoolean("controllable", false);
		hitGround = getConfigBoolean("hit-ground", true);
		hitAirAtEnd = getConfigBoolean("hit-air-at-end", false);
		hitAirDuring = getConfigBoolean("hit-air-during", false);
		hitAirAfterDuration = getConfigBoolean("hit-air-after-duration", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", true);
		stopOnModifierFail = getConfigBoolean("stop-on-modifier-fail", true);

		airJutsuName = getConfigString("jutsu-on-hit-air", "");
		selfJutsuName = getConfigString("jutsu-on-hit-self", "");
		tickJutsuName = getConfigString("jutsu-on-tick", "");
		groundJutsuName = getConfigString("jutsu-on-hit-ground", "");
		entityJutsuName = getConfigString("jutsu-on-hit-entity", "");
		durationJutsuName = getConfigString("jutsu-on-duration-end", "");
		modifierJutsuName = getConfigString("jutsu-on-modifier-fail", "");
		entityLocationJutsuName = getConfigString("jutsu-on-entity-location", "");

		projModifiersStrings = getConfigStringList("projectile-modifiers", null);
	}

	@Override
	public void initialize() {
		super.initialize();

		airJutsu = new Ninjutsu(airJutsuName);
		if (!airJutsu.process() || !airJutsu.isTargetedLocationJutsu()) {
			if (!airJutsuName.isEmpty()) MagicJutsus.error("ProjectileModifyJutsu '" + internalName + "' has an invalid jutsu-on-hit-air defined!");
			airJutsu = null;
		}

		selfJutsu = new Ninjutsu(selfJutsuName);
		if (!selfJutsu.process()) {
			if (!selfJutsuName.isEmpty()) MagicJutsus.error("ProjectileModifyJutsu '" + internalName + "' has an invalid jutsu-on-hit-self defined!");
			selfJutsu = null;
		}

		tickJutsu = new Ninjutsu(tickJutsuName);
		if (!tickJutsu.process() || !tickJutsu.isTargetedLocationJutsu()) {
			if (!tickJutsuName.isEmpty()) MagicJutsus.error("ProjectileModifyJutsu '" + internalName + "' has an invalid jutsu-on-tick defined!");
			tickJutsu = null;
		}

		groundJutsu = new Ninjutsu(groundJutsuName);
		if (!groundJutsu.process() || !groundJutsu.isTargetedLocationJutsu()) {
			if (!groundJutsuName.isEmpty()) MagicJutsus.error("ProjectileModifyJutsu '" + internalName + "' has an invalid jutsu-on-hit-ground defined!");
			groundJutsu = null;
		}

		entityJutsu = new Ninjutsu(entityJutsuName);
		if (!entityJutsu.process()) {
			if (!entityJutsuName.isEmpty()) MagicJutsus.error("ProjectileModifyJutsu '" + internalName + "' has an invalid jutsu-on-hit-entity defined!");
			entityJutsu = null;
		}

		durationJutsu = new Ninjutsu(durationJutsuName);
		if (!durationJutsu.process()) {
			if (!durationJutsuName.isEmpty()) MagicJutsus.error("ProjectileModifyJutsu '" + internalName + "' has an invalid jutsu-on-duration-end defined!");
			durationJutsu = null;
		}

		modifierJutsu = new Ninjutsu(modifierJutsuName);
		if (!modifierJutsu.process()) {
			if (!modifierJutsuName.isEmpty()) MagicJutsus.error("ProjectileModifyJutsu '" + internalName + "' has an invalid jutsu-on-modifier-fail defined!");
			modifierJutsu = null;
		}

		entityLocationJutsu = new Ninjutsu(entityLocationJutsuName);
		if (!entityLocationJutsu.process() || !entityLocationJutsu.isTargetedLocationJutsu()) {
			if (!entityLocationJutsuName.isEmpty()) MagicJutsus.error("ProjectileModifyJutsu '" + internalName + "' has an invalid jutsu-on-entity-location defined!");
			entityLocationJutsu = null;
		}

		if (projModifiersStrings != null && !projModifiersStrings.isEmpty()) {
			projModifiers = new ModifierSet(projModifiersStrings);
			projModifiersStrings = null;
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Location loc = null;
			if (pointBlank) loc = livingEntity.getLocation();
			else {
				try {
					Block block = getTargetedBlock(livingEntity, power);
					if (block != null && !BlockUtils.isAir(block.getType())) loc = block.getLocation();
				} catch (IllegalStateException e) {
					loc = null;
				}
			}
			if (loc == null) return noTarget(livingEntity);

			modify(livingEntity, loc);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return modify(caster, target);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return modify(null, target);
	}

	private boolean modify(LivingEntity livingEntity, Location location) {
		int count = 0;

		Vector facing = livingEntity != null ? livingEntity.getLocation().getDirection() : location.getDirection();
		Vector vLoc = livingEntity != null ? livingEntity.getLocation().toVector() : location.toVector();

		BoundingBox box = new BoundingBox(location, hRadius, vRadius);

		Set<ProjectileTracker> trackerSet = ParticleProjectileJutsu.getProjectileTrackers();

		for (ProjectileTracker tracker : trackerSet) {
			if (tracker == null) continue;
			if (!tracker.getCurrentLocation().getWorld().equals(location.getWorld())) continue;
			if (!box.contains(tracker.getCurrentLocation())) continue;
			if (tracker.getJutsu() != null && !filter.check(tracker.getJutsu())) continue;

			if (pointBlank && cone > 0) {
				Vector dir = tracker.getCurrentLocation().toVector().subtract(vLoc);
				if (Math.abs(dir.angle(facing)) > cone) continue;
			}

			if (claimProjectiles) tracker.setCaster(livingEntity);

			tracker.setAcceleration(acceleration);
			tracker.setAccelerationDelay(accelerationDelay);

			tracker.setProjectileTurn(projectileTurn);
			tracker.setProjectileVertGravity(projectileVertGravity);
			tracker.setProjectileHorizGravity(projectileHorizGravity);
			tracker.setTickInterval(tickInterval);
			tracker.setJutsuInterval(jutsuInterval);
			tracker.setIntermediateEffects(intermediateEffects);
			tracker.setIntermediateHitboxes(intermediateHitboxes);
			tracker.setSpecialEffectInterval(specialEffectInterval);
			tracker.setMaxDistanceSquared(maxDistanceSquared);
			tracker.setMaxDuration(maxDuration);
			tracker.setMaxEntitiesHit(maxEntitiesHit);
			tracker.setHorizontalHitRadius(hitRadius);
			tracker.setVerticalHitRadius(verticalHitRadius);
			tracker.setGroundHorizontalHitRadius(groundHitRadius);
			tracker.setGroundVerticalHitRadius(groundVerticalHitRadius);
			tracker.setHugSurface(hugSurface);
			tracker.setHeightFromSurface(heightFromSurface);
			tracker.setControllable(controllable);
			tracker.setHitGround(hitGround);
			tracker.setHitAirAtEnd(hitAirAtEnd);
			tracker.setHitAirDuring(hitAirDuring);
			tracker.setHitAirAfterDuration(hitAirAfterDuration);
			tracker.setStopOnHitGround(stopOnHitGround);
			tracker.setStopOnModifierFail(stopOnModifierFail);
			tracker.setProjectileModifiers(projModifiers);
			tracker.setTickJutsuLimit(tickJutsuLimit);
			if (airJutsu != null) tracker.setAirJutsu(airJutsu);
			if (tickJutsu != null) tracker.setTickJutsu(tickJutsu);
			if (selfJutsu != null) tracker.setCasterJutsu(selfJutsu);
			if (groundJutsu != null) tracker.setGroundJutsu(groundJutsu);
			if (entityJutsu != null) tracker.setEntityJutsu(entityJutsu);
			if (durationJutsu != null) tracker.setDurationJutsu(durationJutsu);
			if (modifierJutsu != null) tracker.setModifierJutsu(modifierJutsu);
			if (entityLocationJutsu != null) tracker.setEntityLocationJutsu(entityLocationJutsu);

			tracker.getCurrentVelocity().multiply(velocity);

			playJutsuEffects(EffectPosition.TARGET, tracker.getCurrentLocation());
			playJutsuEffectsTrail(location, tracker.getCurrentLocation());
			if (livingEntity != null) playJutsuEffectsTrail(livingEntity.getLocation(), tracker.getCurrentLocation());

			count++;

			if (maxTargets > 0 && count >= maxTargets) break;

		}

		if (livingEntity != null) playJutsuEffects(EffectPosition.CASTER, livingEntity);
		playJutsuEffects(EffectPosition.SPECIAL, location);

		return count > 0;
	}

}
