package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.effect.ModifiedEffect;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellPreImpactEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class HomingMissileSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private static final Set<MissileTracker> trackers = new HashSet<>();

	private NoMagicZoneManager zoneManager;

	private ModifierSet homingModifiers;
	private List<String> homingModifiersStrings;

	private final ConfigData<Vector> effectOffset;
	private final ConfigData<Vector> relativeOffset;
	private final ConfigData<Vector> targetRelativeOffset;

	private final ConfigData<Boolean> hitGround;
	private final ConfigData<Boolean> hitAirDuring;
	private final ConfigData<Boolean> stopOnHitTarget;
	private final ConfigData<Boolean> stopOnHitGround;
	private final ConfigData<Boolean> stopOnModifierFail;
	private final ConfigData<Boolean> hitAirAfterDuration;

	private final String hitSpellName;
	private final String airSpellName;
	private final String groundSpellName;
	private final String modifierSpellName;
	private final String durationSpellName;
	private final String entityLocationSpellName;

	private Subspell hitSpell;
	private Subspell airSpell;
	private Subspell groundSpell;
	private Subspell modifierSpell;
	private Subspell durationSpell;
	private Subspell entityLocationSpell;

	private final ConfigData<Double> yOffset;
	private final ConfigData<Double> maxDuration;

	private final ConfigData<Float> hitRadius;
	private final ConfigData<Float> verticalHitRadius;
	private final ConfigData<Float> projectileInertia;
	private final ConfigData<Float> projectileVelocity;

	private final ConfigData<Integer> tickInterval;
	private final ConfigData<Integer> airSpellInterval;
	private final ConfigData<Integer> specialEffectInterval;
	private final ConfigData<Integer> intermediateSpecialEffects;

	private final ConfigData<Float> projectileHorizOffset;
	private final ConfigData<Float> projectileVertOffset;

	private final ConfigData<Float> projectileHorizSpread;
	private final ConfigData<Float> projectileVertSpread;

	public HomingMissileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		homingModifiersStrings = getConfigStringList("homing-modifiers", null);

		effectOffset = getConfigDataVector("effect-offset", new Vector(0, 0, 0));
		relativeOffset = getConfigDataVector("relative-offset", new Vector(0, 0.6, 0));
		targetRelativeOffset = getConfigDataVector("target-relative-offset", new Vector(0, 0.6, 0));

		hitGround = getConfigDataBoolean("hit-ground", false);
		hitAirDuring = getConfigDataBoolean("hit-air-during", false);
		stopOnHitTarget = getConfigDataBoolean("stop-on-hit-target", true);
		stopOnHitGround = getConfigDataBoolean("stop-on-hit-ground", false);
		stopOnModifierFail = getConfigDataBoolean("stop-on-modifier-fail", true);
		hitAirAfterDuration = getConfigDataBoolean("hit-air-after-duration", false);

		hitSpellName = getConfigString("spell", "");
		airSpellName = getConfigString("spell-on-hit-air", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		modifierSpellName = getConfigString("spell-on-modifier-fail", "");
		durationSpellName = getConfigString("spell-after-duration", "");
		entityLocationSpellName = getConfigString("spell-on-entity-location", "");

		yOffset = getConfigDataDouble("y-offset", 0.6D);
		maxDuration = getConfigDataDouble("max-duration", 20);

		hitRadius = getConfigDataFloat("hit-radius", 1.5F);
		verticalHitRadius = getConfigDataFloat("vertical-hit-radius", hitRadius);
		projectileInertia = getConfigDataFloat("projectile-inertia", 1.5F);
		projectileVelocity = getConfigDataFloat("projectile-velocity", 5F);

		tickInterval = getConfigDataInt("tick-interval", 2);
		airSpellInterval = getConfigDataInt("spell-interval", 20);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 2);
		intermediateSpecialEffects = getConfigDataInt("intermediate-special-effect-locations", 0);

		projectileHorizOffset = getConfigDataFloat("projectile-horiz-offset", 0);
		projectileVertOffset = getConfigDataFloat("projectile-vert-offset", 0);

		projectileHorizSpread = getConfigDataFloat("projectile-horiz-spread", 0);
		projectileVertSpread = getConfigDataFloat("projectile-vert-spread", 0);
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		if (homingModifiersStrings != null && !homingModifiersStrings.isEmpty()) {
			homingModifiers = new ModifierSet(homingModifiersStrings, this);
			homingModifiersStrings = null;
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "HomingMissileSpell '" + internalName + "' has an invalid '%s' defined!";
		hitSpell = initSubspell(hitSpellName,
				error.formatted("spell"),
				true);
		groundSpell = initSubspell(groundSpellName,
				error.formatted("spell-on-hit-ground"),
				true);
		airSpell = initSubspell(airSpellName,
				error.formatted("spell-on-hit-air"),
				true);
		durationSpell = initSubspell(durationSpellName,
				error.formatted("spell-after-duration"),
				true);
		modifierSpell = initSubspell(modifierSpellName,
				error.formatted("spell-on-modifier-fail"),
				true);
		entityLocationSpell = initSubspell(entityLocationSpellName,
				error.formatted("spell-on-entity-location"),
				true);

		zoneManager = MagicSpells.getNoMagicZoneManager();
	}

	@Override
	public CastResult cast(SpellData data) {
		ValidTargetChecker checker = hitSpell != null ? hitSpell.getSpell().getValidTargetChecker() : null;
		TargetInfo<LivingEntity> info = getTargetedEntity(data, checker);
		if (info.noTarget()) return noTarget(info);
		data = info.spellData().location(data.caster().getLocation());

		new MissileTracker(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		data = data.location(data.caster().getLocation());

		new MissileTracker(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		new MissileTracker(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	protected void turnOff() {
		trackers.forEach(tracker -> tracker.stop(false));
		trackers.clear();
	}

	private class MissileTracker implements Runnable {

		private Location currentLocation;
		private Vector currentVelocity;
		private SpellData data;

		private final Set<EffectlibSpellEffect> effectSet;
		private final Map<SpellEffect, DelayableEntity<Entity>> entityMap;
		private final Set<DelayableEntity<ArmorStand>> armorStandSet;

		private final BoundingBox hitBox;
		private final long startTime;
		private final ScheduledTask task;

		private final Vector effectOffset;
		private final Vector targetRelativeOffset;

		private final double maxDuration;

		private final float velocityPerTick;
		private final float projectileInertia;

		private final int airSpellInterval;
		private final int specialEffectInterval;
		private final int intermediateSpecialEffects;

		private final boolean hitGround;
		private final boolean hitAirDuring;
		private final boolean stopOnHitTarget;
		private final boolean stopOnHitGround;
		private final boolean stopOnModifierFail;
		private final boolean hitAirAfterDuration;

		private int counter = 0;

		private MissileTracker(SpellData data) {
			trackers.add(this);
			startTime = System.currentTimeMillis();

			currentLocation = data.location();

			Vector relativeOffset = HomingMissileSpell.this.relativeOffset.get(data);

			double yOffset = HomingMissileSpell.this.yOffset.get(data);
			if (yOffset == 0.6D) yOffset = relativeOffset.getY();

			Util.applyRelativeOffset(currentLocation, relativeOffset.setY(0));
			currentLocation.add(0, yOffset, 0);

			float projectileHorizOffset = HomingMissileSpell.this.projectileHorizOffset.get(data);
			float projectileVertOffset = HomingMissileSpell.this.projectileVertOffset.get(data);

			float projectileHorizSpread = HomingMissileSpell.this.projectileHorizSpread.get(data);
			float projectileVertSpread = HomingMissileSpell.this.projectileVertSpread.get(data);

			currentVelocity = data.target().getLocation().subtract(currentLocation).toVector()
					.add(new Vector(0, projectileVertOffset, 0))
					.rotateAroundAxis(new Vector(0, 1, 0), -Math.toRadians(projectileHorizOffset))
					.normalize();

			if (projectileVertSpread > 0 || projectileHorizSpread > 0) {
				float rx = -1 + random.nextFloat() * 2;
				float ry = -1 + random.nextFloat() * 2;
				float rz = -1 + random.nextFloat() * 2;
				currentVelocity.add(new Vector(rx * projectileHorizSpread, ry * projectileVertSpread, rz * projectileHorizSpread));
			}
			currentLocation.setDirection(currentVelocity);
			data = data.location(currentLocation);
			this.data = data;

			float projectileVelocity = HomingMissileSpell.this.projectileVelocity.get(data);
			int tickInterval = HomingMissileSpell.this.tickInterval.get(data);
			velocityPerTick = projectileVelocity * tickInterval / 20;
			if (!currentVelocity.isZero()) currentVelocity.normalize().multiply(velocityPerTick);

			effectOffset = HomingMissileSpell.this.effectOffset.get(data);
			targetRelativeOffset = HomingMissileSpell.this.targetRelativeOffset.get(data);

			maxDuration = HomingMissileSpell.this.maxDuration.get(data) * TimeUtil.MILLISECONDS_PER_SECOND;

			projectileInertia = HomingMissileSpell.this.projectileInertia.get(data);

			airSpellInterval = HomingMissileSpell.this.airSpellInterval.get(data);
			specialEffectInterval = HomingMissileSpell.this.specialEffectInterval.get(data);

			intermediateSpecialEffects = Math.min(HomingMissileSpell.this.intermediateSpecialEffects.get(data), 0);

			float hitRadius = HomingMissileSpell.this.hitRadius.get(data);
			float verticalHitRadius = HomingMissileSpell.this.verticalHitRadius.get(data);
			hitBox = new BoundingBox(currentLocation, hitRadius, verticalHitRadius);

			hitGround = HomingMissileSpell.this.hitGround.get(data);
			hitAirDuring = HomingMissileSpell.this.hitAirDuring.get(data);
			stopOnHitTarget = HomingMissileSpell.this.stopOnHitTarget.get(data);
			stopOnHitGround = HomingMissileSpell.this.stopOnHitGround.get(data);
			stopOnModifierFail = HomingMissileSpell.this.stopOnModifierFail.get(data);
			hitAirAfterDuration = HomingMissileSpell.this.hitAirAfterDuration.get(data);

			effectSet = playSpellEffectLibEffects(EffectPosition.PROJECTILE, currentLocation, data);
			entityMap = playSpellEntityEffects(EffectPosition.PROJECTILE, currentLocation, data);
			armorStandSet = playSpellArmorStandEffects(EffectPosition.PROJECTILE, currentLocation, data);

			if (data.hasCaster()) playSpellEffects(EffectPosition.CASTER, data.caster(), data);

			task = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval, currentLocation);
		}

		@Override
		public void run() {
			if ((data.hasCaster() && !data.caster().isValid()) || !data.target().isValid()) {
				stop();
				return;
			}

			if (!currentLocation.getWorld().equals(data.target().getWorld())) {
				stop();
				return;
			}

			if (zoneManager.willFizzle(currentLocation, HomingMissileSpell.this)) {
				stop();
				return;
			}

			if (homingModifiers != null && data.hasCaster()) {
				ModifierResult result = homingModifiers.apply(data.caster(), data);
				data = result.data();

				if (!result.check()) {
					if (modifierSpell != null) modifierSpell.subcast(data.noTarget());
					if (stopOnModifierFail) stop();
					return;
				}
			}

			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				if (hitAirAfterDuration && durationSpell != null) durationSpell.subcast(data.noTarget());
				stop();
				return;
			}

			Location oldLocation = currentLocation.clone();

			// Calculate target location aka targetRelativeOffset
			Location targetLoc = data.target().getLocation();
			Vector startDir = targetLoc.getDirection();
			Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
			targetLoc.add(horizOffset.multiply(targetRelativeOffset.getZ()));
			targetLoc.add(startDir.multiply(targetRelativeOffset.getX()));
			targetLoc.setY(targetLoc.getY() + targetRelativeOffset.getY());

			// Move projectile and calculate new vector
			currentLocation.add(currentVelocity);
			Vector oldVelocity = currentVelocity.clone();
			data = data.location(currentLocation);

			currentVelocity.multiply(projectileInertia);
			Vector force = targetLoc.clone().subtract(currentLocation).toVector();
			if (!force.isZero()) currentVelocity.add(force.normalize());
			currentVelocity.normalize().multiply(velocityPerTick);

			if (armorStandSet != null || entityMap != null) {
				// Changing the effect location
				Vector dir = currentLocation.getDirection().normalize();
				Vector offset = new Vector(-dir.getZ(), 0.0, dir.getX()).normalize();
				Location effectLoc = currentLocation.clone();
				effectLoc.add(offset.multiply(effectOffset.getZ()));
				effectLoc.add(effectLoc.getDirection().multiply(effectOffset.getX()));
				effectLoc.setY(effectLoc.getY() + effectOffset.getY());
				effectLoc = Util.makeFinite(effectLoc);

				if (armorStandSet != null) {
					for (DelayableEntity<ArmorStand> armorStand : armorStandSet) {
						armorStand.teleport(effectLoc);
					}
				}

				if (entityMap != null) {
					for (var entry : entityMap.entrySet()) {
						entry.getValue().teleport(entry.getKey().applyOffsets(effectLoc.clone(), data));
					}
				}
			}

			if (stopOnHitGround && !currentLocation.getBlock().isPassable()) {
				if (hitGround && groundSpell != null) groundSpell.subcast(data.noTarget());
				stop();
				return;
			}

			if (hitAirDuring && airSpellInterval > 0 && counter % airSpellInterval == 0 && airSpell != null)
				airSpell.subcast(data.noTarget());

			if (intermediateSpecialEffects > 0) playIntermediateEffectLocations(oldLocation, oldVelocity);

			// Update the location direction and play the effect
			currentLocation.setDirection(currentVelocity);
			playMissileEffect(currentLocation);
			if (effectSet != null) {
				Effect effect;
				Location effectLoc;
				for (EffectlibSpellEffect spellEffect : effectSet) {
					effect = spellEffect.getEffect();

					effectLoc = spellEffect.getSpellEffect().applyOffsets(currentLocation.clone(), data);
					effect.setLocation(effectLoc);

					if (effect instanceof ModifiedEffect) {
						Effect modifiedEffect = ((ModifiedEffect) effect).getInnerEffect();
						if (modifiedEffect != null) modifiedEffect.setLocation(effectLoc);
					}
				}
			}

			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0)
				playSpellEffects(EffectPosition.SPECIAL, currentLocation, data);

			counter++;

			if (hitSpell == null) return;

			hitBox.setCenter(currentLocation);
			if (hitBox.contains(targetLoc)) {
				SpellPreImpactEvent preImpact = new SpellPreImpactEvent(hitSpell.getSpell(), HomingMissileSpell.this, data.caster(), data.target(), data.power());
				EventUtil.call(preImpact);
				// Should we bounce the missile back?
				if (!preImpact.getRedirected()) {
					// Apparently didn't get redirected, carry out the plans
					hitSpell.subcast(data.noLocation());
					if (entityLocationSpell != null) entityLocationSpell.subcast(data.noTarget());

					playSpellEffects(EffectPosition.TARGET, data.target(), data);
					if (stopOnHitTarget) stop();
				} else {
					if (!data.hasCaster()) {
						stop();
						return;
					}

					currentVelocity.multiply(-1);
					data = data.power(preImpact.getPower()).invert();
				}
			}

		}

		private void playIntermediateEffectLocations(Location old, Vector movement) {
			int divideFactor = intermediateSpecialEffects + 1;
			movement.setX(movement.getX() / divideFactor);
			movement.setY(movement.getY() / divideFactor);
			movement.setZ(movement.getZ() / divideFactor);
			for (int i = 0; i < intermediateSpecialEffects; i++) {
				old = old.add(movement).setDirection(movement);
				playMissileEffect(old);
			}
		}

		private void playMissileEffect(Location loc) {
			playSpellEffects(EffectPosition.SPECIAL, loc, data);
		}

		public void stop() {
			stop(true);
		}

		public void stop(boolean removeTracker) {
			if (removeTracker) trackers.remove(this);

			playSpellEffects(EffectPosition.DELAYED, currentLocation, data);
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
			currentLocation = null;
			currentVelocity = null;
		}

	}

}
