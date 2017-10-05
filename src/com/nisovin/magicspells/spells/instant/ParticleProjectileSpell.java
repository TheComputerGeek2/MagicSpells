package com.nisovin.magicspells.spells.instant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.nisovin.magicspells.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.EffectPackage;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.ParticleNameUtil;
import com.nisovin.magicspells.util.SpellTypesAllowed;
import com.nisovin.magicspells.util.Util;

import de.slikey.effectlib.util.ParticleEffect;
import de.slikey.effectlib.util.ParticleEffect.ParticleData;

public class ParticleProjectileSpell extends InstantSpell implements TargetedLocationSpell {

	float startYOffset;
	float startForwardOffset;
	
	float projectileVelocity;
	float projectileVelocityVertOffset;
	float projectileVelocityHorizOffset;
	float projectileGravity;
	float projectileSpread;
	boolean powerAffectsVelocity;
	
	int tickInterval;
	float ticksPerSecond;
	int specialEffectInterval;
	int spellInterval;
	
	String particleName;
	float particleSpeed;
	int particleCount;
	float particleXSpread;
	float particleYSpread;
	float particleZSpread;
	
	int maxDistanceSquared;
	int maxDuration;
	float hitRadius;
	float verticalHitRadius;
	int renderDistance;
	
	boolean hugSurface;
	float heightFromSurface;
	
	boolean hitPlayers;
	boolean hitNonPlayers;
	boolean hitSelf;
	boolean hitGround;
	boolean hitAirAtEnd;
	boolean hitAirAfterDuration;
	boolean hitAirDuring;
	boolean stopOnHitEntity;
	boolean stopOnHitGround;
	
	@SpellTypesAllowed
	String landSpellName;
	Subspell spell;
	
	ParticleProjectileSpell thisSpell;
	Random rand = new Random();
	
	ParticleEffect effect;
	ParticleData data;

	public ParticleProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		this.thisSpell = this;
		
		this.startYOffset = getConfigFloat("start-y-offset", 1F);
		this.startForwardOffset = getConfigFloat("start-forward-offset", 1F);
		
		this.projectileVelocity = getConfigFloat("projectile-velocity", 10F);
		this.projectileVelocityVertOffset = getConfigFloat("projectile-vert-offset", 0F);
		this.projectileVelocityHorizOffset = getConfigFloat("projectile-horiz-offset", 0F);
		this.projectileGravity = getConfigFloat("projectile-gravity", 0.25F);
		this.projectileSpread = getConfigFloat("projectile-spread", 0F);
		this.powerAffectsVelocity = getConfigBoolean("power-affects-velocity", true);
		
		this.tickInterval = getConfigInt("tick-interval", 2);
		this.ticksPerSecond = 20F / (float)this.tickInterval;
		this.specialEffectInterval = getConfigInt("special-effect-interval", 1);
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
		this.verticalHitRadius = getConfigFloat("vertical-hit-radius", this.hitRadius);
		this.renderDistance = getConfigInt("render-distance", 32);
		
		this.hugSurface = getConfigBoolean("hug-surface", false);
		if (this.hugSurface) {
			this.heightFromSurface = getConfigFloat("height-from-surface", .6F);
		} else {
			this.heightFromSurface = 0;
		}
		
		this.hitPlayers = getConfigBoolean("hit-players", false);
		this.hitNonPlayers = getConfigBoolean("hit-non-players", true);
		this.hitSelf = getConfigBoolean("hit-self", false);
		this.hitGround = getConfigBoolean("hit-ground", true);
		this.hitAirAtEnd = getConfigBoolean("hit-air-at-end", false);
		this.hitAirAfterDuration = getConfigBoolean("hit-air-after-duration", false);
		this.hitAirDuring = getConfigBoolean("hit-air-during", false);
		this.stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", true);
		this.stopOnHitGround = getConfigBoolean("stop-on-hit-ground", true);
		
		this.landSpellName = getConfigString("spell", "explode");
		
