package com.nisovin.magicspells.spells.targeted;

import java.util.*;
import java.util.function.Predicate;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.effect.ModifiedEffect;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.trackers.Interaction;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;

public class OrbitSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private static Set<OrbitTracker> trackerSet;

	private final ValidTargetList entityTargetList;

	private final ConfigData<Double> hitRadius;
	private final ConfigData<Double> maxDuration;
	private final ConfigData<Double> verticalHitRadius;

	private final ConfigData<Integer> immuneTicks;
	private final ConfigData<Integer> tickInterval;
	private final ConfigData<Integer> vertExpandDelay;
	private final ConfigData<Integer> horizExpandDelay;

	private final ConfigData<Float> yOffset;
	private final ConfigData<Float> yawOffset;
	private final ConfigData<Float> angleOffset;
	private final ConfigData<Float> orbitRadius;
	private final ConfigData<Float> pitchOffset;
	private final ConfigData<Float> vertExpandRadius;
	private final ConfigData<Float> horizExpandRadius;
	private final ConfigData<Float> secondsPerRevolution;

	private final ConfigData<Boolean> followYaw;
	private final ConfigData<Boolean> followPitch;
	private final ConfigData<Boolean> lockStartYaw;
	private final ConfigData<Boolean> lockStartPitch;
	private final ConfigData<Boolean> stopOnHitEntity;
	private final ConfigData<Boolean> stopOnHitGround;
	private final ConfigData<Boolean> counterClockwise;
	private final ConfigData<Boolean> constantImmuneTicks;
	private final ConfigData<Boolean> requireEntityTarget;

	private final String orbitSpellName;
	private final String groundSpellName;
	private final String entitySpellName;

	private final List<?> interactionData;
	private List<Interaction> interactions;

	private Subspell orbitSpell;
	private Subspell groundSpell;
	private Subspell entitySpell;

	public OrbitSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		trackerSet = new HashSet<>();

		if (config.isList(internalKey + "can-hit"))
			entityTargetList = new ValidTargetList(this, getConfigStringList("can-hit", null));
		else
			entityTargetList = new ValidTargetList(this, getConfigString("can-hit", null));

		hitRadius = getConfigDataDouble("hit-radius", 1D);
		maxDuration = getConfigDataDouble("max-duration", 20);
		verticalHitRadius = getConfigDataDouble("vertical-hit-radius", 1D);

		immuneTicks = getConfigDataInt("immune-ticks", -1);
		tickInterval = getConfigDataInt("tick-interval", 2);
		vertExpandDelay = getConfigDataInt("vert-expand-delay", 0);
		horizExpandDelay = getConfigDataInt("horiz-expand-delay", 0);

		yOffset = getConfigDataFloat("y-offset", 0.6F);
		yawOffset = getConfigDataFloat("start-yaw-offset", getConfigDataFloat("start-horiz-offset", 0));
		angleOffset = getConfigDataFloat("start-angle-offset", 0);
		orbitRadius = getConfigDataFloat("orbit-radius", 1F);
		pitchOffset = getConfigDataFloat("start-pitch-offset", 0);
		vertExpandRadius = getConfigDataFloat("vert-expand-radius", 0);
		horizExpandRadius = getConfigDataFloat("horiz-expand-radius", 0);
		secondsPerRevolution = getConfigDataFloat("seconds-per-revolution", 3F);

		followYaw = getConfigDataBoolean("follow-yaw", false);
		followPitch = getConfigDataBoolean("follow-pitch", false);
		lockStartYaw = getConfigDataBoolean("lock-start-yaw", false);
		lockStartPitch = getConfigDataBoolean("lock-start-pitch", true);
		stopOnHitEntity = getConfigDataBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigDataBoolean("stop-on-hit-ground", false);
		counterClockwise = getConfigDataBoolean("counter-clockwise", false);
		constantImmuneTicks = getConfigDataBoolean("constant-immune-ticks", true);
		requireEntityTarget = getConfigDataBoolean("require-entity-target", true);

		orbitSpellName = getConfigString("spell", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		entitySpellName = getConfigString("spell-on-hit-entity", "");

		interactionData = getConfigList("interactions", null);
	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "OrbitSpell '" + internalName + "' has an invalid '%s' defined!";
		orbitSpell = initSubspell(orbitSpellName,
				error.formatted("spell"),
				true);
		groundSpell = initSubspell(groundSpellName,
				error.formatted("spell-on-hit-ground"),
				true);
		entitySpell = initSubspell(entitySpellName,
				error.formatted("spell-on-hit-entity"),
				true);

		if (interactionData == null || interactionData.isEmpty()) return;
		interactions = Interaction.read(this, interactionData);
	}

	@Override
	public void turnOff() {
		for (OrbitTracker tracker : trackerSet) tracker.stop(false);
		trackerSet.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		if (requireEntityTarget.get(data)) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(info);

			data = info.spellData();
			data = data.location(data.target().getLocation());

			new OrbitTracker(data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		TargetInfo<Location> info = getTargetedBlockLocation(data, 0.5, 0, 0.5);
		if (info.noTarget()) return noTarget(info);
		data = info.spellData();

		new OrbitTracker(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		data = data.location(data.target().getLocation());
		new OrbitTracker(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		new OrbitTracker(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public boolean hasOrbit(LivingEntity target) {
		for (OrbitTracker orbitTracker : trackerSet)
			if (this == orbitTracker.getOrbitSpell() && target.equals(orbitTracker.data.target()))
				return true;

		return false;
	}

	public void removeOrbits(LivingEntity target) {
		trackerSet.removeIf(tracker -> {
			if (OrbitSpell.this != tracker.getOrbitSpell()) return false;
			if (!target.equals(tracker.data.target())) return false;

			tracker.stop(false);
			return true;
		});
	}

	private class OrbitTracker implements Runnable {

		private SpellData data;
		private boolean stopped = false;

		private final BoundingBox box;
		private final Location center;

		private final Vector axis;
		private final Vector offset;
		private final Vector direction;
		private final Vector perpendicular;

		private final Object2IntMap<UUID> immune;
		private final Set<DelayableEntity<ArmorStand>> armorStandSet;
		private final Predicate<Location> transparent;
		private final Map<SpellEffect, DelayableEntity<Entity>> entityMap;
		private final Set<EffectlibSpellEffect> effectSet;

		private final boolean followYaw;
		private final boolean followPitch;
		private final boolean lockStartYaw;
		private final boolean lockStartPitch;
		private final boolean stopOnHitEntity;
		private final boolean stopOnHitGround;
		private final boolean counterClockwise;
		private final boolean constantImmuneTicks;

		private int tickCount;
		private final int immuneTicks;
		private final int ticksPerRevolution;

		private final ScheduledTask task;
		private final ScheduledTask repeatingVertTaskId;
		private final ScheduledTask repeatingHorizTaskId;

		private final long startTime;

		private float yOffset;
		private float orbitRadius;
		private float previousYaw;
		private float previousPitch;
		private final float startYaw;
		private final float yawOffset;
		private final float startPitch;
		private final float pitchOffset;

		private final double hitRadius;
		private final double angleOffset;
		private final double maxDuration;
		private final double verticalHitRadius;

		private OrbitTracker(SpellData data) {
			startTime = System.currentTimeMillis();

			center = data.location();
			startYaw = previousYaw = center.getYaw();
			startPitch = previousPitch = center.getPitch();

			hitRadius = OrbitSpell.this.hitRadius.get(data);
			verticalHitRadius = OrbitSpell.this.verticalHitRadius.get(data);
			box = BoundingBox.of(center, hitRadius, verticalHitRadius, hitRadius);

			yawOffset = OrbitSpell.this.yawOffset.get(data);
			angleOffset = AccurateMath.toRadians(OrbitSpell.this.angleOffset.get(data));
			pitchOffset = OrbitSpell.this.pitchOffset.get(data);
			lockStartYaw = OrbitSpell.this.lockStartYaw.get(data);
			lockStartPitch = OrbitSpell.this.lockStartPitch.get(data);
			counterClockwise = OrbitSpell.this.counterClockwise.get(data);

			double yaw = (lockStartYaw ? 0 : startYaw) + yawOffset;
			double pitch = (lockStartPitch ? 0 : startPitch) + pitchOffset;

			axis = Util.getDirection(yaw, pitch + (counterClockwise ? -90 : 90));
			offset = new Vector();
			direction = Util.getDirection(yaw, pitch);
			perpendicular = axis.clone().crossProduct(direction);

			followYaw = OrbitSpell.this.followYaw.get(data);
			followPitch = OrbitSpell.this.followPitch.get(data);
			stopOnHitEntity = OrbitSpell.this.stopOnHitEntity.get(data);
			stopOnHitGround = OrbitSpell.this.stopOnHitGround.get(data);

			int tickInterval = OrbitSpell.this.tickInterval.get(data);
			task = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval, center);

			orbitRadius = OrbitSpell.this.orbitRadius.get(data);
			int horizExpandDelay = OrbitSpell.this.horizExpandDelay.get(data);
			if (horizExpandDelay > 0) {
				float horizExpandRadius = OrbitSpell.this.horizExpandRadius.get(data);
				repeatingHorizTaskId = MagicSpells.scheduleRepeatingTask(() -> orbitRadius += horizExpandRadius, horizExpandDelay, horizExpandDelay, center);
			} else repeatingHorizTaskId = null;

			yOffset = OrbitSpell.this.yOffset.get(data);
			int vertExpandDelay = OrbitSpell.this.vertExpandDelay.get(data);
			if (vertExpandDelay > 0) {
				float vertExpandRadius = OrbitSpell.this.vertExpandRadius.get(data);
				repeatingVertTaskId = MagicSpells.scheduleRepeatingTask(() -> yOffset += vertExpandRadius, vertExpandDelay, vertExpandDelay, center);
			} else repeatingVertTaskId = null;

			ticksPerRevolution = Math.round(secondsPerRevolution.get(data) * 20 / tickInterval);

			maxDuration = OrbitSpell.this.maxDuration.get(data) * TimeUtil.MILLISECONDS_PER_SECOND;

			this.data = data;

			transparent = isTransparent(data);

			immune = new Object2IntArrayMap<>();
			immune.defaultReturnValue(-1);

			constantImmuneTicks = OrbitSpell.this.constantImmuneTicks.get(data);
			immuneTicks = constantImmuneTicks ? OrbitSpell.this.immuneTicks.get(data) : 0;

			entityMap = playSpellEntityEffects(EffectPosition.PROJECTILE, center, data);
			effectSet = playSpellEffectLibEffects(EffectPosition.PROJECTILE, center, data);
			armorStandSet = playSpellArmorStandEffects(EffectPosition.PROJECTILE, center, data);

			if (data.hasTarget()) playSpellEffects(data.caster(), data.target(), data);
			else playSpellEffects(data.caster(), center, data);

			trackerSet.add(this);
		}

		@Override
		public void run() {
			if (!data.caster().isValid() || (data.hasTarget() && !data.target().isValid())) {
				stop(true);
				return;
			}

			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				stop(true);
				return;
			}

			Location currentLocation = getCurrentLocation();
			data = data.location(currentLocation);

			if (!transparent.test(currentLocation)) {
				if (groundSpell != null) groundSpell.subcast(data.noTarget());
				if (stopOnHitGround) {
					stop(true);
					return;
				}
			}

			playSpellEffects(EffectPosition.SPECIAL, currentLocation, data);

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

			if (armorStandSet != null) {
				for (DelayableEntity<ArmorStand> armorStand : armorStandSet) {
					armorStand.teleport(currentLocation);
				}
			}

			if (entityMap != null) {
				for (var entry : entityMap.entrySet()) {
					entry.getValue().teleport(entry.getKey().applyOffsets(currentLocation.clone(), data));
				}
			}

			if (orbitSpell != null) orbitSpell.subcast(data.noTarget());

			box.resize(
				currentLocation.getX() - hitRadius,
				currentLocation.getY() - verticalHitRadius,
				currentLocation.getZ() - hitRadius,
				currentLocation.getX() + hitRadius,
				currentLocation.getY() + verticalHitRadius,
				currentLocation.getZ() + hitRadius
			);

			for (LivingEntity target : currentLocation.getNearbyLivingEntities(hitRadius, verticalHitRadius)) {
				if (entityTargetList != null && !entityTargetList.canTarget(data.caster(), target)) continue;

				int immuneTime = immune.getInt(target.getUniqueId());
				int currentTick = Bukkit.getCurrentTick();
				if (immuneTime >= 0 && immuneTime >= currentTick) continue;

				SpellTargetEvent event = new SpellTargetEvent(OrbitSpell.this, data, target);
				if (!event.callEvent()) continue;

				SpellData subData = event.getSpellData();
				target = event.getTarget();

				int immuneTicks = constantImmuneTicks ? this.immuneTicks : OrbitSpell.this.immuneTicks.get(subData);
				immune.put(target.getUniqueId(), immuneTicks >= 0 ? currentTick + immuneTicks : Integer.MAX_VALUE);

				if (entitySpell != null) entitySpell.subcast(subData.noLocation());

				playSpellEffects(EffectPosition.TARGET, target, subData);
				playSpellEffectsTrail(currentLocation, target.getLocation(), subData);

				if (stopOnHitEntity) {
					stop(true);
					return;
				}
			}

			if (ticksPerRevolution > 0) tickCount = (tickCount + 1) % ticksPerRevolution;

			if (interactions == null || interactions.isEmpty()) return;
			Set<OrbitTracker> toRemove = new HashSet<>();
			Set<OrbitTracker> trackers = new HashSet<>(trackerSet);
			for (OrbitTracker collisionTracker : trackers) {
				for (Interaction interaction : interactions) {
					if (!canInteractWith(collisionTracker)) continue;
					if (!interaction.interactsWith().check(collisionTracker.getOrbitSpell())) continue;

					if (interaction.canInteractList() != null && !interaction.canInteractList().canTarget(data.caster(), collisionTracker.data.caster()))
						continue;

					if (interaction.collisionSpell() != null) {
						Location middleLoc = this.center.clone().add(collisionTracker.center).multiply(0.5);
						interaction.collisionSpell().subcast(data.retarget(null, middleLoc));
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

			trackerSet.removeAll(toRemove);
			toRemove.clear();
			trackers.clear();
		}

		private boolean canInteractWith(OrbitTracker collisionTracker) {
			if (collisionTracker == null) return false;
			if (stopped || collisionTracker.stopped) return false;
			if (!data.hasCaster() || !collisionTracker.data.hasCaster()) return false;
			if (collisionTracker.equals(this)) return false;
			if (!collisionTracker.center.getWorld().equals(center.getWorld())) return false;
			return box.overlaps(collisionTracker.box);
		}

		private OrbitSpell getOrbitSpell() {
			return OrbitSpell.this;
		}

		private Location getCurrentLocation() {
			if (data.hasTarget()) {
				data.target().getLocation(center);

				float currentYaw = followYaw ? center.getYaw() : startYaw;
				float currentPitch = followPitch ? center.getPitch() : startPitch;
				if ((followYaw && previousYaw != currentYaw) || (followPitch && previousPitch != currentPitch)) {
					float yaw = (lockStartYaw ? currentYaw - startYaw : currentYaw) + yawOffset;
					float pitch = (lockStartPitch ? currentPitch - startPitch : currentPitch) + pitchOffset;

					Util.getDirection(axis, yaw, pitch + (counterClockwise ? -90 : 90));
					Util.getDirection(direction, yaw, pitch);
					perpendicular.copy(axis).crossProduct(direction);

					previousYaw = currentYaw;
					previousPitch = currentPitch;
				}
			}

			double angle = ticksPerRevolution > 0 ? 2 * tickCount * Math.PI / ticksPerRevolution : 0;
			angle += angleOffset;

			double cos = orbitRadius * Math.cos(angle);
			double sin = orbitRadius * Math.sin(angle);

			offset
				.setX(cos * direction.getX() + sin * perpendicular.getX())
				.setY(cos * direction.getY() + sin * perpendicular.getY())
				.setZ(cos * direction.getZ() + sin * perpendicular.getZ());

			return center.clone().add(offset).add(0, yOffset, 0).setDirection(offset.crossProduct(axis).multiply(-1));
		}

		private void stop(boolean removeTracker) {
			stopped = true;
			playSpellEffects(EffectPosition.DELAYED, getCurrentLocation(), data);

			MagicSpells.cancelTask(task);
			MagicSpells.cancelTask(repeatingHorizTaskId);
			MagicSpells.cancelTask(repeatingVertTaskId);

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

			if (removeTracker) trackerSet.remove(this);
		}

	}

}
