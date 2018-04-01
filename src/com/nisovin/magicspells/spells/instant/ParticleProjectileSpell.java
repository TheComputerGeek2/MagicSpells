package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.EffectPackage;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.util.ParticleNameUtil;
import com.nisovin.magicspells.util.compat.EventUtil;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

import de.slikey.effectlib.util.ParticleEffect;
import de.slikey.effectlib.util.ParticleEffect.ParticleData;

public class ParticleProjectileSpell extends InstantSpell implements TargetedLocationSpell {

	float startXOffset;
	float startYOffset;
	float startZOffset;
	Vector relativeOffset;

	float projectileVelocity;
	float projectileVertOffset;
	float projectileHorizOffset;
	float projectileSpread;
	float projectileVertGravity;
	float projectileHorizGravity;
	boolean powerAffectsVelocity;

	int tickInterval;
	float ticksPerSecond;
	int specialEffectInterval;
	int spellInterval;

	String particleName;
	int particleCount;
	int renderDistance;
	float particleSpeed;
	float particleXSpread;
	float particleYSpread;
	float particleZSpread;

	int maxEntitiesHit;
	float hitRadius;
	float verticalHitRadius;

	int maxDuration;
	int maxDistanceSquared;

	boolean hugSurface;
	float heightFromSurface;

	boolean hitSelf;
	boolean hitGround;
	boolean hitPlayers;
	boolean hitAirAtEnd;
	boolean hitAirDuring;
	boolean hitNonPlayers;
	boolean hitAirAfterDuration;
	boolean stopOnHitEntity;
	boolean stopOnHitGround;
	ValidTargetList targetList;

	Subspell airSpell;
	Subspell selfSpell;
	Subspell tickSpell;
	Subspell entitySpell;
	Subspell groundSpell;
	Subspell durationSpell;
	String airSpellName;
	String selfSpellName;
	String tickSpellName;
	String entitySpellName;
	String groundSpellName;
	String durationSpellName;

	Subspell defaultSpell;
	String defaultSpellName;

	Random rand;
	ParticleProjectileSpell thisSpell;

	ParticleEffect effect;
	ParticleData data;

	public ParticleProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		this.rand = new Random();
		this.thisSpell = this;

		// Compatibility with start-forward-offset
		float startForwardOffset = getConfigFloat("start-forward-offset", 1F);
		this.startXOffset = getConfigFloat("start-x-offset", 1F);
		if (startForwardOffset != 1F) this.startXOffset = startForwardOffset;
		this.startYOffset = getConfigFloat("start-y-offset", 1F);
		this.startZOffset = getConfigFloat("start-z-offset", 0F);

		// If relative-offset contains different values than the offsets above, override them
		this.relativeOffset = getConfigVector("relative-offset", "1,1,0");
		if (this.relativeOffset.getX() != 1F) this.startXOffset = (float) this.relativeOffset.getX();
		if (this.relativeOffset.getY() != 1F) this.startYOffset = (float) this.relativeOffset.getY();
		if (this.relativeOffset.getZ() != 0F) this.startZOffset = (float) this.relativeOffset.getZ();

		this.projectileVelocity = getConfigFloat("projectile-velocity", 10F);
		this.projectileVertOffset = getConfigFloat("projectile-vert-offset", 0F);
		this.projectileHorizOffset = getConfigFloat("projectile-horiz-offset", 0F);
		float projectileGravity = getConfigFloat("projectile-gravity", 0.25F);
		this.projectileVertGravity = getConfigFloat("projectile-vert-gravity", projectileGravity);
		this.projectileHorizGravity = getConfigFloat("projectile-horiz-gravity", 0F);
		this.projectileSpread = getConfigFloat("projectile-spread", 0F);
		this.powerAffectsVelocity = getConfigBoolean("power-affects-velocity", true);

		this.tickInterval = getConfigInt("tick-interval", 2);
		this.ticksPerSecond = 20F / (float)this.tickInterval;
		this.specialEffectInterval = getConfigInt("special-effect-interval", 0);
		this.spellInterval = getConfigInt("spell-interval", 20);

		this.particleName = getConfigString("particle-name", "reddust");
		this.particleSpeed = getConfigFloat("particle-speed", 0.3F);
		this.particleCount = getConfigInt("particle-count", 15);
		this.particleXSpread = getConfigFloat("particle-horizontal-spread", 0.3F);
		this.particleYSpread = getConfigFloat("particle-vertical-spread", 0.3F);
		this.particleZSpread = this.particleXSpread;
		this.particleXSpread = getConfigFloat("particle-red", this.particleXSpread);
		this.particleYSpread = getConfigFloat("particle-green", this.particleYSpread);
		this.particleZSpread = getConfigFloat("particle-blue", this.particleZSpread);