		EffectPackage pkg = ParticleNameUtil.findEffectPackage(this.particleName);
		this.effect = pkg.effect;
		this.data = pkg.data;
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		Subspell s = new Subspell(this.landSpellName);
		if (s.process()) {
			this.spell = s;
		} else {
			MagicSpells.error("ParticleProjectileSpell " + this.internalName + " has an invalid spell defined!");
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
		int currentX;
		int currentZ;
		int taskId;
		List<LivingEntity> inRange;
		Map<LivingEntity, Long> immune;
		
		int counter = 0;
		
		public ProjectileTracker(Player caster, Location from, float power) {
			this.caster = caster;
			this.power = power;
			this.startTime = System.currentTimeMillis();
			this.startLocation = from.clone();
			if (ParticleProjectileSpell.this.startYOffset != 0) {
				this.startLocation.setY(this.startLocation.getY() + ParticleProjectileSpell.this.startYOffset);
			}
			if (ParticleProjectileSpell.this.startForwardOffset != 0) {
				// TODO see what can be cleaned up here with constructing locations and vectors
				this.startLocation.add(this.startLocation.getDirection().clone().multiply(ParticleProjectileSpell.this.startForwardOffset));
			}
			this.previousLocation = this.startLocation.clone();
			this.currentLocation = this.startLocation.clone();
			this.currentVelocity = from.getDirection();
			if (ParticleProjectileSpell.this.projectileVelocityHorizOffset != 0) Util.rotateVector(this.currentVelocity, projectileVelocityHorizOffset);
			if (ParticleProjectileSpell.this.projectileVelocityVertOffset != 0) this.currentVelocity.add(new Vector(0, projectileVelocityVertOffset, 0)).normalize();
			if (ParticleProjectileSpell.this.projectileSpread > 0) this.currentVelocity.add(new Vector(rand.nextFloat() * ParticleProjectileSpell.this.projectileSpread, rand.nextFloat() * ParticleProjectileSpell.this.projectileSpread, rand.nextFloat() * ParticleProjectileSpell.this.projectileSpread));
			if (ParticleProjectileSpell.this.hugSurface) {
				this.currentLocation.setY((int)this.currentLocation.getY() + ParticleProjectileSpell.this.heightFromSurface);
				this.currentVelocity.setY(0).normalize();
			}
			if (ParticleProjectileSpell.this.powerAffectsVelocity) this.currentVelocity.multiply(power);
			this.currentVelocity.multiply(ParticleProjectileSpell.this.projectileVelocity / ParticleProjectileSpell.this.ticksPerSecond);
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, ParticleProjectileSpell.this.tickInterval);
			if (ParticleProjectileSpell.this.hitPlayers || ParticleProjectileSpell.this.hitNonPlayers) {
				this.inRange = this.currentLocation.getWorld().getLivingEntities();
				Iterator<LivingEntity> iter = this.inRange.iterator();
				while (iter.hasNext()) {
					LivingEntity e = iter.next();
					if (!ParticleProjectileSpell.this.hitSelf && caster != null && e.equals(caster)) {
						iter.remove();
						continue;
					}
					if (!ParticleProjectileSpell.this.hitPlayers && e instanceof Player) {
						iter.remove();
						continue;
					}
					if (!ParticleProjectileSpell.this.hitNonPlayers && !(e instanceof Player)) {
						iter.remove();
						continue;
					}
				}
			}
			this.immune = new HashMap<>();
		}
		
