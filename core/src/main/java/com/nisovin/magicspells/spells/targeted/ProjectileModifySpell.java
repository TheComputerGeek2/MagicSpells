package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import org.apache.commons.math3.util.FastMath;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.instant.ParticleProjectileSpell;
import com.nisovin.magicspells.util.trackers.ParticleProjectileTracker;

public class ProjectileModifySpell extends TargetedSpell implements TargetedLocationSpell {

	private int cone;
	private int vRadius;
	private int hRadius;
	private int maxTargets;

	private boolean pointBlank;
	private boolean claimProjectiles;

	private SpellFilter filter;

	private float velocity;

	private float acceleration;
	private int accelerationDelay;

	private float projectileTurn;
	private float projectileVertGravity;
	private float projectileHorizGravity;

	private int tickInterval;
	private int spellInterval;
	private int tickSpellLimit;
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

	private ModifierSet projModifiers;
	private List<String> projModifiersStrings;

	public ProjectileModifySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		cone = getConfigInt("cone", 0);
		vRadius = getConfigInt("vertical-radius", 5);
		hRadius = getConfigInt("horizontal-radius", 10);
		maxTargets = getConfigInt("max-targets", 0);

		pointBlank = getConfigBoolean("point-blank", true);
		claimProjectiles = getConfigBoolean("claim-projectiles", false);

		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> spellTags = getConfigStringList("spell-tags", null);
		List<String> deniedSpellTags = getConfigStringList("denied-spell-tags", null);

		filter = new SpellFilter(spells, deniedSpells, spellTags, deniedSpellTags);

		velocity = getConfigFloat("projectile-velocity", 1F);
		acceleration = getConfigFloat("projectile-acceleration", 0F);
		accelerationDelay = getConfigInt("projectile-acceleration-delay", 0);

		projectileTurn = getConfigFloat("projectile-turn", 0);
		projectileVertGravity = getConfigFloat("projectile-vert-gravity", 0F);
		projectileHorizGravity = getConfigFloat("projectile-horiz-gravity", 0F);

		tickInterval = getConfigInt("tick-interval", 2);
		spellInterval = getConfigInt("spell-interval", 20);
		intermediateEffects = getConfigInt("intermediate-effects", 0);
		specialEffectInterval = getConfigInt("special-effect-interval", 1);

		maxDistanceSquared = getConfigDouble("max-distance", 15);
		maxDistanceSquared *= maxDistanceSquared;
		maxDuration = getConfigDouble("max-duration", 0) * TimeUtil.MILLISECONDS_PER_SECOND;

		intermediateHitboxes = getConfigInt("intermediate-hitboxes", 0);
		tickSpellLimit = getConfigInt("tick-spell-limit", 0);
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

		airSpell = new Subspell(airSpellName);
		if (!airSpell.process() || !airSpell.isTargetedLocationSpell()) {
			if (!airSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-hit-air defined!");
			airSpell = null;
		}

		selfSpell = new Subspell(selfSpellName);
		if (!selfSpell.process()) {
			if (!selfSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-hit-self defined!");
			selfSpell = null;
		}

		tickSpell = new Subspell(tickSpellName);
		if (!tickSpell.process() || !tickSpell.isTargetedLocationSpell()) {
			if (!tickSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-tick defined!");
			tickSpell = null;
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process() || !groundSpell.isTargetedLocationSpell()) {
			if (!groundSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
			groundSpell = null;
		}

		entitySpell = new Subspell(entitySpellName);
		if (!entitySpell.process()) {
			if (!entitySpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-hit-entity defined!");
			entitySpell = null;
		}

		durationSpell = new Subspell(durationSpellName);
		if (!durationSpell.process()) {
			if (!durationSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-duration-end defined!");
			durationSpell = null;
		}

		modifierSpell = new Subspell(modifierSpellName);
		if (!modifierSpell.process()) {
			if (!modifierSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-modifier-fail defined!");
			modifierSpell = null;
		}

		entityLocationSpell = new Subspell(entityLocationSpellName);
		if (!entityLocationSpell.process() || !entityLocationSpell.isTargetedLocationSpell()) {
			if (!entityLocationSpellName.isEmpty()) MagicSpells.error("ProjectileModifySpell '" + internalName + "' has an invalid spell-on-entity-location defined!");
			entityLocationSpell = null;
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			if (pointBlank) loc = caster.getLocation();
			else {
				try {
					Block block = getTargetedBlock(caster, power);
					if (block != null && !BlockUtils.isAir(block.getType())) loc = block.getLocation();
				} catch (IllegalStateException ignored) {}
			}
			if (loc == null) return noTarget(caster);

			modify(caster, loc, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		return modify(caster, target, power, args);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return modify(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return modify(null, target, power, args);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return modify(null, target, power, null);
	}

	private boolean modify(LivingEntity livingEntity, Location location, float power, String[] args) {
		int count = 0;

		Vector facing = livingEntity != null ? livingEntity.getLocation().getDirection() : location.getDirection();
		Vector vLoc = livingEntity != null ? livingEntity.getLocation().toVector() : location.toVector();

		BoundingBox box = new BoundingBox(location, hRadius, vRadius);

		Set<ParticleProjectileTracker> trackerSet = ParticleProjectileSpell.getProjectileTrackers();

		for (ParticleProjectileTracker tracker : trackerSet) {
			if (tracker == null) continue;
			if (!tracker.getCurrentLocation().getWorld().equals(location.getWorld())) continue;
			if (!box.contains(tracker.getCurrentLocation())) continue;
			if (tracker.getSpell() != null && !filter.check(tracker.getSpell())) continue;

			if (pointBlank && cone > 0) {
				Vector dir = tracker.getCurrentLocation().toVector().subtract(vLoc);
				if (FastMath.abs(dir.angle(facing)) > cone) continue;
			}

			if (claimProjectiles) tracker.setCaster(livingEntity);

			tracker.setAcceleration(acceleration);
			tracker.setAccelerationDelay(accelerationDelay);

			tracker.setProjectileTurn(projectileTurn);
			tracker.setProjectileVertGravity(projectileVertGravity);
			tracker.setProjectileHorizGravity(projectileHorizGravity);
			tracker.setTickInterval(tickInterval);
			tracker.setSpellInterval(spellInterval);
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
			tracker.setTickSpellLimit(tickSpellLimit);
			if (airSpell != null) tracker.setAirSpell(airSpell);
			if (tickSpell != null) tracker.setTickSpell(tickSpell);
			if (selfSpell != null) tracker.setCasterSpell(selfSpell);
			if (groundSpell != null) tracker.setGroundSpell(groundSpell);
			if (entitySpell != null) tracker.setEntitySpell(entitySpell);
			if (durationSpell != null) tracker.setDurationSpell(durationSpell);
			if (modifierSpell != null) tracker.setModifierSpell(modifierSpell);
			if (entityLocationSpell != null) tracker.setEntityLocationSpell(entityLocationSpell);

			tracker.getCurrentVelocity().multiply(velocity);

			playSpellEffects(EffectPosition.TARGET, tracker.getCurrentLocation());
			playSpellEffectsTrail(location, tracker.getCurrentLocation());
			if (livingEntity != null) playSpellEffectsTrail(livingEntity.getLocation(), tracker.getCurrentLocation());

			count++;

			if (maxTargets > 0 && count >= maxTargets) break;

		}

		if (livingEntity != null) playSpellEffects(EffectPosition.CASTER, livingEntity);
		playSpellEffects(EffectPosition.SPECIAL, location);

		return count > 0;
	}

}
