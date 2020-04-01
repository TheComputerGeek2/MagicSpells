package com.nisovin.magicjutsus.jutsus.instant;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.util.ValidTargetList;
import com.nisovin.magicjutsus.util.ProjectileTracker;
import com.nisovin.magicjutsus.castmodifiers.ModifierSet;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityFromLocationJutsu;

public class ParticleProjectileJutsu extends InstantJutsu implements TargetedLocationJutsu, TargetedEntityJutsu, TargetedEntityFromLocationJutsu {

	private static Set<ProjectileTracker> trackerSet;

	private float targetYOffset;
	private float startXOffset;
	private float startYOffset;
	private float startZOffset;
	private Vector relativeOffset;

	private float acceleration;
	private int accelerationDelay;
	private float projectileTurn;
	private float projectileVelocity;
	private float projectileVertOffset;
	private float projectileHorizOffset;
	private float projectileVertSpread;
	private float projectileHorizSpread;
	private float projectileVertGravity;
	private float projectileHorizGravity;

	private int tickInterval;
	private float ticksPerSecond;
	private int jutsuInterval;
	private int intermediateEffects;
	private int specialEffectInterval;

	private int tickJutsuLimit;
	private int intermediateHitboxes;
	private int maxEntitiesHit;
	private float hitRadius;
	private float verticalHitRadius;
	private int groundHitRadius;
	private int groundVerticalHitRadius;
	private Set<Material> groundMaterials;

	private double maxDuration;
	private double maxDistanceSquared;

	private boolean hugSurface;
	private float heightFromSurface;

	private boolean controllable;
	private boolean changePitch;
	private boolean hitSelf;
	private boolean hitGround;
	private boolean hitPlayers;
	private boolean hitAirAtEnd;
	private boolean hitAirDuring;
	private boolean hitNonPlayers;
	private boolean hitAirAfterDuration;
	private boolean stopOnHitEntity;
	private boolean stopOnHitGround;
	private boolean stopOnModifierFail;
	private boolean allowCasterInteract;
	private boolean powerAffectsVelocity;

	private ModifierSet projModifiers;
	private List<String> projModifiersStrings;
	private List<String> interactions;
	private Map<String, Ninjutsu> interactionJutsus;

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

	private Ninjutsu defaultJutsu;
	private String defaultJutsuName;

	public ParticleProjectileJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		trackerSet = new HashSet<>();

		// Compatibility with start-forward-offset
		float startForwardOffset = getConfigFloat("start-forward-offset", 1F);
		startXOffset = getConfigFloat("start-x-offset", 1F);
		if (startForwardOffset != 1F) startXOffset = startForwardOffset;
		startYOffset = getConfigFloat("start-y-offset", 1F);
		startZOffset = getConfigFloat("start-z-offset", 0F);
		targetYOffset = getConfigFloat("target-y-offset", 0F);

		// If relative-offset contains different values than the offsets above, override them
		relativeOffset = getConfigVector("relative-offset", "1,1,0");
		if (relativeOffset.getX() != 1F) startXOffset = (float) relativeOffset.getX();
		if (relativeOffset.getY() != 1F) startYOffset = (float) relativeOffset.getY();
		if (relativeOffset.getZ() != 0F) startZOffset = (float) relativeOffset.getZ();

		acceleration = getConfigFloat("projectile-acceleration", 0F);
		accelerationDelay = getConfigInt("projectile-acceleration-delay", 0);

		projectileTurn = getConfigFloat("projectile-turn", 0);
		projectileVelocity = getConfigFloat("projectile-velocity", 10F);
		projectileVertOffset = getConfigFloat("projectile-vert-offset", 0F);
		projectileHorizOffset = getConfigFloat("projectile-horiz-offset", 0F);
		float projectileGravity = getConfigFloat("projectile-gravity", 0F);
		projectileVertGravity = getConfigFloat("projectile-vert-gravity", projectileGravity);
		projectileHorizGravity = getConfigFloat("projectile-horiz-gravity", 0F);
		float projectileSpread = getConfigFloat("projectile-spread", 0F);
		projectileVertSpread = getConfigFloat("projectile-vertical-spread", projectileSpread);
		projectileHorizSpread = getConfigFloat("projectile-horizontal-spread", projectileSpread);

		tickInterval = getConfigInt("tick-interval", 2);
		ticksPerSecond = 20F / (float) tickInterval;
		jutsuInterval = getConfigInt("jutsu-interval", 20);
		intermediateEffects = getConfigInt("intermediate-effects", 0);
		specialEffectInterval = getConfigInt("special-effect-interval", 1);

