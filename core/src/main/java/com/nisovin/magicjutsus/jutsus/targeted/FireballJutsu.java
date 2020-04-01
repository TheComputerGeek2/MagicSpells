package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Fireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.SmallFireball;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedEntityFromLocationJutsu;
import com.nisovin.magicjutsus.events.MagicJutsusEntityDamageByEntityEvent;

public class FireballJutsu extends TargetedJutsu implements TargetedEntityFromLocationJutsu {

	private Map<Fireball, Float> fireballs;

	private float explosionSize;
	private float damageMultiplier;

	private int taskId;
	private int noExplosionDamage;
	private int noExplosionDamageRange;

	private boolean noFire;
	private boolean noExplosion;
	private boolean checkPlugins;
	private boolean smallFireball;
	private boolean fireballGravity;
	private boolean noExplosionEffect;
	private boolean requireEntityTarget;
	private boolean doOffsetTargetingCorrections;
	private boolean useRelativeCastLocationOffset;
	private boolean useAbsoluteCastLocationOffset;

	private Vector relativeCastLocationOffset;
	private Vector absoluteCastLocationOffset;

	public FireballJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		fireballs = new HashMap<>();

		explosionSize = getConfigFloat("explosion-size", 0);
		damageMultiplier = getConfigFloat("damage-multiplier", 0);

		noExplosionDamage = getConfigInt("no-explosion-damage", 5);
		noExplosionDamageRange = getConfigInt("no-explosion-damage-range", 3);

		noFire = getConfigBoolean("no-fire", true);
		noExplosion = getConfigBoolean("no-explosion", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		smallFireball = getConfigBoolean("small-fireball", false);
		fireballGravity = getConfigBoolean("gravity", false);
		noExplosionEffect = getConfigBoolean("no-explosion-effect", true);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		doOffsetTargetingCorrections = getConfigBoolean("do-offset-targeting-corrections", true);
		useRelativeCastLocationOffset = getConfigBoolean("use-relative-cast-location-offset", false);
		useAbsoluteCastLocationOffset = getConfigBoolean("use-absolute-cast-location-offset", false);

		relativeCastLocationOffset = getConfigVector("relative-cast-position-offset", "0,0,0");
		absoluteCastLocationOffset = getConfigVector("absolute-cast-position-offset", "0,0,0");
		
		taskId = MagicJutsus.scheduleRepeatingTask(() -> {
			fireballs.entrySet().removeIf(fireballFloatEntry -> fireballFloatEntry.getKey().isDead());
		}, TimeUtil.TICKS_PER_MINUTE, TimeUtil.TICKS_PER_MINUTE);
	}

	@Override
	public void turnOff() {
		MagicJutsus.cancelTask(taskId);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Location targetLoc = null;
			boolean selfTarget = false;
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
				if (targetInfo == null) return noTarget(livingEntity);
				
				LivingEntity entity = targetInfo.getTarget();
				power = targetInfo.getPower();
				if (entity == null) return noTarget(livingEntity);
				if (checkPlugins) {
					// Run a pvp damage check
					MagicJutsusEntityDamageByEntityEvent event = new MagicJutsusEntityDamageByEntityEvent(livingEntity, entity, DamageCause.ENTITY_ATTACK, 1D);
					EventUtil.call(event);
					if (event.isCancelled()) return noTarget(livingEntity);
				}
				targetLoc = entity.getLocation();
				if (entity.equals(livingEntity)) selfTarget = true;
			}
			
			// Create fireball
			Location loc;
			Location pLoc = livingEntity.getLocation();
			if (!selfTarget) {
				loc = livingEntity.getEyeLocation().toVector().add(pLoc.getDirection().multiply(2)).toLocation(livingEntity.getWorld(), pLoc.getYaw(), pLoc.getPitch());
				loc = offsetLocation(loc);
				loc = applyOffsetTargetingCorrection(loc, targetLoc);
			} else {
				loc = pLoc.toVector().add(pLoc.getDirection().setY(0).multiply(2)).toLocation(livingEntity.getWorld(), pLoc.getYaw() + 180, 0);
			}
			Fireball fireball;
			if (smallFireball && livingEntity instanceof Player) {
				fireball = livingEntity.launchProjectile(SmallFireball.class);
				livingEntity.getWorld().playEffect(livingEntity.getLocation(), Effect.GHAST_SHOOT, 0);
			} else {
				fireball = livingEntity.getWorld().spawn(loc, Fireball.class);
				livingEntity.getWorld().playEffect(livingEntity.getLocation(), Effect.GHAST_SHOOT, 0);
				fireballs.put(fireball, power);
			}

			fireball.setShooter(livingEntity);
			fireball.setGravity(fireballGravity);

