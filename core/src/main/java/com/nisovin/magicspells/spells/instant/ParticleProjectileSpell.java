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
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;
import com.nisovin.magicspells.util.trackers.ParticleProjectileTracker;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

public class ParticleProjectileSpell extends InstantSpell implements TargetedLocationSpell, TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private static Set<ParticleProjectileTracker> trackerSet;

	private ConfigData<Float> targetYOffset;
	private ConfigData<Float> startXOffset;
	private ConfigData<Float> startYOffset;
	private ConfigData<Float> startZOffset;
	private Vector relativeOffset;
	private Vector effectOffset;

	private ConfigData<Float> acceleration;
	private ConfigData<Integer> accelerationDelay;
	private ConfigData<Float> projectileTurn;
	private ConfigData<Float> projectileVelocity;
	private ConfigData<Float> projectileVertOffset;
	private ConfigData<Float> projectileHorizOffset;
	private ConfigData<Double> verticalRotation;
	private ConfigData<Double> horizontalRotation;
	private ConfigData<Double> xRotation;
	private ConfigData<Float> projectileVertSpread;
	private ConfigData<Float> projectileHorizSpread;
	private ConfigData<Float> projectileVertGravity;
	private ConfigData<Float> projectileHorizGravity;

	private ConfigData<Integer> tickInterval;
	private ConfigData<Integer> spellInterval;
	private ConfigData<Integer> intermediateEffects;
	private ConfigData<Integer> specialEffectInterval;

	private ConfigData<Integer> tickSpellLimit;
	private ConfigData<Integer> intermediateHitboxes;
	private ConfigData<Integer> maxEntitiesHit;
	private ConfigData<Float> hitRadius;
	private ConfigData<Float> verticalHitRadius;
	private ConfigData<Integer> groundHitRadius;
	private ConfigData<Integer> groundVerticalHitRadius;
	private Set<Material> groundMaterials;
	private Set<Material> disallowedGroundMaterials;

	private ConfigData<Double> maxDuration;
	private ConfigData<Double> maxDistance;

	private boolean hugSurface;
	private ConfigData<Float> heightFromSurface;

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
		ConfigData<Float> startForwardOffset = getConfigDataFloat("start-forward-offset", 1F);
		startXOffset = getConfigDataFloat("start-x-offset", startForwardOffset);
		startYOffset = getConfigDataFloat("start-y-offset", 1F);
		startZOffset = getConfigDataFloat("start-z-offset", 0F);
		targetYOffset = getConfigDataFloat("target-y-offset", 0F);

		relativeOffset = getConfigVector("relative-offset", "1,1,0");
		effectOffset = getConfigVector("effect-offset", "0,0,0");

		acceleration = getConfigDataFloat("projectile-acceleration", 0F);
		accelerationDelay = getConfigDataInt("projectile-acceleration-delay", 0);

		projectileTurn = getConfigDataFloat("projectile-turn", 0);
		projectileVelocity = getConfigDataFloat("projectile-velocity", 10F);
		projectileVertOffset = getConfigDataFloat("projectile-vert-offset", 0F);
		projectileHorizOffset = getConfigDataFloat("projectile-horiz-offset", 0F);
		verticalRotation = getConfigDataDouble("vertical-rotation", 0F);
		horizontalRotation = getConfigDataDouble("horizontal-rotation", 0F);
		xRotation = getConfigDataDouble("x-rotation", 0F);

		ConfigData<Float> projectileGravity = getConfigDataFloat("projectile-gravity", 0F);
		projectileVertGravity = getConfigDataFloat("projectile-vert-gravity", projectileGravity);
		projectileHorizGravity = getConfigDataFloat("projectile-horiz-gravity", 0F);

		ConfigData<Float> projectileSpread = getConfigDataFloat("projectile-spread", 0F);
		projectileVertSpread = getConfigDataFloat("projectile-vertical-spread", projectileSpread);
		projectileHorizSpread = getConfigDataFloat("projectile-horizontal-spread", projectileSpread);

		tickInterval = getConfigDataInt("tick-interval", 2);
		spellInterval = getConfigDataInt("spell-interval", 20);
		intermediateEffects = getConfigDataInt("intermediate-effects", 0);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 1);

		maxDistance = getConfigDataDouble("max-distance", 15);
		maxDuration = getConfigDataDouble("max-duration", 0);

		tickSpellLimit = getConfigDataInt("tick-spell-limit", 0);
		intermediateHitboxes = getConfigDataInt("intermediate-hitboxes", 0);
		maxEntitiesHit = getConfigDataInt("max-entities-hit", 0);
		hitRadius = getConfigDataFloat("hit-radius", 1.5F);
		verticalHitRadius = getConfigDataFloat("vertical-hit-radius", hitRadius);
		groundHitRadius = getConfigDataInt("ground-hit-radius", 0);
		groundVerticalHitRadius = getConfigDataInt("ground-vertical-hit-radius", groundHitRadius);
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
		if (hugSurface) heightFromSurface = getConfigDataFloat("height-from-surface", 0.6F);

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
		if (stopOnHitEntity) maxEntitiesHit = (caster, target, power, args) -> 1;

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
			ParticleProjectileTracker tracker = new ParticleProjectileTracker(caster, power, args);
			setupTracker(tracker, caster, null, power, args);
			tracker.start(caster.getLocation());
			playSpellEffects(EffectPosition.CASTER, caster, tracker.getSpellData());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		ParticleProjectileTracker tracker = new ParticleProjectileTracker(caster, power, args);
		setupTracker(tracker, caster, null, power, args);
		tracker.start(target);
		playSpellEffects(EffectPosition.CASTER, caster, tracker.getSpellData());
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
		ParticleProjectileTracker tracker = new ParticleProjectileTracker(null, power, args);
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
		if (!validTargetList.canTarget(caster, target)) return false;
		if (!caster.getLocation().getWorld().equals(target.getLocation().getWorld())) return false;
		Location targetLoc = from.clone();
		if (Float.isNaN(targetLoc.getPitch())) targetLoc.setPitch(0);
		ParticleProjectileTracker tracker = new ParticleProjectileTracker(caster, power, args);
		setupTracker(tracker, caster, target, power, args);
		tracker.startTarget(from, target);
		playSpellEffects(caster, from, target, tracker.getSpellData());
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(caster, from, target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		if (!from.getWorld().equals(target.getLocation().getWorld())) return false;
		Location targetLoc = from.clone();
		if (Float.isNaN(targetLoc.getPitch())) targetLoc.setPitch(0);
		ParticleProjectileTracker tracker = new ParticleProjectileTracker(null, power, args);
		setupTracker(tracker, null, target, power, args);
		tracker.startTarget(from, target);
		playSpellEffects(from, target, tracker.getSpellData());
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(from, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		if (!caster.getLocation().getWorld().equals(target.getLocation().getWorld())) return false;
		ParticleProjectileTracker tracker = new ParticleProjectileTracker(caster, power, args);
		setupTracker(tracker, caster, target, power, args);
		tracker.startTarget(caster.getLocation(), target);
		playSpellEffects(caster, target, tracker.getSpellData());
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

	public void playEffects(EffectPosition position, Location loc, SpellData data) {
		playSpellEffects(position, loc, data);
	}

	public void playEffects(EffectPosition position, Entity entity, SpellData data) {
		playSpellEffects(position, entity, data);
	}

	public Set<EffectlibSpellEffect> playEffectsProjectile(EffectPosition position, Location location, SpellData data) {
		return playSpellEffectLibEffects(position, location, data);
	}

	public Set<Entity> playEntityEffectsProjectile(EffectPosition position, Location location, SpellData data) {
		return playSpellEntityEffects(position, location, data);
	}

	public Set<ArmorStand> playArmorStandEffectsProjectile(EffectPosition position, Location location, SpellData data) {
		return playSpellArmorStandEffects(position, location, data);
	}

	private void setupTracker(ParticleProjectileTracker tracker, LivingEntity caster, LivingEntity target, float power, String[] args) {
		tracker.setSpell(this);

		float startXOffset = (float) relativeOffset.getX();
		if (startXOffset == 1) startXOffset = this.startXOffset.get(caster, target, power, args);

		float startYOffset = (float) relativeOffset.getY();
		if (startYOffset == 1) startYOffset = this.startYOffset.get(caster, target, power, args);

		float startZOffset = (float) relativeOffset.getZ();
		if (startZOffset == 0) startZOffset = this.startZOffset.get(caster, target, power, args);

		tracker.setStartXOffset(startXOffset);
		tracker.setStartYOffset(startYOffset);
		tracker.setStartZOffset(startZOffset);
		tracker.setTargetYOffset(targetYOffset.get(caster, target, power, args));
		tracker.setEffectOffset(effectOffset);

		tracker.setAcceleration(acceleration.get(caster, target, power, args));
		tracker.setAccelerationDelay(accelerationDelay.get(caster, target, power, args));

		tracker.setProjectileTurn(projectileTurn.get(caster, target, power, args));
		tracker.setProjectileVelocity(projectileVelocity.get(caster, target, power, args));
		tracker.setVerticalRotation(AccurateMath.toRadians(verticalRotation.get(caster, target, power, args)));
		tracker.setHorizontalRotation(AccurateMath.toRadians(horizontalRotation.get(caster, target, power, args)));
		tracker.setXRotation(AccurateMath.toRadians(xRotation.get(caster, target, power, args)));
		tracker.setProjectileVertOffset(projectileVertOffset.get(caster, target, power, args));
		tracker.setProjectileHorizOffset(projectileHorizOffset.get(caster, target, power, args));
		tracker.setProjectileVertGravity(projectileVertGravity.get(caster, target, power, args));
		tracker.setProjectileHorizGravity(projectileHorizGravity.get(caster, target, power, args));
		tracker.setProjectileVertSpread(projectileVertSpread.get(caster, target, power, args));
		tracker.setProjectileHorizSpread(projectileHorizSpread.get(caster, target, power, args));

		int tickInterval = this.tickInterval.get(caster, target, power, args);
		tracker.setTickInterval(tickInterval);
		tracker.setTicksPerSecond(20f / tickInterval);

		tracker.setSpellInterval(spellInterval.get(caster, target, power, args));
		tracker.setIntermediateEffects(intermediateEffects.get(caster, target, power, args));
		tracker.setIntermediateHitboxes(intermediateHitboxes.get(caster, target, power, args));
		tracker.setSpecialEffectInterval(specialEffectInterval.get(caster, target, power, args));

		double maxDistance = this.maxDistance.get(caster, target, power, args);
		tracker.setMaxDistanceSquared(maxDistance * maxDistance);

		tracker.setMaxDuration(maxDuration.get(caster, target, power, args) * TimeUtil.MILLISECONDS_PER_SECOND);

		tracker.setTickSpellLimit(tickSpellLimit.get(caster, target, power, args));
		tracker.setMaxEntitiesHit(maxEntitiesHit.get(caster, target, power, args));
		tracker.setHorizontalHitRadius(hitRadius.get(caster, target, power, args));
		tracker.setVerticalHitRadius(verticalHitRadius.get(caster, target, power, args));
		tracker.setGroundHorizontalHitRadius(groundHitRadius.get(caster, target, power, args));
		tracker.setGroundVerticalHitRadius(groundVerticalHitRadius.get(caster, target, power, args));
		tracker.setGroundMaterials(groundMaterials);
		tracker.setDisallowedGroundMaterials(disallowedGroundMaterials);

		tracker.setHugSurface(hugSurface);
		tracker.setHeightFromSurface(hugSurface ? heightFromSurface.get(caster, target, power, args) : 0);

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

	public Set<Material> getGroundMaterials() {
		return groundMaterials;
	}

	public Set<Material> getDisallowedGroundMaterials() {
		return disallowedGroundMaterials;
	}

	public boolean shouldHugSurface() {
		return hugSurface;
	}

	public void setHugSurface(boolean hugSurface) {
		this.hugSurface = hugSurface;
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
