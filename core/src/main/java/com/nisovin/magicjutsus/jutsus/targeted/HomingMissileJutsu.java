package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.BoundingBox;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.util.ValidTargetChecker;
import com.nisovin.magicjutsus.castmodifiers.ModifierSet;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.events.JutsuPreImpactEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedEntityFromLocationJutsu;

public class HomingMissileJutsu extends TargetedJutsu implements TargetedEntityJutsu, TargetedEntityFromLocationJutsu {

	private HomingMissileJutsu thisJutsu;

	private ModifierSet homingModifiers;
	private List<String> homingModifiersStrings;

	private Vector relativeOffset;
	private Vector targetRelativeOffset;

	private boolean hitGround;
	private boolean hitAirDuring;
	private boolean stopOnHitGround;
	private boolean stopOnModifierFail;
	private boolean hitAirAfterDuration;

	private String hitJutsuName;
	private String airJutsuName;
	private String groundJutsuName;
	private String modifierJutsuName;
	private String durationJutsuName;

	private Ninjutsu hitJutsu;
	private Ninjutsu airJutsu;
	private Ninjutsu groundJutsu;
	private Ninjutsu modifierJutsu;
	private Ninjutsu durationJutsu;

	private double maxDuration;

	private float yOffset;
	private float hitRadius;
	private float ticksPerSecond;
	private float velocityPerTick;
	private float verticalHitRadius;
	private float projectileInertia;
	private float projectileVelocity;

	private int tickInterval;
	private int airJutsuInterval;
	private int specialEffectInterval;
	private int intermediateSpecialEffects;

	public HomingMissileJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		thisJutsu = this;

		homingModifiersStrings = getConfigStringList("homing-modifiers", null);

		relativeOffset = getConfigVector("relative-offset", "0,0.6,0");
		targetRelativeOffset = getConfigVector("target-relative-offset", "0,0.6,0");

		hitGround = getConfigBoolean("hit-ground", false);
		hitAirDuring = getConfigBoolean("hit-air-during", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);
		stopOnModifierFail = getConfigBoolean("stop-on-modifier-fail", true);
		hitAirAfterDuration = getConfigBoolean("hit-air-after-duration", false);

		hitJutsuName = getConfigString("jutsu", "");
		airJutsuName = getConfigString("jutsu-on-hit-air", "");
		groundJutsuName = getConfigString("jutsu-on-hit-ground", "");
		modifierJutsuName = getConfigString("jutsu-on-modifier-fail", "");
		durationJutsuName = getConfigString("jutsu-after-duration", "");

		maxDuration = getConfigDouble("max-duration", 20) * (double) TimeUtil.MILLISECONDS_PER_SECOND;

		yOffset = getConfigFloat("y-offset", 0.6F);
		hitRadius = getConfigFloat("hit-radius", 1.5F);
		verticalHitRadius = getConfigFloat("vertical-hit-radius", hitRadius);
		projectileInertia = getConfigFloat("projectile-inertia", 1.5F);
		projectileVelocity = getConfigFloat("projectile-velocity", 5F);

		tickInterval = getConfigInt("tick-interval", 2);
		airJutsuInterval = getConfigInt("jutsu-interval", 20);
		specialEffectInterval = getConfigInt("special-effect-interval", 2);
		intermediateSpecialEffects = getConfigInt("intermediate-special-effect-locations", 0);

		ticksPerSecond = 20F / (float) tickInterval;
		velocityPerTick = projectileVelocity / ticksPerSecond;