		this.maxDistanceSquared = getConfigInt("max-distance", 15);
		this.maxDistanceSquared *= this.maxDistanceSquared;
		this.maxDuration = (int)(getConfigInt("max-duration", 0) * TimeUtil.MILLISECONDS_PER_SECOND);
		this.hitRadius = getConfigFloat("hit-radius", 1.5F);
		this.maxEntitiesHit = getConfigInt("max-entities-hit", 0);
		this.verticalHitRadius = getConfigFloat("vertical-hit-radius", this.hitRadius);
		this.renderDistance = getConfigInt("render-distance", 32);

		this.hugSurface = getConfigBoolean("hug-surface", false);
		if (this.hugSurface) this.heightFromSurface = getConfigFloat("height-from-surface", 0.6F);

		this.hitSelf = getConfigBoolean("hit-self", false);
		this.hitGround = getConfigBoolean("hit-ground", true);
		this.hitPlayers = getConfigBoolean("hit-players", false);
		this.hitAirAtEnd = getConfigBoolean("hit-air-at-end", false);
		this.hitAirDuring = getConfigBoolean("hit-air-during", false);
		this.hitNonPlayers = getConfigBoolean("hit-non-players", true);
		this.hitAirAfterDuration = getConfigBoolean("hit-air-after-duration", false);
		this.stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", true);
		this.stopOnHitGround = getConfigBoolean("stop-on-hit-ground", true);

		// Target List
		this.targetList = new ValidTargetList(this, getConfigStringList("can-target", null));
		if (this.hitSelf) this.targetList.enforce(ValidTargetList.TargetingElement.TARGET_SELF, true);
		if (this.hitPlayers) this.targetList.enforce(ValidTargetList.TargetingElement.TARGET_PLAYERS, true);
		if (this.hitNonPlayers) this.targetList.enforce(ValidTargetList.TargetingElement.TARGET_NONPLAYERS, true);

		// Compatibility
		this.defaultSpellName = getConfigString("spell", "explode");
		this.airSpellName = getConfigString("spell-on-hit-air", defaultSpellName);
		this.selfSpellName = getConfigString("spell-on-hit-self", defaultSpellName);
		this.tickSpellName = getConfigString("spell-on-tick", defaultSpellName);
		this.groundSpellName = getConfigString("spell-on-hit-ground", defaultSpellName);
		this.entitySpellName = getConfigString("spell-on-hit-entity", defaultSpellName);
		this.durationSpellName = getConfigString("spell-on-duration-end", defaultSpellName);

