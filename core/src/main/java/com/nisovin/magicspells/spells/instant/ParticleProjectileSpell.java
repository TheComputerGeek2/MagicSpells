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

import de.slikey.effectlib.Effect;

public class ParticleProjectileSpell extends InstantSpell implements TargetedLocationSpell, TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private static Set<ProjectileTracker> trackerSet;

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
			projModifiers = new ModifierSet(projModifiersStrings);
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
				if (params[0].equalsIgnoreCase(internalName)) {
					MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an interaction with itself!");
					continue;
				}

				Subspell projectile = new Subspell(params[0]);
				if (!projectile.process() || !(projectile.getSpell() instanceof ParticleProjectileSpell)) {
					MagicSpells.error("ParticleProjectileSpell '" + internalName + "' has an interaction with '" + params[0] + "' but that's not a valid particle projectile!");
					continue;
				}

				if (params.length == 1) {
					interactionSpells.put(params[0], null);
					continue;
				}

				if (params.length <= 1) continue;
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
		for (ProjectileTracker tracker : trackerSet) {
			tracker.stop(false);
		}
		trackerSet.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			ProjectileTracker tracker = new ProjectileTracker(caster, power);
			setupProjectile(tracker);
			tracker.start(caster.getLocation());
			playSpellEffects(EffectPosition.CASTER, caster);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		ProjectileTracker tracker = new ProjectileTracker(caster, power);
		setupProjectile(tracker);
		tracker.start(target);
		playSpellEffects(EffectPosition.CASTER, caster);
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
		playSpellEffects(from, target);
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
		playSpellEffects(from, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!caster.getLocation().getWorld().equals(target.getLocation().getWorld())) return false;
		ProjectileTracker tracker = new ProjectileTracker(caster, power);
		setupProjectile(tracker);
		tracker.startTarget(caster.getLocation(), target);
		playSpellEffects(caster, target);
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
		playSpellEffects(position, loc);
	}

	public void playEffects(EffectPosition position, Entity entity) {
		playSpellEffects(position, entity);
	}

	public Set<Effect> playEffectsProjectile(EffectPosition position, Location location) {
		return playSpellEffectLibEffects(position, location);
	}

	public Set<Entity> playEntityEffectsProjectile(EffectPosition position, Location location) {
		return playSpellEntityEffects(position, location);
	}

	public Set<ArmorStand> playArmorStandEffectsProjectile(EffectPosition position, Location location) {
		return playSpellArmorStandEffects(position, location);
	}

	private void setupProjectile(ProjectileTracker tracker) {
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

}