		if (airJutsuInterval <= 0) hitAirDuring = false;
		if (yOffset != 0.6F) relativeOffset.setY(yOffset);
		if (intermediateSpecialEffects < 0) intermediateSpecialEffects = 0;
	}

	@Override
	public void initialize() {
		super.initialize();

		if (homingModifiersStrings != null && !homingModifiersStrings.isEmpty()) {
			homingModifiers = new ModifierSet(homingModifiersStrings);
			homingModifiersStrings = null;
		}

		hitJutsu = new Ninjutsu(hitJutsuName);
		if (!hitJutsu.process()) {
			hitJutsu = null;
			if (!hitJutsuName.isEmpty()) MagicJutsus.error("HomingMissileJutsu '" + internalName + "' has an invalid jutsu defined!");
		}

		groundJutsu = new Ninjutsu(groundJutsuName);
		if (!groundJutsu.process() || !groundJutsu.isTargetedLocationJutsu()) {
			groundJutsu = null;
			if (!groundJutsuName.isEmpty()) MagicJutsus.error("HomingMissileJutsu '" + internalName + "' has an invalid jutsu-on-hit-ground defined!");
		}

		airJutsu = new Ninjutsu(airJutsuName);
		if (!airJutsu.process() || !airJutsu.isTargetedLocationJutsu()) {
			airJutsu = null;
			if (!airJutsuName.isEmpty()) MagicJutsus.error("HomingMissileJutsu '" + internalName + "' has an invalid jutsu-on-hit-air defined!");
		}

		durationJutsu = new Ninjutsu(durationJutsuName);
		if (!durationJutsu.process() || !durationJutsu.isTargetedLocationJutsu()) {
			durationJutsu = null;
			if (!durationJutsuName.isEmpty()) MagicJutsus.error("HomingMissileJutsu '" + internalName + "' has an invalid jutsu-after-duration defined!");
		}

		modifierJutsu = new Ninjutsu(modifierJutsuName);
		if (!modifierJutsu.process() || !modifierJutsu.isTargetedLocationJutsu()) {
			if (!modifierJutsuName.isEmpty()) MagicJutsus.error("HomingMissileJutsu '" + internalName + "' has an invalid jutsu-on-modifier-fail defined!");
			modifierJutsu = null;
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			ValidTargetChecker checker = hitJutsu != null ? hitJutsu.getJutsu().getValidTargetChecker() : null;
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power, checker);
			if (target == null) return noTarget(livingEntity);
			new MissileTracker(livingEntity, target.getTarget(), target.getPower());
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		new MissileTracker(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		new MissileTracker(null, target, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		new MissileTracker(caster, from, target, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		new MissileTracker(null, from, target, power);
		return true;
	}

	private class MissileTracker implements Runnable {

		LivingEntity caster;
		LivingEntity target;
		Location currentLocation;
		Vector currentVelocity;
		BoundingBox hitBox;
		float power;
		long startTime;
		int taskId;

		int counter = 0;

		private MissileTracker(LivingEntity caster, LivingEntity target, float power) {
			currentLocation = caster.getLocation().clone();
			currentVelocity = currentLocation.getDirection();
			init(caster, target, power);
			playJutsuEffects(EffectPosition.CASTER, caster);
		}

		private MissileTracker(LivingEntity caster, Location startLocation, LivingEntity target, float power) {
			currentLocation = startLocation.clone();
			if (Float.isNaN(currentLocation.getPitch())) currentLocation.setPitch(0);
			currentVelocity = target.getLocation().clone().toVector().subtract(currentLocation.toVector()).normalize();
			init(caster, target, power);

			if (caster != null) playJutsuEffects(EffectPosition.CASTER, caster);
			else playJutsuEffects(EffectPosition.CASTER, startLocation);
		}

		private void init(LivingEntity caster, LivingEntity target, float power) {
			this.caster = caster;
			this.target = target;
			this.power = power;

			currentVelocity.multiply(velocityPerTick);
			startTime = System.currentTimeMillis();
			taskId = MagicJutsus.scheduleRepeatingTask(this, 0, tickInterval);

			Vector startDir = caster.getLocation().clone().getDirection().normalize();
			Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
			currentLocation.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
			currentLocation.add(currentLocation.getDirection().multiply(relativeOffset.getX()));
			currentLocation.setY(currentLocation.getY() + relativeOffset.getY());

			hitBox = new BoundingBox(currentLocation, hitRadius, verticalHitRadius);
		}

		@Override
		public void run() {
			if ((caster != null && !caster.isValid()) || !target.isValid()) {
				stop();
				return;
			}

			if (!currentLocation.getWorld().equals(target.getWorld())) {
				stop();
				return;
			}

			if (homingModifiers != null && !homingModifiers.check(caster)) {
				if (modifierJutsu != null) modifierJutsu.castAtLocation(caster, currentLocation, power);
				if (stopOnModifierFail) stop();
				return;
			}

			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				if (hitAirAfterDuration && durationJutsu != null) durationJutsu.castAtLocation(caster, currentLocation, power);
				stop();
				return;
			}

			Location oldLocation = currentLocation.clone();

			// Calculate target location aka targetRelativeOffset
			Location targetLoc = target.getLocation().clone();
			Vector startDir = targetLoc.clone().getDirection().normalize();
			Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
			targetLoc.add(horizOffset.multiply(targetRelativeOffset.getZ())).getBlock().getLocation();
			targetLoc.add(targetLoc.getDirection().multiply(targetRelativeOffset.getX()));
			targetLoc.setY(target.getLocation().getY() + targetRelativeOffset.getY());

			// Move projectile and calculate new vector
			currentLocation.add(currentVelocity);
			Vector oldVelocity = new Vector(currentVelocity.getX(), currentVelocity.getY(), currentVelocity.getZ());
			currentVelocity.multiply(projectileInertia);
			currentVelocity.add(targetLoc.clone().subtract(currentLocation).toVector().normalize());
			currentVelocity.normalize().multiply(velocityPerTick);

			if (stopOnHitGround && !BlockUtils.isPathable(currentLocation.getBlock())) {
				if (hitGround && groundJutsu != null) groundJutsu.castAtLocation(caster, currentLocation, power);
				stop();
				return;
			}

			if (hitAirDuring && counter % airJutsuInterval == 0 && airJutsu != null) airJutsu.castAtLocation(caster, currentLocation, power);

			if (intermediateSpecialEffects > 0) playIntermediateEffectLocations(oldLocation, oldVelocity);

			// Update the location direction and play the effect
			currentLocation.setDirection(currentVelocity);
			playMissileEffect(currentLocation);

			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) playJutsuEffects(EffectPosition.SPECIAL, currentLocation);

			counter++;

			// Check for hit
			if (hitJutsu != null) {
				hitBox.setCenter(currentLocation);
				if (hitBox.contains(targetLoc)) {
					JutsuPreImpactEvent preImpact = new JutsuPreImpactEvent(hitJutsu.getJutsu(), thisJutsu, caster, target, power);
					EventUtil.call(preImpact);
					// Should we bounce the missile back?
					if (!preImpact.getRedirected()) {

						// Apparently didn't get redirected, carry out the plans
						if (hitJutsu.isTargetedEntityJutsu()) hitJutsu.castAtEntity(caster, target, power);
						else if (hitJutsu.isTargetedLocationJutsu()) hitJutsu.castAtLocation(caster, target.getLocation(), power);

						playJutsuEffects(EffectPosition.TARGET, target);
						stop();
					} else {
						redirect();
						power = preImpact.getPower();
					}
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
			playJutsuEffects(EffectPosition.SPECIAL, loc);
		}

		private void redirect() {
			LivingEntity c = caster;
			LivingEntity t =  target;
			caster = t;
			target = c;
			currentVelocity.multiply(-1F);
		}

		private void stop() {
			playJutsuEffects(EffectPosition.DELAYED, currentLocation);
			MagicJutsus.cancelTask(taskId);
			caster = null;
			target = null;
			currentLocation = null;
			currentVelocity = null;
		}

	}

}