			playJutsuEffects(EffectPosition.CASTER, livingEntity);
			playJutsuEffects(EffectPosition.PROJECTILE, fireball);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		from = offsetLocation(from);
		Vector facing = target.getLocation().toVector().subtract(from.toVector()).normalize();
		Location loc = from.clone();
		Util.setLocationFacingFromVector(loc, facing);
		loc.add(facing.multiply(2));
		
		Fireball fireball = from.getWorld().spawn(loc, Fireball.class);
		fireball.setGravity(fireballGravity);
		if (caster != null) fireball.setShooter(caster);
		fireballs.put(fireball, power);
		
		if (caster != null) playJutsuEffects(EffectPosition.CASTER, caster);
		else playJutsuEffects(EffectPosition.CASTER, from);

		playJutsuEffects(EffectPosition.PROJECTILE, fireball);
		playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, from, fireball.getLocation(), caster, fireball);
		
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(null, from, target, power);
	}

	private Location offsetLocation(Location loc) {
		Location ret = loc;
		if (useRelativeCastLocationOffset) ret = Util.applyRelativeOffset(ret, relativeCastLocationOffset);
		if (useAbsoluteCastLocationOffset) ret = Util.applyAbsoluteOffset(ret, absoluteCastLocationOffset);
		return ret;
	}

	private Location applyOffsetTargetingCorrection(Location origin, Location target) {
		if (doOffsetTargetingCorrections && target != null) return Util.faceTarget(origin, target);
		return origin;
	}
	
	@EventHandler(priority=EventPriority.HIGH, ignoreCancelled = true)
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		Entity entityRaw = event.getEntity();
		if (!(entityRaw instanceof Fireball)) return;
		final Fireball fireball = (Fireball) entityRaw;
		if (!fireballs.containsKey(fireball)) return;
		
		playJutsuEffects(EffectPosition.TARGET, fireball.getLocation());
		
		if (noExplosion) {
			event.setCancelled(true);
			Location loc = fireball.getLocation();
			if (noExplosionEffect) loc.getWorld().createExplosion(loc, 0);
			if (noExplosionDamage > 0) {
				float power = fireballs.get(fireball);
				List<Entity> inRange = fireball.getNearbyEntities(noExplosionDamageRange, noExplosionDamageRange, noExplosionDamageRange);
				for (Entity entity : inRange) {
					if (!(entity instanceof LivingEntity)) continue;
					if (!validTargetList.canTarget(entity)) continue;
					((LivingEntity) entity).damage(Math.round(noExplosionDamage * power), (LivingEntity) fireball.getShooter());
				}
			}

			if (!noFire) {
				Set<Block> fires = new HashSet<>();
				for (int x = loc.getBlockX() - 1; x <= loc.getBlockX() + 1; x++) {
					for (int y = loc.getBlockY() - 1; y <= loc.getBlockY() + 1; y++) {
						for (int z = loc.getBlockZ() - 1; z <= loc.getBlockZ() + 1; z++) {
							if (!BlockUtils.isAir(loc.getWorld().getBlockAt(x, y, z).getType())) continue;
							Block b = loc.getWorld().getBlockAt(x, y, z);
							BlockUtils.setTypeAndData(b, Material.FIRE, Material.FIRE.createBlockData(), false);
							fires.add(b);
						}
					}						
				}
				fireball.remove();
				if (!fires.isEmpty()) {
					MagicJutsus.scheduleDelayedTask(() -> fires.stream().filter(b -> b.getType() == Material.FIRE).forEachOrdered(b -> b.setType(Material.AIR)), TimeUtil.TICKS_PER_SECOND);
				}
			}
		} else {
			if (noFire) event.setFire(false);
			else event.setFire(true);
			if (explosionSize > 0) event.setRadius(explosionSize);
		}

		if (noExplosion) fireballs.remove(fireball);
		else MagicJutsus.scheduleDelayedTask(() -> fireballs.remove(fireball), 1);
	}

	@EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
	public void onEntityDamage(EntityDamageEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity)) return;
		if (!(event instanceof EntityDamageByEntityEvent)) return;
		
		EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent) event;
		if (event.getCause() != DamageCause.ENTITY_EXPLOSION && event.getCause() != DamageCause.PROJECTILE) return;
		Entity damager = evt.getDamager();
		if (!(damager instanceof Fireball || damager instanceof SmallFireball)) return;
		Fireball fireball = (Fireball) damager;
		ProjectileSource shooter = fireball.getShooter();
		if (!(shooter instanceof Player)) return;
		if (!fireballs.containsKey(fireball)) return;
		
		float power = fireballs.get(fireball);

		if (!validTargetList.canTarget((Player) shooter, entity)) event.setCancelled(true);
		else if (damageMultiplier > 0) event.setDamage(Math.round(event.getDamage() * damageMultiplier * power));
	}
	
}
