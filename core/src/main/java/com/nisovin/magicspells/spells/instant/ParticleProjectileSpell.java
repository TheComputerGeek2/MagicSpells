package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;
import com.nisovin.magicspells.util.trackers.ParticleProjectileTracker;

import org.apache.commons.math3.util.FastMath;

public class ParticleProjectileSpell extends InstantSpell implements TargetedLocationSpell, TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private static Set<ParticleProjectileTracker> trackerSet;

	private float targetYOffset;
	private float startXOffset;
	private float startYOffset;
	private float startZOffset;
	private Vector relativeOffset;
	private Vector effectOffset;

	private float acceleration;
	private int accelerationDelay;
	private float projectileTurn;
	private float projectileVelocity;
	private float projectileVertOffset;
	private float projectileHorizOffset;
	private double verticalRotation;
	private double horizontalRotation;
	private double xRotation;
	private float projectileVertSpread;
	private float projectileHorizSpread;
	private float projectileVertGravity;
	private float projectileHorizGravity;

	private int tickInterval;
	private float ticksPerSecond;
	private int spellInterval;
	private int intermediateEffects;
	private int specialEffectInterval;

	private int tickSpellLimit;
	private int intermediateHitboxes;
	private int maxEntitiesHit;
	private float hitRadius;
	private float verticalHitRadius;
	private int groundHitRadius;
	private int groundVerticalHitRadius;
	private Set<Material> groundMaterials;
	private Set<Material> disallowedGroundMaterials;

	private double maxDuration;
	private double maxDistanceSquared;

	private boolean hugSurface;
	private float heightFromSurface;

	private boolean controllable;
	private boolean checkPlugins;
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
	private Map<String, Subspell> interactionSpells;

	private Subspell airSpell;
	private Subspell selfSpell;
	private Subspell tickSpell;
	private Subspell entitySpell;
	private Subspell groundSpell;
	private Subspell durationSpell;
	private Subspell modifierSpell;
	private Subspell entityLocationSpell;
	private String airSpellName;
	private String selfSpellName;
	private String tickSpellName;
	private String entitySpellName;
	private String groundSpellName;
	private String durationSpellName;
	private String modifierSpellName;
	private String entityLocationSpellName;

	private Subspell defaultSpell;
	private String defaultSpellName;

	public ParticleProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

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

		effectOffset = getConfigVector("effect-offset", "0,0,0");

		acceleration = getConfigFloat("projectile-acceleration", 0F);
		accelerationDelay = getConfigInt("projectile-acceleration-delay", 0);

		projectileTurn = getConfigFloat("projectile-turn", 0);
		projectileVelocity = getConfigFloat("projectile-velocity", 10F);
		projectileVertOffset = getConfigFloat("projectile-vert-offset", 0F);
		projectileHorizOffset = getConfigFloat("projectile-horiz-offset", 0F);
		verticalRotation = getConfigFloat("vertical-rotation", 0F);
		horizontalRotation = getConfigFloat("horizontal-rotation", 0F);
		xRotation = getConfigFloat("x-rotation", 0F);
		float projectileGravity = getConfigFloat("projectile-gravity", 0F);
		projectileVertGravity = getConfigFloat("projectile-vert-gravity", projectileGravity);
		projectileHorizGravity = getConfigFloat("projectile-horiz-gravity", 0F);
		float projectileSpread = getConfigFloat("projectile-spread", 0F);
		projectileVertSpread = getConfigFloat("projectile-vertical-spread", projectileSpread);
		projectileHorizSpread = getConfigFloat("projectile-horizontal-spread", projectileSpread);

		tickInterval = getConfigInt("tick-interval", 2);
		ticksPerSecond = 20F / (float) tickInterval;
		spellInterval = getConfigInt("spell-interval", 20);
		intermediateEffects = getConfigInt("intermediate-effects", 0);
		specialEffectInterval = getConfigInt("special-effect-interval", 1);

		maxDistanceSquared = getConfigDouble("max-distance", 15);
		maxDistanceSquared *= maxDistanceSquared;
		maxDuration = getConfigDouble("max-duration", 0) * TimeUtil.MILLISECONDS_PER_SECOND;

		tickSpellLimit = getConfigInt("tick-spell-limit", 0);
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
				Material material = Util.getMaterial(str);
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
		disallowedGroundMaterials = new HashSet<>();
		List<String> disallowedGroundMaterialNames = getConfigStringList("disallowed-ground-materials", null);
		if (disallowedGroundMaterialNames != null) {
			for (String str : disallowedGroundMaterialNames) {
				Material material = Util.getMaterial(str);
				if (material == null) continue;
				if (!material.isBlock()) continue;
				disallowedGroundMaterials.add(material);
			}
		}

		hugSurface = getConfigBoolean("hug-surface", false);
		if (hugSurface) heightFromSurface = getConfigFloat("height-from-surface", 0.6F);

		controllable = getConfigBoolean("controllable", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
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
		interactionSpells = new HashMap<>();

		// Compatibility
		defaultSpellName = getConfigString("spell", "");
		airSpellName = getConfigString("spell-on-hit-air", defaultSpellName);
		selfSpellName = getConfigString("spell-on-hit-self", defaultSpellName);
		tickSpellName = getConfigString("spell-on-tick", defaultSpellName);
		groundSpellName = getConfigString("spell-on-hit-ground", defaultSpellName);
		entitySpellName = getConfigString("spell-on-hit-entity", defaultSpellName);
		durationSpellName = getConfigString("spell-on-duration-end", defaultSpellName);
		modifierSpellName = getConfigString("spell-on-modifier-fail", defaultSpellName);
		entityLocationSpellName = getConfigString("spell-on-entity-location", "");
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

		defaultSpell = new Subspell(defaultSpellName);
		if (!defaultSpell.process()) {
			if (!defaultSpellName.isEmpty()) MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an invalid spell defined!");
			defaultSpell = null;
		}

		airSpell = new Subspell(airSpellName);
		if (!airSpell.process() || !airSpell.isTargetedLocationSpell()) {
			if (!airSpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an invalid spell-on-hit-air defined!");
			airSpell = null;
		}

		selfSpell = new Subspell(selfSpellName);
		if (!selfSpell.process()) {
			if (!selfSpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an invalid spell-on-hit-self defined!");
			selfSpell = null;
		}

		tickSpell = new Subspell(tickSpellName);
		if (!tickSpell.process() || !tickSpell.isTargetedLocationSpell()) {
			if (!tickSpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an invalid spell-on-tick defined!");
			tickSpell = null;
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process() || !groundSpell.isTargetedLocationSpell()) {
			if (!groundSpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
			groundSpell = null;
		}

		entitySpell = new Subspell(entitySpellName);
		if (!entitySpell.process()) {
			if (!entitySpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an invalid spell-on-hit-entity defined!");
			entitySpell = null;
		}

		durationSpell = new Subspell(durationSpellName);
		if (!durationSpell.process()) {
			if (!durationSpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an invalid spell-on-duration-end defined!");
			durationSpell = null;
		}

		modifierSpell = new Subspell(modifierSpellName);
		if (!modifierSpell.process()) {
			if (!modifierSpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an invalid spell-on-modifier-fail defined!");
			modifierSpell = null;
		}

		entityLocationSpell = new Subspell(entityLocationSpellName);
		if (!entityLocationSpell.process() || !entityLocationSpell.isTargetedLocationSpell()) {
			if (!entityLocationSpellName.isEmpty()) MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an invalid spell-on-entity-location defined!");
			entityLocationSpell = null;
		}

		if (interactions != null && !interactions.isEmpty()) {
			for (String str : interactions) {
				String[] params = str.split(" ");
				if (params[0] == null) continue;

				Subspell projectile = new Subspell(params[0]);
				if (!projectile.process() || !(projectile.getSpell() instanceof ParticleProjectileSpell)) {
					MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an interaction with '" + params[0] + "' but that's not a valid particle projectile!");
					continue;
				}

				if (params.length == 1) {
					interactionSpells.put(params[0], null);
					continue;
				}

				if (params[1] == null) continue;
				Subspell collisionSpell = new Subspell(params[1]);
				if (!collisionSpell.process() || !collisionSpell.isTargetedLocationSpell()) {
					MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an interaction with '" + params[0] + "' and their spell on collision '" + params[1] + "' is not a valid spell!");
					continue;
				}
				interactionSpells.put(params[0], collisionSpell);
			}
		}
	}

	@Override
	public void turnOff() {
		for (ParticleProjectileTracker tracker : trackerSet) {
			tracker.stop(false);
		}
		trackerSet.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			ParticleProjectileTracker tracker = new ParticleProjectileTracker(caster, power);
			setupTracker(tracker, caster, null, power, args);
			tracker.start(caster.getLocation());
			playSpellEffects(EffectPosition.CASTER, caster);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		ParticleProjectileTracker tracker = new ParticleProjectileTracker(caster, power);
		setupTracker(tracker, caster, null, power, args);
		tracker.start(target);
		playSpellEffects(EffectPosition.CASTER, caster);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		Location targetLoc = target.clone();
		if (Float.isNaN(targetLoc.getPitch())) targetLoc.setPitch(0);
		ParticleProjectileTracker tracker = new ParticleProjectileTracker(null, power);
		setupTracker(tracker, null, null, power, args);
		tracker.start(target);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, String[] args) {
		if (!caster.getLocation().getWorld().equals(target.getLocation().getWorld())) return false;
		Location targetLoc = from.clone();
		if (Float.isNaN(targetLoc.getPitch())) targetLoc.setPitch(0);
		ParticleProjectileTracker tracker = new ParticleProjectileTracker(caster, power);
		setupTracker(tracker, caster, target, power, args);
		tracker.startTarget(from, target);
		playSpellEffects(from, target);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(caster, from, target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power, String[] args) {
		if (!from.getWorld().equals(target.getLocation().getWorld())) return false;
		Location targetLoc = from.clone();
		if (Float.isNaN(targetLoc.getPitch())) targetLoc.setPitch(0);
		ParticleProjectileTracker tracker = new ParticleProjectileTracker(null, power);
		setupTracker(tracker, null, target, power, args);
		tracker.startTarget(from, target);
		playSpellEffects(from, target);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(from, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!caster.getLocation().getWorld().equals(target.getLocation().getWorld())) return false;
		ParticleProjectileTracker tracker = new ParticleProjectileTracker(caster, power);
		setupTracker(tracker, caster, target, power, args);
		tracker.startTarget(caster.getLocation(), target);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	public static Set<ParticleProjectileTracker> getProjectileTrackers() {
		return trackerSet;
	}

	public void playEffects(EffectPosition position, Location loc) {
		playSpellEffects(position, loc);
	}

	public void playEffects(EffectPosition position, Entity entity) {
		playSpellEffects(position, entity);
	}

	public Set<EffectlibSpellEffect> playEffectsProjectile(EffectPosition position, Location location) {
		return playSpellEffectLibEffects(position, location);
	}

	public Set<Entity> playEntityEffectsProjectile(EffectPosition position, Location location) {
		return playSpellEntityEffects(position, location);
	}

	public Set<ArmorStand> playArmorStandEffectsProjectile(EffectPosition position, Location location) {
		return playSpellArmorStandEffects(position, location);
	}

	private void setupTracker(ParticleProjectileTracker tracker, LivingEntity caster, LivingEntity target, float power, String[] args) {
		tracker.setSpell(this);
		tracker.setStartXOffset(startXOffset);
		tracker.setStartYOffset(startYOffset);
		tracker.setStartZOffset(startZOffset);
		tracker.setTargetYOffset(targetYOffset);
		tracker.setEffectOffset(effectOffset);

		tracker.setAcceleration(acceleration);
		tracker.setAccelerationDelay(accelerationDelay);

		tracker.setProjectileTurn(projectileTurn);
		tracker.setProjectileVelocity(projectileVelocity);
		tracker.setVerticalRotation(FastMath.toRadians(verticalRotation));
		tracker.setHorizontalRotation(FastMath.toRadians(horizontalRotation));
		tracker.setXRotation(FastMath.toRadians(xRotation));
		tracker.setProjectileVertOffset(projectileVertOffset);
		tracker.setProjectileHorizOffset(projectileHorizOffset);
		tracker.setProjectileVertGravity(projectileVertGravity);
		tracker.setProjectileHorizGravity(projectileHorizGravity);
		tracker.setProjectileVertSpread(projectileVertSpread);
		tracker.setProjectileHorizSpread(projectileHorizSpread);

		tracker.setTickInterval(tickInterval);
		tracker.setTicksPerSecond(ticksPerSecond);
		tracker.setSpellInterval(spellInterval);
		tracker.setIntermediateEffects(intermediateEffects);
		tracker.setIntermediateHitboxes(intermediateHitboxes);
		tracker.setSpecialEffectInterval(specialEffectInterval);

		tracker.setMaxDistanceSquared(maxDistanceSquared);
		tracker.setMaxDuration(maxDuration);

		tracker.setTickSpellLimit(tickSpellLimit);
		tracker.setMaxEntitiesHit(maxEntitiesHit);
		tracker.setHorizontalHitRadius(hitRadius);
		tracker.setVerticalHitRadius(verticalHitRadius);
		tracker.setGroundHorizontalHitRadius(groundHitRadius);
		tracker.setGroundVerticalHitRadius(groundVerticalHitRadius);
		tracker.setGroundMaterials(groundMaterials);
		tracker.setDisallowedGroundMaterials(disallowedGroundMaterials);

		tracker.setHugSurface(hugSurface);
		tracker.setHeightFromSurface(heightFromSurface);

		tracker.setControllable(controllable);
		tracker.setCallEvents(true);
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
		tracker.setInteractionSpells(interactionSpells);

		tracker.setAirSpell(airSpell);
		tracker.setTickSpell(tickSpell);
		tracker.setCasterSpell(selfSpell);
		tracker.setGroundSpell(groundSpell);
		tracker.setEntitySpell(entitySpell);
		tracker.setDurationSpell(durationSpell);
		tracker.setModifierSpell(modifierSpell);
		tracker.setEntityLocationSpell(entityLocationSpell);
	}

	public float getTargetYOffset() {
		return targetYOffset;
	}

	public void setTargetYOffset(float targetYOffset) {
		this.targetYOffset = targetYOffset;
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

	public Vector getRelativeOffset() {
		return relativeOffset;
	}

	public void setRelativeOffset(Vector relativeOffset) {
		this.relativeOffset = relativeOffset;
	}

	public Vector getEffectOffset() {
		return effectOffset;
	}

	public void setEffectOffset(Vector effectOffset) {
		this.effectOffset = effectOffset;
	}

	public float getAcceleration() {
		return acceleration;
	}

	public void setAcceleration(float acceleration) {
		this.acceleration = acceleration;
	}

	public int getAccelerationDelay() {
		return accelerationDelay;
	}

	public void setAccelerationDelay(int accelerationDelay) {
		this.accelerationDelay = accelerationDelay;
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

	public void setVerticalRotation(float verticalRotation) {
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

	public float getTicksPerSecond() {
		return ticksPerSecond;
	}

	public void setTicksPerSecond(float ticksPerSecond) {
		this.ticksPerSecond = ticksPerSecond;
	}

	public int getSpellInterval() {
		return spellInterval;
	}

	public void setSpellInterval(int spellInterval) {
		this.spellInterval = spellInterval;
	}

	public int getIntermediateEffects() {
		return intermediateEffects;
	}

	public void setIntermediateEffects(int intermediateEffects) {
		this.intermediateEffects = intermediateEffects;
	}

	public int getSpecialEffectInterval() {
		return specialEffectInterval;
	}

	public void setSpecialEffectInterval(int specialEffectInterval) {
		this.specialEffectInterval = specialEffectInterval;
	}

	public int getTickSpellLimit() {
		return tickSpellLimit;
	}

	public void setTickSpellLimit(int tickSpellLimit) {
		this.tickSpellLimit = tickSpellLimit;
	}

	public int getIntermediateHitboxes() {
		return intermediateHitboxes;
	}

	public void setIntermediateHitboxes(int intermediateHitboxes) {
		this.intermediateHitboxes = intermediateHitboxes;
	}

	public int getMaxEntitiesHit() {
		return maxEntitiesHit;
	}

	public void setMaxEntitiesHit(int maxEntitiesHit) {
		this.maxEntitiesHit = maxEntitiesHit;
	}

	public float getVerticalHitRadius() {
		return verticalHitRadius;
	}

	public void setVerticalHitRadius(float verticalHitRadius) {
		this.verticalHitRadius = verticalHitRadius;
	}

	public float getHitRadius() {
		return hitRadius;
	}

	public void setHitRadius(float hitRadius) {
		this.hitRadius = hitRadius;
	}

	public int getGroundVerticalHitRadius() {
		return groundVerticalHitRadius;
	}

	public void setGroundVerticalHitRadius(int groundVerticalHitRadius) {
		this.groundVerticalHitRadius = groundVerticalHitRadius;
	}

	public int getGroundHitRadius() {
		return groundHitRadius;
	}

	public void setGroundHitRadius(int groundHitRadius) {
		this.groundHitRadius = groundHitRadius;
	}

	public Set<Material> getGroundMaterials() {
		return groundMaterials;
	}

	public Set<Material> getDisallowedGroundMaterials() {
		return disallowedGroundMaterials;
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

	public float getHeightFromSurface() {
		return heightFromSurface;
	}

	public void setHeightFromSurface(float heightFromSurface) {
		this.heightFromSurface = heightFromSurface;
	}

	public boolean isControllable() {
		return controllable;
	}

	public void setControllable(boolean controllable) {
		this.controllable = controllable;
	}

	public boolean shouldCheckPlugins() {
		return checkPlugins;
	}

	public void setCheckPlugins(boolean checkPlugins) {
		this.checkPlugins = checkPlugins;
	}

	public boolean shouldChangePitch() {
		return changePitch;
	}

	public void setChangePitch(boolean changePitch) {
		this.changePitch = changePitch;
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

	public ModifierSet getProjectileModifiers() {
		return projModifiers;
	}

	public Map<String, Subspell> getInteractionSpells() {
		return interactionSpells;
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
		return selfSpell;
	}

	public void setCasterSpell(Subspell selfSpell) {
		this.selfSpell = selfSpell;
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

}
