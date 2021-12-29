package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.ValidTargetChecker;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellPreImpactEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

import io.papermc.lib.PaperLib;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.effect.ModifiedEffect;

public class HomingMissileSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private HomingMissileSpell thisSpell;

	private NoMagicZoneManager zoneManager;

	private ModifierSet homingModifiers;
	private List<String> homingModifiersStrings;

	private Vector effectOffset;
	private Vector relativeOffset;
	private Vector targetRelativeOffset;

	private boolean hitGround;
	private boolean hitAirDuring;
	private boolean stopOnHitTarget;
	private boolean stopOnHitGround;
	private boolean stopOnModifierFail;
	private boolean hitAirAfterDuration;

	private String hitSpellName;
	private String airSpellName;
	private String groundSpellName;
	private String modifierSpellName;
	private String durationSpellName;
	private String entityLocationSpellName;

	private Subspell hitSpell;
	private Subspell airSpell;
	private Subspell groundSpell;
	private Subspell modifierSpell;
	private Subspell durationSpell;
	private Subspell entityLocationSpell;

	private ConfigData<Double> maxDuration;

	private ConfigData<Float> yOffset;
	private ConfigData<Float> hitRadius;
	private ConfigData<Float> verticalHitRadius;
	private ConfigData<Float> projectileInertia;
	private ConfigData<Float> projectileVelocity;

	private ConfigData<Integer> tickInterval;
	private ConfigData<Integer> airSpellInterval;
	private ConfigData<Integer> specialEffectInterval;
	private ConfigData<Integer> intermediateSpecialEffects;

	public HomingMissileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		thisSpell = this;

		homingModifiersStrings = getConfigStringList("homing-modifiers", null);

		effectOffset = getConfigVector("effect-offset", "0,0,0");
		relativeOffset = getConfigVector("relative-offset", "0,0.6,0");
		targetRelativeOffset = getConfigVector("target-relative-offset", "0,0.6,0");

		hitGround = getConfigBoolean("hit-ground", false);
		hitAirDuring = getConfigBoolean("hit-air-during", false);
		stopOnHitTarget = getConfigBoolean("stop-on-hit-target", true);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);
		stopOnModifierFail = getConfigBoolean("stop-on-modifier-fail", true);
		hitAirAfterDuration = getConfigBoolean("hit-air-after-duration", false);

		hitSpellName = getConfigString("spell", "");
		airSpellName = getConfigString("spell-on-hit-air", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		modifierSpellName = getConfigString("spell-on-modifier-fail", "");
		durationSpellName = getConfigString("spell-after-duration", "");
		entityLocationSpellName = getConfigString("spell-on-entity-location", "");

		maxDuration = getConfigDataDouble("max-duration", 20);

		yOffset = getConfigDataFloat("y-offset", 0.6F);
		hitRadius = getConfigDataFloat("hit-radius", 1.5F);
		verticalHitRadius = getConfigDataFloat("vertical-hit-radius", hitRadius);
		projectileInertia = getConfigDataFloat("projectile-inertia", 1.5F);
		projectileVelocity = getConfigDataFloat("projectile-velocity", 5F);

		tickInterval = getConfigDataInt("tick-interval", 2);
		airSpellInterval = getConfigDataInt("spell-interval", 20);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 2);
		intermediateSpecialEffects = getConfigDataInt("intermediate-special-effect-locations", 0);
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

		hitSpell = new Subspell(hitSpellName);
		if (!hitSpell.process()) {
			hitSpell = null;
			if (!hitSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell '" + internalName + "' has an invalid spell defined!");
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process() || !groundSpell.isTargetedLocationSpell()) {
			groundSpell = null;
			if (!groundSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
		}

		airSpell = new Subspell(airSpellName);
		if (!airSpell.process() || !airSpell.isTargetedLocationSpell()) {
			airSpell = null;
			if (!airSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell '" + internalName + "' has an invalid spell-on-hit-air defined!");
		}

		durationSpell = new Subspell(durationSpellName);
		if (!durationSpell.process() || !durationSpell.isTargetedLocationSpell()) {
			durationSpell = null;
			if (!durationSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell '" + internalName + "' has an invalid spell-after-duration defined!");
		}

		modifierSpell = new Subspell(modifierSpellName);
		if (!modifierSpell.process() || !modifierSpell.isTargetedLocationSpell()) {
			if (!modifierSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell '" + internalName + "' has an invalid spell-on-modifier-fail defined!");
			modifierSpell = null;
		}

		entityLocationSpell = new Subspell(entityLocationSpellName);
		if (!entityLocationSpell.process() || !entityLocationSpell.isTargetedLocationSpell()) {
			if (!entityLocationSpellName.isEmpty()) MagicSpells.error("HomingMissileSpell '" + internalName + "' has an invalid spell-on-entity-location defined!");
			entityLocationSpell = null;
		}

		zoneManager = MagicSpells.getNoMagicZoneManager();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			ValidTargetChecker checker = hitSpell != null ? hitSpell.getSpell().getValidTargetChecker() : null;
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power, checker, args);
			if (target == null) return noTarget(caster);
			new MissileTracker(caster, target.getTarget(), target.getPower(), args);
			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		new MissileTracker(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		new MissileTracker(null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		new MissileTracker(caster, from, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(caster, from, target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		new MissileTracker(null, from, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(from, target, power, null);
	}

	private class MissileTracker implements Runnable {

		private Set<EffectlibSpellEffect> effectSet;
		private Set<Entity> entitySet;
		private Set<ArmorStand> armorStandSet;

		private LivingEntity caster;
		private LivingEntity target;
		private Location currentLocation;
		private Vector currentVelocity;
		private BoundingBox hitBox;
		private float power;
		private long startTime;
		private int taskId;

		private Vector relativeOffset;

		private double maxDuration;

		private float velocityPerTick;
		private float projectileInertia;

		private int airSpellInterval;
		private int specialEffectInterval;
		private int intermediateSpecialEffects;

		private int counter = 0;

		private MissileTracker(LivingEntity caster, LivingEntity target, float power, String[] args) {
			currentLocation = caster.getLocation().clone();
			currentVelocity = currentLocation.getDirection();
			init(caster, target, power, args);
			playSpellEffects(EffectPosition.CASTER, caster);
		}

		private MissileTracker(LivingEntity caster, Location startLocation, LivingEntity target, float power, String[] args) {
			currentLocation = startLocation.clone();
			if (Float.isNaN(currentLocation.getPitch())) currentLocation.setPitch(0);
			currentVelocity = target.getLocation().clone().toVector().subtract(currentLocation.toVector()).normalize();
			init(caster, target, power, args);

			if (caster != null) playSpellEffects(EffectPosition.CASTER, caster);
			else playSpellEffects(EffectPosition.CASTER, startLocation);
		}

		private void init(LivingEntity caster, LivingEntity target, float power, String[] args) {
			this.caster = caster;
			this.target = target;
			this.power = power;

			startTime = System.currentTimeMillis();

			int tickInterval = HomingMissileSpell.this.tickInterval.get(caster, target, power, args);
			taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);

			maxDuration = HomingMissileSpell.this.maxDuration.get(caster, target, power, args) * TimeUtil.MILLISECONDS_PER_SECOND;

			projectileInertia = HomingMissileSpell.this.projectileInertia.get(caster, target, power, args);

			airSpellInterval = HomingMissileSpell.this.airSpellInterval.get(caster, target, power, args);
			specialEffectInterval = HomingMissileSpell.this.specialEffectInterval.get(caster, target, power, args);

			intermediateSpecialEffects = HomingMissileSpell.this.intermediateSpecialEffects.get(caster, target, power, args);
			if (intermediateSpecialEffects < 0) intermediateSpecialEffects = 0;

			float yOffset = HomingMissileSpell.this.yOffset.get(caster, target, power, args);
			relativeOffset = yOffset != 0.6f ? relativeOffset.clone().setY(yOffset) : relativeOffset;

			float projectileVelocity = HomingMissileSpell.this.projectileVelocity.get(caster, target, power, args);
			velocityPerTick = projectileVelocity * tickInterval / 20;
			currentVelocity.multiply(velocityPerTick);

			Vector startDir = caster.getLocation().clone().getDirection().normalize();
			Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
			currentLocation.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
			currentLocation.add(currentLocation.getDirection().multiply(relativeOffset.getX()));
			currentLocation.setY(currentLocation.getY() + relativeOffset.getY());

			float hitRadius = HomingMissileSpell.this.hitRadius.get(caster, target, power, args);
			float verticalHitRadius = HomingMissileSpell.this.verticalHitRadius.get(caster, target, power, args);
			hitBox = new BoundingBox(currentLocation, hitRadius, verticalHitRadius);

			effectSet = playSpellEffectLibEffects(EffectPosition.PROJECTILE, currentLocation);
			entitySet = playSpellEntityEffects(EffectPosition.PROJECTILE, currentLocation);
			armorStandSet = playSpellArmorStandEffects(EffectPosition.PROJECTILE, currentLocation);
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

			if (zoneManager.willFizzle(currentLocation, thisSpell)) {
				stop();
				return;
			}

			if (homingModifiers != null && !homingModifiers.check(caster)) {
				if (modifierSpell != null) modifierSpell.castAtLocation(caster, currentLocation, power);
				if (stopOnModifierFail) stop();
				return;
			}

			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				if (hitAirAfterDuration && durationSpell != null) durationSpell.castAtLocation(caster, currentLocation, power);
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

			if (armorStandSet != null || entitySet != null) {
				// Changing the effect location
				Vector dir = currentLocation.getDirection().normalize();
				Vector offset = new Vector(-dir.getZ(), 0.0, dir.getX()).normalize();
				Location effectLoc = currentLocation.clone();
				effectLoc.add(offset.multiply(effectOffset.getZ())).getBlock().getLocation();
				effectLoc.add(effectLoc.getDirection().multiply(effectOffset.getX()));
				effectLoc.setY(effectLoc.getY() + effectOffset.getY());

				if (armorStandSet != null) {
					for (ArmorStand armorStand : armorStandSet) {
						PaperLib.teleportAsync(armorStand, effectLoc);
					}
				}

				if (entitySet != null) {
					for (Entity entity : entitySet) {
						PaperLib.teleportAsync(entity, effectLoc);
					}
				}
			}

			if (stopOnHitGround && !BlockUtils.isPathable(currentLocation.getBlock())) {
				if (hitGround && groundSpell != null) groundSpell.castAtLocation(caster, currentLocation, power);
				stop();
				return;
			}

			if (hitAirDuring && airSpellInterval > 0 && counter % airSpellInterval == 0 && airSpell != null)
				airSpell.castAtLocation(caster, currentLocation, power);

			if (intermediateSpecialEffects > 0) playIntermediateEffectLocations(oldLocation, oldVelocity);

			// Update the location direction and play the effect
			currentLocation.setDirection(currentVelocity);
			playMissileEffect(currentLocation);
			if (effectSet != null) {
				Effect effect;
				Location effectLoc;
				for (EffectlibSpellEffect spellEffect : effectSet) {
					if (spellEffect == null) continue;
					effect = spellEffect.getEffect();
					if (effect == null) continue;

					effectLoc = spellEffect.getSpellEffect().applyOffsets(currentLocation.clone());
					effect.setLocation(effectLoc);

					if (effect instanceof ModifiedEffect) {
						Effect modifiedEffect = ((ModifiedEffect) effect).getInnerEffect();
						if (modifiedEffect != null) modifiedEffect.setLocation(effectLoc);
					}
				}
			}

			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) playSpellEffects(EffectPosition.SPECIAL, currentLocation);

			counter++;

			if (hitSpell == null) return;

			hitBox.setCenter(currentLocation);
			if (hitBox.contains(targetLoc)) {
				SpellPreImpactEvent preImpact = new SpellPreImpactEvent(hitSpell.getSpell(), thisSpell, caster, target, power);
				EventUtil.call(preImpact);
				// Should we bounce the missile back?
				if (!preImpact.getRedirected()) {

					// Apparently didn't get redirected, carry out the plans
					if (hitSpell.isTargetedEntitySpell()) hitSpell.castAtEntity(caster, target, power);
					else if (hitSpell.isTargetedLocationSpell()) hitSpell.castAtLocation(caster, target.getLocation(), power);
					if (entityLocationSpell != null) entityLocationSpell.castAtLocation(caster, currentLocation, power);
					playSpellEffects(EffectPosition.TARGET, target);
					if (stopOnHitTarget) stop();
				} else {
					redirect();
					power = preImpact.getPower();
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
			playSpellEffects(EffectPosition.SPECIAL, loc);
		}

		private void redirect() {
			LivingEntity temp = target;
			target = caster;
			caster = temp;
			currentVelocity.multiply(-1F);
		}

		private void stop() {
			playSpellEffects(EffectPosition.DELAYED, currentLocation);
			MagicSpells.cancelTask(taskId);
			if (effectSet != null) {
				for (EffectlibSpellEffect spellEffect : effectSet) {
					if (spellEffect == null) continue;
					if (spellEffect.getEffect() == null) continue;
					spellEffect.getEffect().cancel();
				}
				effectSet.clear();
			}
			if (armorStandSet != null) {
				for (ArmorStand armorStand : armorStandSet) {
					armorStand.remove();
				}
				armorStandSet.clear();
			}
			if (entitySet != null) {
				for (Entity entity : entitySet) {
					entity.remove();
				}
				entitySet.clear();
			}
			caster = null;
			target = null;
			currentLocation = null;
			currentVelocity = null;
		}

	}

}