		@Override
		public void run() {
			if (this.caster != null && !this.caster.isValid()) {
				stop();
				return;
			}
			
			// Check if duration is up
			if (ParticleProjectileSpell.this.maxDuration > 0 && this.startTime + ParticleProjectileSpell.this.maxDuration < System.currentTimeMillis()) {
				if (ParticleProjectileSpell.this.hitAirAfterDuration && ParticleProjectileSpell.this.spell != null && ParticleProjectileSpell.this.spell.isTargetedLocationSpell()) {
					ParticleProjectileSpell.this.spell.castAtLocation(this.caster, this.currentLocation, this.power);
					playSpellEffects(EffectPosition.TARGET, this.currentLocation);
				}
				stop();
				return;
			}
			
			// Move projectile and apply gravity
			previousLocation = this.currentLocation.clone();
			this.currentLocation.add(currentVelocity);
			if (ParticleProjectileSpell.this.hugSurface) {
				if (this.currentLocation.getBlockX() != this.currentX || this.currentLocation.getBlockZ() != this.currentZ) {
					Block b = this.currentLocation.subtract(0, ParticleProjectileSpell.this.heightFromSurface, 0).getBlock();
					if (BlockUtils.isPathable(b)) {
						int attempts = 0;
						boolean ok = false;
						while (attempts++ < 10) {
							b = b.getRelative(BlockFace.DOWN);
							if (BlockUtils.isPathable(b)) {
								this.currentLocation.add(0, -1, 0);
							} else {
								ok = true;
								break;
							}
						}
						if (!ok) {
							stop();
							return;
						}
					} else {
						int attempts = 0;
						boolean ok = false;
						while (attempts++ < 10) {
							b = b.getRelative(BlockFace.UP);
							this.currentLocation.add(0, 1, 0);
							if (BlockUtils.isPathable(b)) {
								ok = true;
								break;
							}
						}
						if (!ok) {
							stop();
							return;
						}
					}
					this.currentLocation.setY((int)this.currentLocation.getY() + ParticleProjectileSpell.this.heightFromSurface);
					this.currentX = this.currentLocation.getBlockX();
					this.currentZ = this.currentLocation.getBlockZ();
				}
			} else if (ParticleProjectileSpell.this.projectileGravity != 0) {
				this.currentVelocity.setY(this.currentVelocity.getY() - (ParticleProjectileSpell.this.projectileGravity / ParticleProjectileSpell.this.ticksPerSecond));
			}
			
			// Show particle
			
			//MagicSpells.getVolatileCodeHandler().playParticleEffect(currentLocation, particleName, particleHorizontalSpread, particleVerticalSpread, particleSpeed, particleCount, renderDistance, 0F);
			ParticleProjectileSpell.this.effect.display(ParticleProjectileSpell.this.data, this.currentLocation, null, ParticleProjectileSpell.this.renderDistance, ParticleProjectileSpell.this.particleXSpread, ParticleProjectileSpell.this.particleYSpread, ParticleProjectileSpell.this.particleZSpread, ParticleProjectileSpell.this.particleSpeed, ParticleProjectileSpell.this.particleCount);
			//ParticleData data, Location center, Color color, double range, float offsetX, float offsetY, float offsetZ, float speed, int amount
			
			//MagicSpells.getVolatileCodeHandler().playParticleEffect(currentLocation, particleName, particleXSpread, particleYSpread, particleZSpread, particleSpeed, particleCount, renderDistance, 0F);
			
			// Play effects
			if (ParticleProjectileSpell.this.specialEffectInterval > 0 && this.counter % ParticleProjectileSpell.this.specialEffectInterval == 0) {
				playSpellEffects(EffectPosition.SPECIAL, this.currentLocation);
			}
			
			counter++;
			
			// Cast spell mid air
			if (ParticleProjectileSpell.this.hitAirDuring && this.counter % ParticleProjectileSpell.this.spellInterval == 0 && ParticleProjectileSpell.this.spell.isTargetedLocationSpell()) {
				ParticleProjectileSpell.this.spell.castAtLocation(this.caster, this.currentLocation.clone(), this.power);
			}
			
			if (ParticleProjectileSpell.this.stopOnHitGround && !BlockUtils.isPathable(this.currentLocation.getBlock())) {
				if (ParticleProjectileSpell.this.hitGround && ParticleProjectileSpell.this.spell != null && ParticleProjectileSpell.this.spell.isTargetedLocationSpell()) {
					Util.setLocationFacingFromVector(this.previousLocation, this.currentVelocity);
					ParticleProjectileSpell.this.spell.castAtLocation(this.caster, this.previousLocation, this.power);
					playSpellEffects(EffectPosition.TARGET, this.currentLocation);
				}
				stop();
			} else if (this.currentLocation.distanceSquared(startLocation) >= maxDistanceSquared) {
				if (hitAirAtEnd && ParticleProjectileSpell.this.spell != null && ParticleProjectileSpell.this.spell.isTargetedLocationSpell()) {
					ParticleProjectileSpell.this.spell.castAtLocation(this.caster, this.currentLocation.clone(), this.power);
					playSpellEffects(EffectPosition.TARGET, this.currentLocation);
				}
				stop();
			} else if (this.inRange != null) {
				BoundingBox hitBox = new BoundingBox(this.currentLocation, ParticleProjectileSpell.this.hitRadius, ParticleProjectileSpell.this.verticalHitRadius);
				for (int i = 0; i < this.inRange.size(); i++) {
					LivingEntity e = this.inRange.get(i);
					if (e.isDead()) continue;
					if (!hitBox.contains(e.getLocation().add(0, 0.6, 0))) continue;
					if (ParticleProjectileSpell.this.spell != null) {
						if (ParticleProjectileSpell.this.spell.isTargetedEntitySpell()) {
							ValidTargetChecker checker = ParticleProjectileSpell.this.spell.getSpell().getValidTargetChecker();
							if (checker != null && !checker.isValidTarget(e)) {
								this.inRange.remove(i);
								break;
							}
							LivingEntity target = e;
							float thisPower = this.power;
							SpellTargetEvent event = new SpellTargetEvent(ParticleProjectileSpell.this.thisSpell, this.caster, target, thisPower);
							EventUtil.call(event);
							if (event.isCancelled()) {
								this.inRange.remove(i);
								break;
							} else {
								target = event.getTarget();
								thisPower = event.getPower();
							}
							ParticleProjectileSpell.this.spell.castAtEntity(this.caster, target, thisPower);
							playSpellEffects(EffectPosition.TARGET, e);
						} else if (ParticleProjectileSpell.this.spell.isTargetedLocationSpell()) {
							ParticleProjectileSpell.this.spell.castAtLocation(this.caster, this.currentLocation.clone(), this.power);
							playSpellEffects(EffectPosition.TARGET, this.currentLocation);
						}
					}
					if (ParticleProjectileSpell.this.stopOnHitEntity) {
						stop();
					} else {
						this.inRange.remove(i);
						this.immune.put(e, System.currentTimeMillis());
					}
					break;
				}
				Iterator<Map.Entry<LivingEntity, Long>> iter = this.immune.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<LivingEntity, Long> entry = iter.next();
					if (entry.getValue().longValue() < System.currentTimeMillis() - (2 * TimeUtil.MILLISECONDS_PER_SECOND)) {
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
