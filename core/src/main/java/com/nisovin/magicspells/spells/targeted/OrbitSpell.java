package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;

import io.papermc.lib.PaperLib;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.effect.ModifiedEffect;

public class OrbitSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private static Set<OrbitTracker> trackerSet;

	private ValidTargetList entityTargetList;
	private List<String> targetList;

	private ConfigData<Double> maxDuration;

	private ConfigData<Integer> tickInterval;
	private ConfigData<Integer> vertExpandDelay;
	private ConfigData<Integer> horizExpandDelay;

	private ConfigData<Float> yOffset;
	private ConfigData<Float> hitRadius;
	private ConfigData<Float> orbitRadius;
	private ConfigData<Float> horizOffset;
	private ConfigData<Float> vertExpandRadius;
	private ConfigData<Float> verticalHitRadius;
	private ConfigData<Float> horizExpandRadius;
	private ConfigData<Float> secondsPerRevolution;

	private boolean stopOnHitEntity;
	private boolean stopOnHitGround;
	private boolean counterClockwise;
	private boolean requireEntityTarget;

	private String orbitSpellName;
	private String groundSpellName;
	private String entitySpellName;

	private Subspell orbitSpell;
	private Subspell groundSpell;
	private Subspell entitySpell;

	public OrbitSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		trackerSet = new HashSet<>();

		targetList = getConfigStringList("can-hit", null);
		entityTargetList = new ValidTargetList(this, targetList);

		maxDuration = getConfigDataDouble("max-duration", 20);

		tickInterval = getConfigDataInt("tick-interval", 2);
		vertExpandDelay = getConfigDataInt("vert-expand-delay", 0);
		horizExpandDelay = getConfigDataInt("horiz-expand-delay", 0);

		yOffset = getConfigDataFloat("y-offset", 0.6F);
		hitRadius = getConfigDataFloat("hit-radius", 1F);
		orbitRadius = getConfigDataFloat("orbit-radius", 1F);
		horizOffset = getConfigDataFloat("start-horiz-offset", 0);
		vertExpandRadius = getConfigDataFloat("vert-expand-radius", 0);
		verticalHitRadius = getConfigDataFloat("vertical-hit-radius", 1F);
		horizExpandRadius = getConfigDataFloat("horiz-expand-radius", 0);
		secondsPerRevolution = getConfigDataFloat("seconds-per-revolution", 3F);

		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);
		counterClockwise = getConfigBoolean("counter-clockwise", false);
		requireEntityTarget = getConfigBoolean("require-entity-target", true);

		orbitSpellName = getConfigString("spell", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		entitySpellName = getConfigString("spell-on-hit-entity", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		orbitSpell = new Subspell(orbitSpellName);
		if (!orbitSpell.process() || !orbitSpell.isTargetedLocationSpell()) {
			orbitSpell = null;
			if (!orbitSpellName.isEmpty())
				MagicSpells.error("OrbitSpell '" + internalName + "' has an invalid spell defined!");
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process() || !groundSpell.isTargetedLocationSpell()) {
			groundSpell = null;
			if (!groundSpellName.isEmpty())
				MagicSpells.error("OrbitSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
		}

		entitySpell = new Subspell(entitySpellName);
		if (!entitySpell.process() || !entitySpell.isTargetedEntitySpell()) {
			entitySpell = null;
			if (!entitySpellName.isEmpty())
				MagicSpells.error("OrbitSpell '" + internalName + "' has an invalid spell-on-hit-entity defined!");
		}
	}

	@Override
	public void turnOff() {
		for (OrbitTracker tracker : trackerSet) {
			tracker.stop(false);
		}
		trackerSet.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
				if (target == null) return noTarget(caster);
				new OrbitTracker(caster, target.getTarget(), target.getPower(), args);
				playSpellEffects(caster, target.getTarget());
				sendMessages(caster, target.getTarget(), args);
				return PostCastAction.NO_MESSAGES;
			}

			Block block = getTargetedBlock(caster, power);
			if (block != null) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, caster, block.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) block = null;
				else {
					block = event.getTargetLocation().getBlock();
					power = event.getPower();
				}
			}

			if (block == null) return noTarget(caster);

			new OrbitTracker(caster, block.getLocation().add(0.5, 0, 0.5), power, args);
			return PostCastAction.HANDLE_NORMALLY;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		new OrbitTracker(caster, target, power, args);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		new OrbitTracker(caster, target, power, null);
		playSpellEffects(caster, target);
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		new OrbitTracker(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		new OrbitTracker(caster, target, power, null);
		return false;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	public boolean hasOrbit(LivingEntity target) {
		for (OrbitTracker orbitTracker : trackerSet) {
			if (orbitTracker.target == null) continue;
			if (orbitTracker.target.equals(target)) return true;
		}
		return false;
	}

	public void removeOrbits(LivingEntity target) {
		Set<OrbitTracker> toRemove = new HashSet<>();
		for (OrbitTracker tracker : trackerSet) {
			if (tracker.target == null) continue;
			if (!tracker.target.equals(target)) continue;
			if (!internalName.equals(tracker.internalName)) continue;
			tracker.stop(false);
			toRemove.add(tracker);
		}

		toRemove.forEach(tracker -> trackerSet.remove(tracker));
		toRemove.clear();
	}

	private class OrbitTracker implements Runnable {

		private String internalName;

		private Set<EffectlibSpellEffect> effectSet;
		private Set<Entity> entitySet;
		private Set<ArmorStand> armorStandSet;

		private LivingEntity caster;
		private LivingEntity target;
		private Location targetLoc;
		private Vector currentPosition;
		private BoundingBox box;
		private Set<LivingEntity> immune;

		private float power;
		private float orbRadius;
		private float orbHeight;
		private float distancePerTick;

		private double maxDuration;

		private int taskId;
		private int repeatingHorizTaskId;
		private int repeatingVertTaskId;

		private long startTime;

		private OrbitTracker(LivingEntity caster, LivingEntity target, float power, String[] args) {
			this.caster = caster;
			this.target = target;
			this.power = power;

			targetLoc = target.getLocation();
			initialize(caster, target, power, args);
		}

		private OrbitTracker(LivingEntity caster, Location targetLoc, float power, String[] args) {
			this.caster = caster;
			this.targetLoc = targetLoc;
			this.power = power;

			initialize(caster, null, power, args);
		}

		private void initialize(LivingEntity caster, LivingEntity target, float power, String[] args) {
			internalName = OrbitSpell.this.internalName;
			startTime = System.currentTimeMillis();
			currentPosition = targetLoc.getDirection().setY(0);
			Util.rotateVector(currentPosition, horizOffset.get(caster, target, power, args));
			orbRadius = orbitRadius.get(caster, target, power, args);
			orbHeight = yOffset.get(caster, target, power, args);
			;
			immune = new HashSet<>();

			box = new BoundingBox(targetLoc, hitRadius.get(caster, target, power, args), verticalHitRadius.get(caster, target, power, args));

			int tickInterval = OrbitSpell.this.tickInterval.get(caster, target, power, args);
			taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);

			int horizExpandDelay = OrbitSpell.this.horizExpandDelay.get(caster, target, power, args);
			if (horizExpandDelay > 0) {
				float horizExpandRadius = OrbitSpell.this.horizExpandRadius.get(caster, target, power, args);
				repeatingHorizTaskId = MagicSpells.scheduleRepeatingTask(() -> orbRadius += horizExpandRadius, horizExpandDelay, horizExpandDelay);
			}

			int vertExpandDelay = OrbitSpell.this.vertExpandDelay.get(caster, target, power, args);
			if (vertExpandDelay > 0) {
				float vertExpandRadius = OrbitSpell.this.vertExpandRadius.get(caster, target, power, args);
				repeatingVertTaskId = MagicSpells.scheduleRepeatingTask(() -> orbHeight += vertExpandRadius, vertExpandDelay, vertExpandDelay);
			}

			distancePerTick = 6.28f * tickInterval / secondsPerRevolution.get(caster, target, power, args) / 20;

			maxDuration = OrbitSpell.this.maxDuration.get(caster, target, power, args) * TimeUtil.MILLISECONDS_PER_SECOND;

			effectSet = playSpellEffectLibEffects(EffectPosition.PROJECTILE, targetLoc);
			entitySet = playSpellEntityEffects(EffectPosition.PROJECTILE, targetLoc);
			armorStandSet = playSpellArmorStandEffects(EffectPosition.PROJECTILE, targetLoc);

			trackerSet.add(this);
		}

		@Override
		public void run() {
			if (!caster.isValid() || (target != null && !target.isValid())) {
				stop(true);
				return;
			}

			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				stop(true);
				return;
			}

			if (target != null) targetLoc = target.getLocation();

			Location loc = getLocation();

			if (!isTransparent(loc.getBlock())) {
				if (groundSpell != null) groundSpell.castAtLocation(caster, loc, power);
				if (stopOnHitGround) {
					stop(true);
					return;
				}
			}

			playSpellEffects(EffectPosition.SPECIAL, loc);

			if (effectSet != null) {
				Effect effect;
				Location effectLoc;
				for (EffectlibSpellEffect spellEffect : effectSet) {
					if (spellEffect == null) continue;
					effect = spellEffect.getEffect();
					if (effect == null) continue;

					effectLoc = spellEffect.getSpellEffect().applyOffsets(loc.clone());
					effect.setLocation(effectLoc);

					if (effect instanceof ModifiedEffect) {
						Effect modifiedEffect = ((ModifiedEffect) effect).getInnerEffect();
						if (modifiedEffect != null) modifiedEffect.setLocation(effectLoc);
					}
				}
			}

			if (armorStandSet != null) {
				for (ArmorStand armorStand : armorStandSet) {
					PaperLib.teleportAsync(armorStand, loc);
				}
			}

			if (entitySet != null) {
				for (Entity entity : entitySet) {
					PaperLib.teleportAsync(entity, loc);
				}
			}

			if (orbitSpell != null) orbitSpell.castAtLocation(caster, loc, power);

			box.setCenter(loc);

			for (LivingEntity e : caster.getWorld().getLivingEntities()) {
				if (e.equals(caster)) continue;
				if (e.isDead()) continue;
				if (immune.contains(e)) continue;
				if (!box.contains(e)) continue;
				if (entityTargetList != null && !entityTargetList.canTarget(e)) continue;

				SpellTargetEvent event = new SpellTargetEvent(OrbitSpell.this, caster, e, power);
				EventUtil.call(event);
				if (event.isCancelled()) continue;

				immune.add(event.getTarget());
				if (entitySpell != null) entitySpell.castAtEntity(event.getCaster(), event.getTarget(), event.getPower());
				playSpellEffects(EffectPosition.TARGET, event.getTarget());
				playSpellEffectsTrail(targetLoc, event.getTarget().getLocation());
				if (stopOnHitEntity) {
					stop(true);
					return;
				}
			}
		}

		private Location getLocation() {
			Vector perp;
			if (counterClockwise) perp = new Vector(currentPosition.getZ(), 0, -currentPosition.getX());
			else perp = new Vector(-currentPosition.getZ(), 0, currentPosition.getX());
			currentPosition.add(perp.multiply(distancePerTick)).normalize();
			return targetLoc.clone().add(0, orbHeight, 0).add(currentPosition.clone().multiply(orbRadius)).setDirection(perp);
		}


		private void stop(boolean removeTracker) {
			if (target != null && target.isValid()) playSpellEffects(EffectPosition.DELAYED, getLocation());
			MagicSpells.cancelTask(taskId);
			MagicSpells.cancelTask(repeatingHorizTaskId);
			MagicSpells.cancelTask(repeatingVertTaskId);
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
			}
			if (entitySet != null) {
				for (Entity entity : entitySet) {
					entity.remove();
				}
			}
			caster = null;
			target = null;
			targetLoc = null;
			currentPosition = null;
			if (removeTracker) trackerSet.remove(this);
		}

	}

}