		EffectPackage pkg = ParticleNameUtil.findEffectPackage(this.particleName);
		this.effect = pkg.effect;
		this.data = pkg.data;
	}

	@Override
	public void initialize() {
		super.initialize();

		this.defaultSpell = new Subspell(this.defaultSpellName);
		if (!this.defaultSpell.process()) {
			MagicSpells.error("ParticleProjectileSpell '" + this.internalName + "' has an invalid spell defined!");
			this.defaultSpell = null;
		}

		this.airSpell = new Subspell(this.airSpellName);
		if (!this.airSpell.process()) {
			if (!this.airSpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + this.internalName + "' has an invalid spell-on-hit-air defined!");
			this.airSpell = null;
		}

		this.selfSpell = new Subspell(this.selfSpellName);
		if (!this.selfSpell.process()) {
			if (!this.selfSpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + this.internalName + "' has an invalid spell-on-hit-self defined!");
			this.selfSpell = null;
		}

		this.tickSpell = new Subspell(this.tickSpellName);
		if (!this.tickSpell.process()) {
			if (!this.tickSpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + this.internalName + "' has an invalid spell-on-tick defined!");
			this.tickSpell = null;
		}

		this.groundSpell = new Subspell(this.groundSpellName);
		if (!this.groundSpell.process()) {
			if (!this.groundSpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + this.internalName + "' has an invalid spell-on-hit-ground defined!");
			this.groundSpell = null;
		}

		this.entitySpell = new Subspell(this.entitySpellName);
		if (!this.entitySpell.process()) {
			if (!this.entitySpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + this.internalName + "' has an invalid spell-on-hit-entity defined!");
			this.entitySpell = null;
		}

		this.durationSpell = new Subspell(this.durationSpellName);
		if (!this.durationSpell.process()) {
			if (!this.durationSpellName.equals(defaultSpellName)) MagicSpells.error("ParticleProjectileSpell '" + this.internalName + "' has an invalid spell-on-duration-end defined!");
			this.durationSpell = null;
		}

	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			new ProjectileTracker(player, player.getLocation(), power);
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	// TODO move to a separate Java file and use getters for field access
	class ProjectileTracker implements Runnable {

		Player caster;
		float power;
		long startTime;
		Location startLocation;
		Location previousLocation;
		Location currentLocation;
		Vector currentVelocity;
		Vector startDirection;
		int currentX;
		int currentZ;
		int taskId;
		BoundingBox hitBox;
		List<LivingEntity> inRange;
		List<LivingEntity> maxHitLimit;
		Map<LivingEntity, Long> immune;
		ValidTargetChecker entitySpellChecker;

		int counter = 0;

		public ProjectileTracker(Player caster, Location from, float power) {
			this.caster = caster;
			this.power = power;
			this.startTime = System.currentTimeMillis();
			this.startLocation = from.clone();
			// Changing the start location
			this.startDirection = caster.getLocation().getDirection().normalize();
			Vector horizOffset = new Vector(-startDirection.getZ(), 0.0, startDirection.getX()).normalize();
			this.startLocation.add(horizOffset.multiply(startZOffset)).getBlock().getLocation();
			this.startLocation.add(this.startLocation.getDirection().multiply(startXOffset));
			this.startLocation.setY(this.startLocation.getY() + startYOffset);

			this.previousLocation = this.startLocation.clone();
			this.currentLocation = this.startLocation.clone();
			this.currentVelocity = from.getDirection();

			if (projectileHorizOffset != 0) Util.rotateVector(this.currentVelocity, projectileHorizOffset);
			if (projectileVertOffset != 0) this.currentVelocity.add(new Vector(0, projectileVertOffset, 0)).normalize();
			if (projectileSpread > 0) this.currentVelocity.add(new Vector(rand.nextFloat() * projectileSpread, rand.nextFloat() * projectileSpread, rand.nextFloat() * projectileSpread));
			if (hugSurface) {
				this.currentLocation.setY(this.currentLocation.getY() + heightFromSurface);
				this.currentVelocity.setY(0).normalize();
				// Fix for effectlib effects
				this.currentLocation.setPitch(0);
			}
			if (powerAffectsVelocity) this.currentVelocity.multiply(power);
			this.currentVelocity.multiply(projectileVelocity / ticksPerSecond);
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
			if (targetList.canTargetPlayers() || targetList.canTargetLivingEntities()) {
				this.inRange = this.currentLocation.getWorld().getLivingEntities();
				this.inRange.removeIf(e -> !targetList.canTarget(caster, e));
			}
			this.immune = new HashMap<>();
			this.maxHitLimit = new ArrayList<>();
			this.hitBox = new BoundingBox(this.currentLocation, hitRadius, verticalHitRadius);
			// Rotate effectlib effects
			this.currentLocation.setDirection(currentVelocity);
		}

		@Override
		public void run() {
			if (this.caster != null && !this.caster.isValid()) {
				stop();
				return;
			}

			// Check if duration is up
			if (maxDuration > 0 && this.startTime + maxDuration < System.currentTimeMillis()) {
				if (hitAirAfterDuration && durationSpell != null && durationSpell.isTargetedLocationSpell()) {
					durationSpell.castAtLocation(this.caster, this.currentLocation, this.power);
					playSpellEffects(EffectPosition.TARGET, this.currentLocation);
				}
				stop();
				return;
			}

			// Move projectile and apply gravity
			previousLocation = this.currentLocation.clone();
			this.currentLocation.add(currentVelocity);
			if (hugSurface && (this.currentLocation.getBlockX() != this.currentX || this.currentLocation.getBlockZ() != this.currentZ)) {
				Block b = this.currentLocation.subtract(0, heightFromSurface, 0).getBlock();

				int attempts = 0;
				boolean ok = false;
				while (attempts++ < 10) {
					if (BlockUtils.isPathable(b)) {
						b = b.getRelative(BlockFace.DOWN);
						if (BlockUtils.isPathable(b)) {
							this.currentLocation.add(0, -1, 0);
						} else {
							ok = true;
							break;
						}
					} else {
						b = b.getRelative(BlockFace.UP);
						this.currentLocation.add(0, 1, 0);
						if (BlockUtils.isPathable(b)) {
							ok = true;
							break;
						}
					}
				}
				if (!ok) {
					stop();
					return;
				}

				this.currentLocation.setY((int)this.currentLocation.getY() + heightFromSurface);
				this.currentX = this.currentLocation.getBlockX();
				this.currentZ = this.currentLocation.getBlockZ();

				// Apply vertical gravity
			} else if (projectileVertGravity != 0) this.currentVelocity.setY(this.currentVelocity.getY() - (projectileVertGravity / ticksPerSecond));

			// Apply horizontal gravity
			if (projectileHorizGravity != 0) Util.rotateVector(currentVelocity, (projectileHorizGravity / ticksPerSecond) * counter);

			// Rotate effects properly
			if (projectileHorizGravity != 0 || projectileVertGravity != 0) this.currentLocation.setDirection(currentVelocity);

			// Show particle
			effect.display(data, this.currentLocation, null, renderDistance, particleXSpread, particleYSpread, particleZSpread, particleSpeed, particleCount);

			// Play effects
			if (specialEffectInterval > 0 && this.counter % specialEffectInterval == 0) playSpellEffects(EffectPosition.SPECIAL, this.currentLocation);

			counter++;

			// Cast spell mid air
			if (hitAirDuring && this.counter % spellInterval == 0 && tickSpell != null && tickSpell.isTargetedLocationSpell()) {
				tickSpell.castAtLocation(this.caster, this.currentLocation.clone(), this.power);
			}

			// The projectile can now cast spell-on-hit-ground without having to stop
			if (!BlockUtils.isPathable(this.currentLocation.getBlock())) {
				if (hitGround && groundSpell != null && groundSpell.isTargetedLocationSpell()) {
					Util.setLocationFacingFromVector(this.previousLocation, this.currentVelocity);
					groundSpell.castAtLocation(this.caster, this.previousLocation, this.power);
					playSpellEffects(EffectPosition.TARGET, this.currentLocation);
				}
				if (stopOnHitGround) stop();
			} else if (this.currentLocation.distanceSquared(startLocation) >= maxDistanceSquared) {
				if (hitAirAtEnd && airSpell != null && airSpell.isTargetedLocationSpell()) {
					airSpell.castAtLocation(this.caster, this.currentLocation.clone(), this.power);
					playSpellEffects(EffectPosition.TARGET, this.currentLocation);
				}
				stop();
			} else if (this.inRange != null) {
				this.hitBox.setCenter(this.currentLocation);
				for (int i = 0; i < this.inRange.size(); i++) {
					LivingEntity e = this.inRange.get(i);
					if (e.isDead()) continue;
					if (!hitBox.contains(e.getLocation().add(0, 0.6, 0))) continue;
					if (entitySpell != null && entitySpell.isTargetedEntitySpell()) {
						entitySpellChecker = entitySpell.getSpell().getValidTargetChecker();
						if (entitySpellChecker != null && !entitySpellChecker.isValidTarget(e)) {
							this.inRange.remove(i);
							break;
						}
						SpellTargetEvent event = new SpellTargetEvent(thisSpell, this.caster, e, this.power);
						EventUtil.call(event);
						if (event.isCancelled()) {
							this.inRange.remove(i);
							break;
						} else {
							e = event.getTarget();
							this.power = event.getPower();
						}
						entitySpell.castAtEntity(this.caster, e, this.power);
						playSpellEffects(EffectPosition.TARGET, e);
					} else if (entitySpell != null && entitySpell.isTargetedLocationSpell()) {
						entitySpell.castAtLocation(this.caster, this.currentLocation.clone(), this.power);
						playSpellEffects(EffectPosition.TARGET, this.currentLocation);
					}
					if (stopOnHitEntity) {
						stop();
					} else {
						this.inRange.remove(i);
						this.maxHitLimit.add(e);
						this.immune.put(e, System.currentTimeMillis());
						if (this.maxHitLimit.size() >= maxEntitiesHit) stop();
					}
					break;
				}

				if (this.immune == null || this.immune.isEmpty()) return;
				Iterator<Map.Entry<LivingEntity, Long>> iter = this.immune.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<LivingEntity, Long> entry = iter.next();
					if (entry.getValue() < System.currentTimeMillis() - (2 * TimeUtil.MILLISECONDS_PER_SECOND)) {
						iter.remove();
						this.inRange.add(entry.getKey());
					}
				}
			}
		}

		public void stop() {
			playSpellEffects(EffectPosition.DELAYED, this.currentLocation);
			MagicSpells.cancelTask(taskId);
			this.caster = null;
			this.startLocation = null;
			this.previousLocation = null;
			this.currentLocation = null;
			this.currentVelocity = null;
			this.maxHitLimit.clear();
			this.maxHitLimit = null;
			this.immune.clear();
			this.immune = null;
			if (this.inRange == null) return;
			this.inRange.clear();
			this.inRange = null;
		}

	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		Location loc = target.clone();
		loc.setDirection(caster.getLocation().getDirection());
		new ProjectileTracker(caster, target, power);
		playSpellEffects(EffectPosition.CASTER, caster);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		new ProjectileTracker(null, target, power);
		playSpellEffects(EffectPosition.CASTER, target);
		return true;
	}

}
