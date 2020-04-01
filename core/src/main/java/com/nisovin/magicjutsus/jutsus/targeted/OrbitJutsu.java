package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.BoundingBox;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.ValidTargetList;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;

public class OrbitJutsu extends TargetedJutsu implements TargetedEntityJutsu, TargetedLocationJutsu {

	private ValidTargetList entityTargetList;
	private List<String> targetList;

	private double maxDuration;

	private int tickInterval;
	private int vertExpandDelay;
	private int horizExpandDelay;

	private float yOffset;
	private float hitRadius;
	private float orbitRadius;
	private float horizOffset;
	private float ticksPerSecond;
	private float distancePerTick;
	private float vertExpandRadius;
	private float verticalHitRadius;
	private float horizExpandRadius;
	private float secondsPerRevolution;

	private boolean stopOnHitEntity;
	private boolean stopOnHitGround;
	private boolean counterClockwise;
	private boolean requireEntityTarget;

	private String orbitJutsuName;
	private String groundJutsuName;
	private String entityJutsuName;

	private Ninjutsu orbitJutsu;
	private Ninjutsu groundJutsu;
	private Ninjutsu entityJutsu;

	public OrbitJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		targetList = getConfigStringList("can-hit", null);
		entityTargetList = new ValidTargetList(this, targetList);

		maxDuration = getConfigDouble("max-duration", 20) * (double) TimeUtil.MILLISECONDS_PER_SECOND;

		tickInterval = getConfigInt("tick-interval", 2);
		vertExpandDelay = getConfigInt("vert-expand-delay", 0);
		horizExpandDelay = getConfigInt("horiz-expand-delay", 0);