		maxDistanceSquared = getConfigDouble("max-distance", 15);
		maxDistanceSquared *= maxDistanceSquared;
		maxDuration = getConfigDouble("max-duration", 0) * TimeUtil.MILLISECONDS_PER_SECOND;

		tickJutsuLimit = getConfigInt("tick-jutsu-limit", 0);
		intermediateHitboxes = getConfigInt("intermediate-hitboxes", 0);
		maxEntitiesHit = getConfigInt("max-entities-hit", 0);
		hitRadius = getConfigFloat("hit-radius", 1.5F);
		verticalHitRadius = getConfigFloat("vertical-hit-radius", hitRadius);
		groundHitRadius = getConfigInt("ground-hit-radius", 0);
		groundVerticalHitRadius = getConfigInt("ground-vertical-hit-radius", groundHitRadius);
		groundMaterials = new HashSet<>();
		List<String> groundMaterialNames = getConfigStringList("ground-materials", null);
		if (groundMaterialNames != null) {
			for (String str : groundMaterialNames) {
				Material material = Material.getMaterial(str.toUpperCase());
				if (material == null) continue;
				if (!material.isBlock()) continue;
				groundMaterials.add(material);
			}
		} else {
			for (Material material : Material.values()) {
				if (BlockUtils.isPathable(material)) continue;
				groundMaterials.add(material);
			}
		}

		hugSurface = getConfigBoolean("hug-surface", false);
		if (hugSurface) heightFromSurface = getConfigFloat("height-from-surface", 0.6F);

		controllable = getConfigBoolean("controllable", false);
		changePitch = getConfigBoolean("change-pitch", true);
		hitSelf = getConfigBoolean("hit-self", false);
		hitGround = getConfigBoolean("hit-ground", true);
		hitPlayers = getConfigBoolean("hit-players", false);
		hitAirAtEnd = getConfigBoolean("hit-air-at-end", false);
		hitAirDuring = getConfigBoolean("hit-air-during", false);
		hitNonPlayers = getConfigBoolean("hit-non-players", true);
		hitAirAfterDuration = getConfigBoolean("hit-air-after-duration", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", true);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", true);
		stopOnModifierFail = getConfigBoolean("stop-on-modifier-fail", true);
		allowCasterInteract = getConfigBoolean("allow-caster-interact", true);
		powerAffectsVelocity = getConfigBoolean("power-affects-velocity", true);
		if (stopOnHitEntity) maxEntitiesHit = 1;

		// Target List
		validTargetList.enforce(ValidTargetList.TargetingElement.TARGET_SELF, hitSelf);
		validTargetList.enforce(ValidTargetList.TargetingElement.TARGET_PLAYERS, hitPlayers);
		validTargetList.enforce(ValidTargetList.TargetingElement.TARGET_NONPLAYERS, hitNonPlayers);
		projModifiersStrings = getConfigStringList("projectile-modifiers", null);
		interactions = getConfigStringList("interactions", null);
		interactionJutsus = new HashMap<>();

