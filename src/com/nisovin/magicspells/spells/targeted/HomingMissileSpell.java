package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellPreImpactEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

import de.slikey.effectlib.util.ParticleEffect;
import de.slikey.effectlib.util.ParticleEffect.ParticleData;

import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.EffectPackage;
import com.nisovin.magicspells.util.ParticleNameUtil;

public class HomingMissileSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	Vector relativeOffset;

	boolean stopOnHitGround;
	boolean hitGround;
	boolean hitAirDuring;
	boolean hitAirAfterDuration;

	String durationSpellName;
	String airSpellName;
	String hitSpellName;
	String groundSpellName;
	Subspell durationSpell;
	Subspell groundSpell;
	Subspell airSpell;
	Subspell spell;

	float projectileVelocity;
	float projectileInertia;

	int airSpellInterval;
	int tickInterval;
	float ticksPerSecond;
	float velocityPerTick;
	int specialEffectInterval;

	String particleName;
	float particleSpeed;
	int particleCount;
	float particleHorizontalSpread;
	float particleVerticalSpread;

	int maxDuration;
	float hitRadius;
	float yOffset;
	int renderDistance;

	boolean changeCasterOnReflect = true;

	HomingMissileSpell thisSpell;

	ParticleEffect effect;
	ParticleData data;

	boolean useParticles = false;

	int intermediateSpecialEffects = 0;

	public HomingMissileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		thisSpell = this;

		relativeOffset = getConfigVector("relative-offset", "0,0.6,0");
		yOffset = getConfigFloat("y-offset", 0.6F);
		if (yOffset != 0.6F) relativeOffset.setY(yOffset);

		hitAirAfterDuration = getConfigBoolean("hit-air-after-duration", false);
		durationSpellName = getConfigString("spell-after-duration", "");

		hitGround = getConfigBoolean("hit-ground", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);
		groundSpellName = getConfigString("spell-on-hit-ground", "");

		airSpellInterval = getConfigInt("spell-interval", 20);
		hitAirDuring = getConfigBoolean("hit-air-during", false);
		airSpellName = getConfigString("spell-on-hit-air", "");
		if (airSpellInterval <= 0) hitAirDuring = false;

		projectileVelocity = getConfigFloat("projectile-velocity", 5F);
		projectileInertia = getConfigFloat("projectile-inertia", 1.5F);
		tickInterval = getConfigInt("tick-interval", 2);
		ticksPerSecond = 20F / (float)tickInterval;
		velocityPerTick = projectileVelocity / ticksPerSecond;
		specialEffectInterval = getConfigInt("special-effect-interval", 0);
		particleName = getConfigString("particle-name", "reddust");
		particleSpeed = getConfigFloat("particle-speed", 0.3F);
		particleCount = getConfigInt("particle-count", 15);
		particleHorizontalSpread = getConfigFloat("particle-horizontal-spread", 0.3F);
		particleVerticalSpread = getConfigFloat("particle-vertical-spread", 0.3F);
		maxDuration = getConfigInt("max-duration", 20) * (int)TimeUtil.MILLISECONDS_PER_SECOND;
		hitRadius = getConfigFloat("hit-radius", 1.5F);
		renderDistance = getConfigInt("render-distance", 32);
		hitSpellName = getConfigString("spell", "");
		useParticles = getConfigBoolean("use-particles", false);
		changeCasterOnReflect = getConfigBoolean("change-caster-on-reflect", true);

		intermediateSpecialEffects = getConfigInt("intermediate-special-effect-locations", 0);
		if (intermediateSpecialEffects < 0) intermediateSpecialEffects = 0;

		EffectPackage pkg = ParticleNameUtil.findEffectPackage(particleName);
		effect = pkg.effect;
		data = pkg.data;
	}

	@Override
	public void initialize() {
		super.initialize();

		spell = new Subspell(hitSpellName);
		if (!spell.process()) {
			spell = null;
			MagicSpells.error("HomingMissileSpell " + internalName + " has an invalid spell defined!");
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process()) {
			groundSpell = null;
			if (!groundSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell " + internalName + " has an invalid spell-on-hit-ground defined!");
		}

		airSpell = new Subspell(airSpellName);
		if (!airSpell.process()) {
			airSpell = null;
			if (!airSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell " + internalName + " has an invalid spell-on-hit-air defined!");
		}

		durationSpell = new Subspell(durationSpellName);
		if (!durationSpell.process()) {
			durationSpell = null;
			if (!durationSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell " + internalName + " has an invalid spell-after-duration defined!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			ValidTargetChecker checker = spell != null ? spell.getSpell().getValidTargetChecker() : null;
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power, checker);
			if (target == null) return noTarget(player);
			new MissileTracker(player, target.getTarget(), target.getPower());
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
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
	public boolean castAtEntityFromLocation(Player caster, Location from, LivingEntity target, float power) {
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

	class MissileTracker implements Runnable {

		Player caster;
		LivingEntity target;
		float power;
		long startTime;
		Location currentLocation;
		Vector currentVelocity;
		int taskId;

		int counter = 0;

		public MissileTracker(Player caster, LivingEntity target, float power) {
			this.currentLocation = caster.getLocation();
			this.currentVelocity = currentLocation.getDirection();
			init(caster, target, power);
			playSpellEffects(EffectPosition.CASTER, caster);
		}

		public MissileTracker(Player caster, Location startLocation, LivingEntity target, float power) {
			this.currentLocation = startLocation.clone();
			this.currentVelocity = target.getLocation().toVector().subtract(currentLocation.toVector()).normalize();
			init(caster, target, power);
			if (caster != null) {
				playSpellEffects(EffectPosition.CASTER, caster);
			} else {
				playSpellEffects(EffectPosition.CASTER, startLocation);
			}
		}

		private void init(Player caster, LivingEntity target, float power) {
			this.currentVelocity.multiply(velocityPerTick);
			this.caster = caster;
			this.target = target;
			this.power = power;
			this.startTime = System.currentTimeMillis();
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);

			//apply relativeOffset
			Vector startDir = caster.getLocation().clone().getDirection().normalize();
			Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
			this.currentLocation.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
			this.currentLocation.add(this.currentLocation.getDirection().multiply(relativeOffset.getX()));
			this.currentLocation.setY(this.currentLocation.getY() + relativeOffset.getY());
		}

		@Override
		public void run() {
			// Check for valid and alive caster and target
			if ((caster != null && !caster.isValid()) || !target.isValid()) {
				stop();
				return;
			}

			// Check if target has left the world
			if (!currentLocation.getWorld().equals(target.getWorld())) {
				stop();
				return;
			}

			// Check if duration is up
			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				if (hitAirAfterDuration && durationSpell != null && durationSpell.isTargetedLocationSpell()) {
					durationSpell.castAtLocation(caster, currentLocation, power);
				}
				stop();
				return;
			}
			Location oldLocation = currentLocation.clone();

			// Move projectile and calculate new vector
			currentLocation.add(currentVelocity);
			Vector oldVelocity = new Vector(currentVelocity.getX(), currentVelocity.getY(), currentVelocity.getZ());
			currentVelocity.multiply(projectileInertia);
			currentVelocity.add(target.getLocation().add(0, yOffset, 0).subtract(currentLocation).toVector().normalize());
			currentVelocity.normalize().multiply(velocityPerTick);

			if (stopOnHitGround && !BlockUtils.isPathable(currentLocation.getBlock())) {
				if (hitGround && groundSpell != null && groundSpell.isTargetedLocationSpell()) {
					groundSpell.castAtLocation(caster, currentLocation, power);
				}
				stop();
				return;
			}

			if (hitAirDuring && counter % airSpellInterval == 0 && airSpell != null && airSpell.isTargetedLocationSpell()) {
				airSpell.castAtLocation(caster, currentLocation, power);
			}

			if (intermediateSpecialEffects > 0) {
				// Time to put extra effects in between
				playIntermediateEffectLocations(oldLocation, oldVelocity);
			}

			// Update the location direction and play the effect
			currentLocation.setDirection(currentVelocity);
			playMissileEffect(currentLocation);

			// Play effects
			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) {
				playSpellEffects(EffectPosition.SPECIAL, currentLocation);
			}

			counter++;

			// Check for hit
			if (hitRadius > 0 && spell != null) {
				BoundingBox hitBox = new BoundingBox(currentLocation, hitRadius);
				if (hitBox.contains(target.getLocation().add(0, yOffset, 0))) {
					// Fire off a preimpact event so reflect spells can still let us have our animation
					SpellPreImpactEvent preImpact = new SpellPreImpactEvent(spell.getSpell(), thisSpell, caster, target, power);
					EventUtil.call(preImpact);
					// Should we bounce the missile back?
					if (!preImpact.getRedirected()) {
						// Apparently didn't get redirected, carry out the plans
						if (spell.isTargetedEntitySpell()) {
							spell.castAtEntity(caster, target, power);
						} else if (spell.isTargetedLocationSpell()) {
							spell.castAtLocation(caster, target.getLocation(), power);
						}
						playSpellEffects(EffectPosition.TARGET, target);
						stop();
					} else {
						// If it got redirected, redirect it!
						redirect();
						power = preImpact.getPower();

					}
				}
			}
		}

		private void playIntermediateEffectLocations(Location old, Vector movement) {
			int divideFactor = intermediateSpecialEffects + 1;
			movement.setX(movement.getX()/divideFactor);
			movement.setY(movement.getY()/divideFactor);
			movement.setZ(movement.getZ()/divideFactor);
			for (int i = 0; i < intermediateSpecialEffects; i++) {
				old = old.add(movement).setDirection(movement);
				playMissileEffect(old);
			}
		}

		private void playMissileEffect(Location loc) {
			// Show particle
			if (useParticles) {
				//MagicSpells.getVolatileCodeHandler().playParticleEffect(currentLocation, particleName, particleHorizontalSpread, particleVerticalSpread, particleSpeed, particleCount, renderDistance, 0F);

				effect.display(data, loc, null, renderDistance, particleHorizontalSpread, particleVerticalSpread, particleHorizontalSpread, particleSpeed, particleCount);
				//ParticleData data, Location center, Color color, double range, float offsetX, float offsetY, float offsetZ, float speed, int amount
			} else {
				playSpellEffects(EffectPosition.SPECIAL, loc);
			}
		}

		private void redirect() {
			Player c = caster;
			Player t = (Player) target;
			caster = t;
			target = c;
			currentVelocity.multiply(-1F);
		}

		public void stop() {
			playSpellEffects(EffectPosition.DELAYED, currentLocation);
			MagicSpells.cancelTask(taskId);
			caster = null;
			target = null;
			currentLocation = null;
			currentVelocity = null;
		}

	}

}
