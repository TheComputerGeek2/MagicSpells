package com.nisovin.magicspells.spells.targeted;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.EffectPackage;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.util.ParticleNameUtil;

import de.slikey.effectlib.util.ParticleEffect;
import de.slikey.effectlib.util.ParticleEffect.ParticleData;

public class OrbitSpell extends TargetedSpell implements TargetedEntitySpell {

	ValidTargetList entityTargetList;
	List<String> targetList;

	float horizExpandRadius;
	float vertExpandRadius;
	int horizExpandDelay;
	int vertExpandDelay;

	float hitRadius;
	float verticalHitRadius;
	float orbitRadius;
	float secondsPerRevolution;
	boolean counterClockwise;

	String particleName;
	float particleSpeed;
	int particleCount;
	float particleHorizontalSpread;
	float particleVerticalSpread;

	int tickInterval;
	float ticksPerSecond;
	float distancePerTick;
	float horizOffset;

	int maxDuration;
	int renderDistance;
	float yOffset;

	boolean targetPlayers;
	boolean targetNonPlayers;
	boolean stopOnHitGround;
	boolean stopOnHitEntity;

	String groundSpellName;
	String entitySpellName;
	String orbitSpellName;
	Subspell groundSpell;
	Subspell entitySpell;
	Subspell orbitSpell;

	Color particleColor = null;
	ParticleEffect effect;
	ParticleData data;

	public OrbitSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		targetList = getConfigStringList("can-hit", null);
		entityTargetList = new ValidTargetList(this, targetList);

		horizExpandRadius = getConfigFloat("horiz-expand-radius", 0);
		horizExpandDelay = getConfigInt("horiz-expand-delay", 0);
		vertExpandRadius = getConfigFloat("vert-expand-radius", 0);
		vertExpandDelay = getConfigInt("vert-expand-delay", 0);

		hitRadius = getConfigFloat("hit-radius", 0);
		verticalHitRadius = getConfigFloat("vertical-hit-radius", 0);
		orbitRadius = getConfigFloat("orbit-radius", 1F);
		secondsPerRevolution = getConfigFloat("seconds-per-revolution", 3F);
		counterClockwise = getConfigBoolean("counter-clockwise", false);

		particleName = getConfigString("particle-name", "reddust");
		particleSpeed = getConfigFloat("particle-speed", 0.3F);
		particleCount = getConfigInt("particle-count", 15);
		particleHorizontalSpread = getConfigFloat("particle-horizontal-spread", 0.3F);
		particleVerticalSpread = getConfigFloat("particle-vertical-spread", 0.3F);

		horizOffset = getConfigFloat("start-horiz-offset", 0);
		tickInterval = getConfigInt("tick-interval", 2);
		ticksPerSecond = 20F / (float)tickInterval;
		distancePerTick = 6.28F / (ticksPerSecond * secondsPerRevolution);

		maxDuration = getConfigInt("max-duration", 20) * (int)TimeUtil.MILLISECONDS_PER_SECOND;
		yOffset = getConfigFloat("y-offset", 0.6F);
		renderDistance = getConfigInt("render-distance", 32);