		// Compatibility
		defaultJutsuName = getConfigString("jutsu", "");
		airJutsuName = getConfigString("jutsu-on-hit-air", defaultJutsuName);
		selfJutsuName = getConfigString("jutsu-on-hit-self", defaultJutsuName);
		tickJutsuName = getConfigString("jutsu-on-tick", defaultJutsuName);
		groundJutsuName = getConfigString("jutsu-on-hit-ground", defaultJutsuName);
		entityJutsuName = getConfigString("jutsu-on-hit-entity", defaultJutsuName);
		durationJutsuName = getConfigString("jutsu-on-duration-end", defaultJutsuName);
		modifierJutsuName = getConfigString("jutsu-on-modifier-fail", defaultJutsuName);
		entityLocationJutsuName = getConfigString("jutsu-on-entity-location", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		defaultJutsu = new Ninjutsu(defaultJutsuName);
		if (!defaultJutsu.process()) {
			if (!defaultJutsuName.isEmpty()) MagicJutsus.error("ParticleProjectileJutsu '" + internalName + "' has an invalid jutsu defined!");
			defaultJutsu = null;
		}

		airJutsu = new Ninjutsu(airJutsuName);
		if (!airJutsu.process() || !airJutsu.isTargetedLocationJutsu()) {
			if (!airJutsuName.equals(defaultJutsuName)) MagicJutsus.error("ParticleProjectileJutsu '" + internalName + "' has an invalid jutsu-on-hit-air defined!");
			airJutsu = null;
		}

		selfJutsu = new Ninjutsu(selfJutsuName);
		if (!selfJutsu.process()) {
			if (!selfJutsuName.equals(defaultJutsuName)) MagicJutsus.error("ParticleProjectileJutsu '" + internalName + "' has an invalid jutsu-on-hit-self defined!");
			selfJutsu = null;
		}

		tickJutsu = new Ninjutsu(tickJutsuName);
		if (!tickJutsu.process() || !tickJutsu.isTargetedLocationJutsu()) {
			if (!tickJutsuName.equals(defaultJutsuName)) MagicJutsus.error("ParticleProjectileJutsu '" + internalName + "' has an invalid jutsu-on-tick defined!");
			tickJutsu = null;
		}

		groundJutsu = new Ninjutsu(groundJutsuName);
		if (!groundJutsu.process() || !groundJutsu.isTargetedLocationJutsu()) {
			if (!groundJutsuName.equals(defaultJutsuName)) MagicJutsus.error("ParticleProjectileJutsu '" + internalName + "' has an invalid jutsu-on-hit-ground defined!");
			groundJutsu = null;
		}

		entityJutsu = new Ninjutsu(entityJutsuName);
		if (!entityJutsu.process()) {
			if (!entityJutsuName.equals(defaultJutsuName)) MagicJutsus.error("ParticleProjectileJutsu '" + internalName + "' has an invalid jutsu-on-hit-entity defined!");
			entityJutsu = null;
		}

		durationJutsu = new Ninjutsu(durationJutsuName);
		if (!durationJutsu.process()) {
			if (!durationJutsuName.equals(defaultJutsuName)) MagicJutsus.error("ParticleProjectileJutsu '" + internalName + "' has an invalid jutsu-on-duration-end defined!");
			durationJutsu = null;
		}

		modifierJutsu = new Ninjutsu(modifierJutsuName);
		if (!modifierJutsu.process()) {
			if (!modifierJutsuName.equals(defaultJutsuName)) MagicJutsus.error("ParticleProjectileJutsu '" + internalName + "' has an invalid jutsu-on-modifier-fail defined!");
			modifierJutsu = null;
		}

		entityLocationJutsu = new Ninjutsu(entityLocationJutsuName);
		if (!entityLocationJutsu.process() || !entityLocationJutsu.isTargetedLocationJutsu()) {
			if (!entityLocationJutsuName.isEmpty()) MagicJutsus.error("ParticleProjectileJutsu '" + internalName + "' has an invalid jutsu-on-entity-location defined!");
			entityLocationJutsu = null;
		}

		if (projModifiersStrings != null && !projModifiersStrings.isEmpty()) {
			projModifiers = new ModifierSet(projModifiersStrings);
			projModifiersStrings = null;
		}

		if (interactions != null && !interactions.isEmpty()) {
			for (String str : interactions) {
				String[] params = str.split(" ");
				if (params[0] == null) continue;
				if (params[0].equalsIgnoreCase(internalName)) {
					MagicJutsus.error("ParticleProjectileJutsu '" + internalName + "' has an interaction with itself!");
					continue;
				}

				Ninjutsu projectile = new Ninjutsu(params[0]);
				if (!projectile.process() || !(projectile.getJutsu() instanceof ParticleProjectileJutsu)) {
					MagicJutsus.error("ParticleProjectileJutsu '" + internalName + "' has an interaction with '" + params[0] + "' but that's not a valid particle projectile!");
					continue;
				}

				if (params.length == 1) {
					interactionJutsus.put(params[0], null);
					continue;
				}

				if (params.length <= 1) continue;
				if (params[1] == null) continue;
				Ninjutsu collisionJutsu = new Ninjutsu(params[1]);
				if (!collisionJutsu.process() || !collisionJutsu.isTargetedLocationJutsu()) {
					MagicJutsus.error("ParticleProjectileJutsu '" + internalName + "' has an interaction with '" + params[0] + "' and their jutsu on collision '" + params[1] + "' is not a valid jutsu!");
					continue;
				}
				interactionJutsus.put(params[0], collisionJutsu);
			}
		}
	}