		yOffset = getConfigFloat("y-offset", 0.6F);
		hitRadius = getConfigFloat("hit-radius", 1F);
		orbitRadius = getConfigFloat("orbit-radius", 1F);
		horizOffset = getConfigFloat("start-horiz-offset", 0);
		vertExpandRadius = getConfigFloat("vert-expand-radius", 0);
		verticalHitRadius = getConfigFloat("vertical-hit-radius", 1F);
		horizExpandRadius = getConfigFloat("horiz-expand-radius", 0);
		secondsPerRevolution = getConfigFloat("seconds-per-revolution", 3F);

		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);
		counterClockwise = getConfigBoolean("counter-clockwise", false);
		requireEntityTarget = getConfigBoolean("require-entity-target", true);

		orbitJutsuName = getConfigString("jutsu", "");
		groundJutsuName = getConfigString("jutsu-on-hit-ground", "");
		entityJutsuName = getConfigString("jutsu-on-hit-entity", "");

		ticksPerSecond = 20F / (float) tickInterval;
		distancePerTick = 6.28F / (ticksPerSecond * secondsPerRevolution);
	}

	@Override
	public void initialize() {
		super.initialize();

		orbitJutsu = new Ninjutsu(orbitJutsuName);
		if (!orbitJutsu.process() || !orbitJutsu.isTargetedLocationJutsu()) {
			orbitJutsu = null;
			if (!orbitJutsuName.isEmpty()) MagicJutsus.error("OrbitJutsu '" + internalName + "' has an invalid jutsu defined!");
		}

		groundJutsu = new Ninjutsu(groundJutsuName);
		if (!groundJutsu.process() || !groundJutsu.isTargetedLocationJutsu()) {
			groundJutsu = null;
			if (!groundJutsuName.isEmpty()) MagicJutsus.error("OrbitJutsu '" + internalName + "' has an invalid jutsu-on-hit-ground defined!");
		}

		entityJutsu = new Ninjutsu(entityJutsuName);
		if (!entityJutsu.process() || !entityJutsu.isTargetedEntityJutsu()) {
			entityJutsu = null;
			if (!entityJutsuName.isEmpty()) MagicJutsus.error("OrbitJutsu '" + internalName + "' has an invalid jutsu-on-hit-entity defined!");
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
				if (target == null) return noTarget(livingEntity);
				new OrbitTracker(livingEntity, target.getTarget(), target.getPower());
				playJutsuEffects(livingEntity, target.getTarget());
				sendMessages(livingEntity, target.getTarget());
				return PostCastAction.NO_MESSAGES;
			}

			Block block = getTargetedBlock(livingEntity, power);
			if (block != null) {
				JutsuTargetLocationEvent event = new JutsuTargetLocationEvent(this, livingEntity, block.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) block = null;
				else {
					block = event.getTargetLocation().getBlock();
					power = event.getPower();
				}
			}

			if (block == null) return noTarget(livingEntity);

			new OrbitTracker(livingEntity, block.getLocation().add(0.5, 0, 0.5), power);
			return PostCastAction.HANDLE_NORMALLY;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		new OrbitTracker(caster, target, power);
		playJutsuEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		new OrbitTracker(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	private class OrbitTracker implements Runnable {

		private LivingEntity caster;
		private LivingEntity target;
		private Location targetLoc;
		private Vector currentPosition;
		private BoundingBox box;
		private Set<LivingEntity> immune;

		private float power;
		private float orbRadius;
		private float orbHeight;

		private int taskId;
		private int repeatingHorizTaskId;
		private int repeatingVertTaskId;

		private long startTime;

		private OrbitTracker(LivingEntity caster, LivingEntity target, float power) {
			this.caster = caster;
			this.target = target;
			this.power = power;

			targetLoc = target.getLocation();
			initialize();
		}

		private OrbitTracker(LivingEntity caster, Location targetLoc, float power) {
			this.caster = caster;
			this.targetLoc = targetLoc;
			this.power = power;

			initialize();
		}

		private void initialize() {
			startTime = System.currentTimeMillis();
			currentPosition = targetLoc.getDirection().setY(0);
			Util.rotateVector(currentPosition, horizOffset);
			taskId = MagicJutsus.scheduleRepeatingTask(this, 0, tickInterval);
			orbRadius = orbitRadius;
			orbHeight = yOffset;
			immune = new HashSet<>();

			box = new BoundingBox(targetLoc, hitRadius, verticalHitRadius);

			if (horizExpandDelay > 0) repeatingHorizTaskId = MagicJutsus.scheduleRepeatingTask(() -> orbRadius += horizExpandRadius, horizExpandDelay, horizExpandDelay);
			if (vertExpandDelay > 0) repeatingVertTaskId = MagicJutsus.scheduleRepeatingTask(() -> orbHeight += vertExpandRadius, vertExpandDelay, vertExpandDelay);
		}

		@Override
		public void run() {
			if (!caster.isValid() || (target != null && !target.isValid())) {
				stop();
				return;
			}

			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				stop();
				return;
			}

			if (target != null) targetLoc = target.getLocation();

			Location loc = getLocation();

			if (!isTransparent(loc.getBlock())) {
				if (groundJutsu != null) groundJutsu.castAtLocation(caster, loc, power);
				if (stopOnHitGround) {
					stop();
					return;
				}
			}

			playJutsuEffects(EffectPosition.SPECIAL, loc);

			if (orbitJutsu != null) orbitJutsu.castAtLocation(caster, loc, power);

			box.setCenter(loc);

			for (LivingEntity e : caster.getWorld().getLivingEntities()) {
				if (e.equals(caster)) continue;
				if (e.isDead()) continue;
				if (immune.contains(e)) continue;
				if (!box.contains(e)) continue;
				if (entityTargetList != null && !entityTargetList.canTarget(e)) continue;

				JutsuTargetEvent event = new JutsuTargetEvent(OrbitJutsu.this, caster, e, power);
				EventUtil.call(event);
				if (event.isCancelled()) continue;

				immune.add(event.getTarget());
				if (entityJutsu != null) entityJutsu.castAtEntity(event.getCaster(), event.getTarget(), event.getPower());
				playJutsuEffects(EffectPosition.TARGET, event.getTarget());
				playJutsuEffectsTrail(targetLoc, event.getTarget().getLocation());
				if (stopOnHitEntity) {
					stop();
					return;
				}
			}
		}

		private Location getLocation() {
			Vector perp;
			if (counterClockwise) perp = new Vector(currentPosition.getZ(), 0, -currentPosition.getX());
			else perp = new Vector(-currentPosition.getZ(), 0, currentPosition.getX());
			currentPosition.add(perp.multiply(distancePerTick)).normalize();
			return targetLoc.clone().add(0, orbHeight, 0).add(currentPosition.clone().multiply(orbRadius));
		}


		private void stop() {
			if (target != null && target.isValid()) playJutsuEffects(EffectPosition.DELAYED, getLocation());
			MagicJutsus.cancelTask(taskId);
			MagicJutsus.cancelTask(repeatingHorizTaskId);
			MagicJutsus.cancelTask(repeatingVertTaskId);
			caster = null;
			target = null;
			targetLoc = null;
			currentPosition = null;
		}

	}

}