		targetPlayers = getConfigBoolean("target-players", true);
		targetNonPlayers = getConfigBoolean("target-non-players", false);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);
		//TODO add color config
		EffectPackage pkg = ParticleNameUtil.findEffectPackage(particleName);
		effect = pkg.effect;
		data = pkg.data;

		groundSpellName = getConfigString("spell-on-hit-ground", "");
		entitySpellName = getConfigString("spell-on-hit-entity", "");
		orbitSpellName = getConfigString("spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		groundSpell = new Subspell(groundSpellName);
		entitySpell = new Subspell(entitySpellName);
		orbitSpell = new Subspell(orbitSpellName);

		if (groundSpellName.isEmpty()) {
			groundSpell = null;
		} else if (!groundSpell.process()) {
			MagicSpells.error("Orbit Spell '" + internalName + "' has an invalid spell-on-hit-ground defined");
			groundSpell = null;
		} else if (!groundSpell.isTargetedLocationSpell()) {
			MagicSpells.error("Orbit Spell '" + internalName + "' spell-on-hit-ground must be a targeted location spell");
			groundSpell = null;
		}

		if (entitySpellName.isEmpty()) {
			entitySpell = null;
		} else if (!entitySpell.process()) {
			MagicSpells.error("Orbit Spell '" + internalName + "' has an invalid spell-on-hit-entity defined");
			entitySpell = null;
		} else if (!entitySpell.isTargetedEntitySpell() && !entitySpell.isTargetedEntityFromLocationSpell()) {
			MagicSpells.error("Orbit Spell '" + internalName + "' spell-on-hit-entity must be a targeted entity spell");
			entitySpell = null;
		}

		if (orbitSpellName.isEmpty()) {
			orbitSpell = null;
		} else if (!orbitSpell.process()) {
			MagicSpells.error("Orbit Spell '" + internalName + "' has an invalid spell defined");
			orbitSpell = null;
		} else if (!orbitSpell.isTargetedLocationSpell()) {
			MagicSpells.error("Orbit Spell '" + internalName + "' spell must be a targeted location spell");
			orbitSpell = null;
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) return noTarget(player);
			new ParticleTracker(player, target.getTarget(), target.getPower());
			playSpellEffects(player, target.getTarget());
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		new ParticleTracker(caster, target, power);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	class ParticleTracker implements Runnable {

		Player caster;
		LivingEntity target;
		float power;
		long startTime;
		Vector currentPosition;
		int taskId;
		int repeatingHorizTaskId;
		int repeatingVertTaskId;
		float orbRadius;
		float orbHeight;
		BoundingBox box;
		Set<LivingEntity> immune;

		int counter = 0;

		public ParticleTracker(Player caster, LivingEntity target, float power) {
			this.caster = caster;
			this.target = target;
			this.power = power;
			this.startTime = System.currentTimeMillis();
			this.currentPosition = target.getLocation().getDirection().setY(0);
			Util.rotateVector(this.currentPosition, horizOffset);
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
			this.orbRadius = orbitRadius;
			this.orbHeight = yOffset;
			this.immune = new HashSet<>();
			if (horizExpandDelay > 0)
				this.repeatingHorizTaskId = MagicSpells.scheduleRepeatingTask(() -> this.orbRadius += horizExpandRadius, horizExpandDelay, horizExpandDelay);
			if (vertExpandDelay > 0)
				this.repeatingVertTaskId = MagicSpells.scheduleRepeatingTask(() -> this.orbHeight += vertExpandRadius, vertExpandDelay, vertExpandDelay);
		}

		@Override
		public void run() {
			// Check for valid and alive caster and target
			if (!caster.isValid() || !target.isValid()) {
				stop();
				return;
			}

			// Check if duration is up
			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				stop();
				return;
			}

			// Move projectile and calculate new vector
			Location loc = getLocation();

			if (!isTransparent(loc.getBlock())) {
				if (groundSpell != null) groundSpell.castAtLocation(caster, loc, power);
				if (stopOnHitGround) {
					stop();
					return;
				}
			}

			// Show particle
			//MagicSpells.getVolatileCodeHandler().playParticleEffect(loc, particleName, particleHorizontalSpread, particleVerticalSpread, particleSpeed, particleCount, renderDistance, 0F);

			playSpellEffects(EffectPosition.SPECIAL, loc);
			effect.display(data, loc, particleColor, renderDistance, particleHorizontalSpread, particleVerticalSpread, particleHorizontalSpread, particleSpeed, particleCount);

			// Cast the spell at the location if it isn't null
			if (orbitSpell != null) orbitSpell.castAtLocation(caster, loc, power);
			//ParticleData data, Location center, Color color, double range, float offsetX, float offsetY, float offsetZ, float speed, int amount

			box = new BoundingBox(loc, hitRadius, verticalHitRadius);

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
				if (stopOnHitEntity) {
					stop();
					return;
				}
			}
		}

		private Location getLocation() {
			Vector perp;
			if (counterClockwise) {
				perp = new Vector(currentPosition.getZ(), 0, -currentPosition.getX());
			} else {
				perp = new Vector(-currentPosition.getZ(), 0, currentPosition.getX());
			}
			currentPosition.add(perp.multiply(distancePerTick)).normalize();
			return target.getLocation().add(0, orbHeight, 0).add(currentPosition.clone().multiply(orbRadius));
		}


		public void stop() {
			if (target.isValid()) playSpellEffects(EffectPosition.DELAYED, getLocation());
			MagicSpells.cancelTask(taskId);
			MagicSpells.cancelTask(repeatingHorizTaskId);
			MagicSpells.cancelTask(repeatingVertTaskId);
			caster = null;
			target = null;
			currentPosition = null;
		}

	}

}