	@Override
	public void turnOff() {
		trackerSet.clear();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity caster, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			ProjectileTracker tracker = new ProjectileTracker(caster, power);
			setupProjectile(tracker);
			tracker.start(caster.getLocation());
			playJutsuEffects(EffectPosition.CASTER, caster);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		ProjectileTracker tracker = new ProjectileTracker(caster, power);
		setupProjectile(tracker);
		tracker.start(target);
		playJutsuEffects(EffectPosition.CASTER, caster);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		Location targetLoc = target.clone();
		if (Float.isNaN(targetLoc.getPitch())) targetLoc.setPitch(0);
		ProjectileTracker tracker = new ProjectileTracker(null, power);
		setupProjectile(tracker);
		tracker.start(target);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		if (!caster.getLocation().getWorld().equals(target.getLocation().getWorld())) return false;
		Location targetLoc = from.clone();
		if (Float.isNaN(targetLoc.getPitch())) targetLoc.setPitch(0);
		ProjectileTracker tracker = new ProjectileTracker(caster, power);
		setupProjectile(tracker);
		tracker.startTarget(from, target);
		playJutsuEffects(from, target);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		if (!from.getWorld().equals(target.getLocation().getWorld())) return false;
		Location targetLoc = from.clone();
		if (Float.isNaN(targetLoc.getPitch())) targetLoc.setPitch(0);
		ProjectileTracker tracker = new ProjectileTracker(null, power);
		setupProjectile(tracker);
		tracker.startTarget(from, target);
		playJutsuEffects(from, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!caster.getLocation().getWorld().equals(target.getLocation().getWorld())) return false;
		ProjectileTracker tracker = new ProjectileTracker(caster, power);
		setupProjectile(tracker);
		tracker.startTarget(caster.getLocation(), target);
		playJutsuEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	public static Set<ProjectileTracker> getProjectileTrackers() {
		return trackerSet;
	}

	public void playEffects(EffectPosition position, Location loc) {
		playJutsuEffects(position, loc);
	}

	public void playEffects(EffectPosition position, Entity entity) {
		playJutsuEffects(position, entity);
	}

	private void setupProjectile(ProjectileTracker tracker) {
		tracker.setJutsu(this);
		tracker.setStartXOffset(startXOffset);
		tracker.setStartYOffset(startYOffset);
		tracker.setStartZOffset(startZOffset);
		tracker.setTargetYOffset(targetYOffset);

		tracker.setAcceleration(acceleration);
		tracker.setAccelerationDelay(accelerationDelay);

		tracker.setProjectileTurn(projectileTurn);
		tracker.setProjectileVelocity(projectileVelocity);
		tracker.setProjectileVertOffset(projectileVertOffset);
		tracker.setProjectileHorizOffset(projectileHorizOffset);
		tracker.setProjectileVertGravity(projectileVertGravity);
		tracker.setProjectileHorizGravity(projectileHorizGravity);
		tracker.setProjectileVertSpread(projectileVertSpread);
		tracker.setProjectileHorizSpread(projectileHorizSpread);

		tracker.setTickInterval(tickInterval);
		tracker.setTicksPerSecond(ticksPerSecond);
		tracker.setJutsuInterval(jutsuInterval);
		tracker.setIntermediateEffects(intermediateEffects);
		tracker.setIntermediateHitboxes(intermediateHitboxes);
		tracker.setSpecialEffectInterval(specialEffectInterval);

		tracker.setMaxDistanceSquared(maxDistanceSquared);
		tracker.setMaxDuration(maxDuration);

		tracker.setTickJutsuLimit(tickJutsuLimit);
		tracker.setMaxEntitiesHit(maxEntitiesHit);
		tracker.setHorizontalHitRadius(hitRadius);
		tracker.setVerticalHitRadius(verticalHitRadius);
		tracker.setGroundHorizontalHitRadius(groundHitRadius);
		tracker.setGroundVerticalHitRadius(groundVerticalHitRadius);
		tracker.setGroundMaterials(groundMaterials);

		tracker.setHugSurface(hugSurface);
		tracker.setHeightFromSurface(heightFromSurface);

		tracker.setControllable(controllable);
		tracker.setChangePitch(changePitch);
		tracker.setHitGround(hitGround);
		tracker.setHitAirAtEnd(hitAirAtEnd);
		tracker.setHitAirDuring(hitAirDuring);
		tracker.setHitAirAfterDuration(hitAirAfterDuration);
		tracker.setStopOnHitGround(stopOnHitGround);
		tracker.setStopOnModifierFail(stopOnModifierFail);
		tracker.setAllowCasterInteract(allowCasterInteract);
		tracker.setPowerAffectsVelocity(powerAffectsVelocity);

		tracker.setTargetList(validTargetList);
		tracker.setProjectileModifiers(projModifiers);
		tracker.setInteractionJutsus(interactionJutsus);

		tracker.setAirJutsu(airJutsu);
		tracker.setTickJutsu(tickJutsu);
		tracker.setCasterJutsu(selfJutsu);
		tracker.setGroundJutsu(groundJutsu);
		tracker.setEntityJutsu(entityJutsu);
		tracker.setDurationJutsu(durationJutsu);
		tracker.setModifierJutsu(modifierJutsu);
		tracker.setEntityLocationJutsu(entityLocationJutsu);
	}

}
