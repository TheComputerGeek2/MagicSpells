package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.nisovin.magicspells.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;

public class FireballSpell extends TargetedSpell implements TargetedEntityFromLocationSpell {
	
	private boolean requireEntityTarget;
	private boolean checkPlugins;
	private float damageMultiplier;
	private float explosionSize;
	private boolean smallFireball;
	private boolean noExplosion;
	private boolean noExplosionEffect;
	private int noExplosionDamage;
	private int noExplosionDamageRange;
	private boolean noFire;
	
	HashMap<Fireball,Float> fireballs;
	private int taskId;
	private Vector relativeCastLocationOffset;
	boolean useRelativeCastLocationOffset = false;
	private Vector absoluteCastLocationOffset;
	boolean useAbsoluteCastLocationOffset = false;
	boolean doOffsetTargetingCorrections = true;
	boolean fireballGravity;
	
	public FireballSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		damageMultiplier = getConfigFloat("damage-multiplier", 0);
		explosionSize = getConfigFloat("explosion-size", 0);
		smallFireball = getConfigBoolean("small-fireball", false);
		noExplosion = config.getBoolean("spells." + spellName + ".no-explosion", false);
		noExplosionEffect = getConfigBoolean("no-explosion-effect", true);
		noExplosionDamage = getConfigInt("no-explosion-damage", 5);
		noExplosionDamageRange = getConfigInt("no-explosion-damage-range", 3);
		noFire = getConfigBoolean("no-fire", false);
		relativeCastLocationOffset = getConfigVector("relative-cast-position-offset", "0,0,0");
		useRelativeCastLocationOffset = getConfigBoolean("use-relative-cast-location-offset", false);
		absoluteCastLocationOffset = getConfigVector("absolute-cast-position-offset", "0,0,0");
		useAbsoluteCastLocationOffset = getConfigBoolean("use-absolute-cast-location-offset", false);
		doOffsetTargetingCorrections = getConfigBoolean("do-offset-targeting-corrections", true);
		fireballGravity = getConfigBoolean("gravity", false);
		
		fireballs = new HashMap<>();
		
		taskId = MagicSpells.scheduleRepeatingTask(new Runnable() {
			
			@Override
			public void run() {
				Iterator<Map.Entry<Fireball, Float>> iter = fireballs.entrySet().iterator();
				while (iter.hasNext()) {
					if (iter.next().getKey().isDead()) {
						iter.remove();
					}
				}
			}
			
		}, TimeUtil.TICKS_PER_MINUTE, TimeUtil.TICKS_PER_MINUTE);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// Get a target if required
			Location targetLoc = null;
			boolean selfTarget = false;
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
				if (targetInfo == null) return noTarget(player);
				
				LivingEntity entity = targetInfo.getTarget();
				power = targetInfo.getPower();
				if (entity == null) return noTarget(player);
				if (entity instanceof Player && checkPlugins) {
					// Run a pvp damage check
					MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(player, entity, DamageCause.ENTITY_ATTACK, 1D);
					EventUtil.call(event);
					if (event.isCancelled()) return noTarget(player);
				}
				targetLoc = entity.getLocation();
				if (entity.equals(player)) selfTarget = true;
			}
			
			// Create fireball
			Location loc;
			if (!selfTarget) {
				Location playerLocation = player.getLocation();
				loc = player.getEyeLocation().toVector().add(playerLocation.getDirection().multiply(2)).toLocation(player.getWorld(), playerLocation.getYaw(), playerLocation.getPitch());
				loc = offsetLocation(loc);
				loc = applyOffsetTargetingCorrection(loc, targetLoc);
			} else {
				loc = player.getLocation().toVector().add(player.getLocation().getDirection().setY(0).multiply(2)).toLocation(player.getWorld(), player.getLocation().getYaw() + 180, 0);
			}
			Fireball fireball;
			if (smallFireball) {
				//fireball = player.getWorld().spawn(loc, SmallFireball.class);
				fireball = MagicSpells.getVolatileCodeHandler().shootSmallFireball(player);
				player.getWorld().playEffect(player.getLocation(), Effect.GHAST_SHOOT, 0);
			} else {
				fireball = player.getWorld().spawn(loc, Fireball.class);
				player.getWorld().playEffect(player.getLocation(), Effect.GHAST_SHOOT, 0);
				fireballs.put(fireball, power);
			}
			MagicSpells.getVolatileCodeHandler().setGravity(fireball, fireballGravity);
			fireball.setShooter(player);
			
			playSpellEffects(EffectPosition.CASTER, player);
			playSpellEffects(EffectPosition.PROJECTILE, fireball);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private Location applyOffsetTargetingCorrection(Location origin, Location target) {
		if (doOffsetTargetingCorrections && target != null) return Util.faceTarget(origin, target);
		return origin;
	}
	
	@Override
	public boolean castAtEntityFromLocation(Player caster, Location from, LivingEntity target, float power) {
		from = offsetLocation(from);
		Vector facing = target.getLocation().toVector().subtract(from.toVector()).normalize();
		Location loc = from.clone();
		Util.setLocationFacingFromVector(loc, facing);
		loc.add(facing.multiply(2));
		
		Fireball fireball = from.getWorld().spawn(loc, Fireball.class);
		MagicSpells.getVolatileCodeHandler().setGravity(fireball, fireballGravity);
		if (caster != null) fireball.setShooter(caster);
		fireballs.put(fireball, power);
		
		if (caster != null) {
			playSpellEffects(EffectPosition.CASTER, caster);
		} else {
			playSpellEffects(EffectPosition.CASTER, from);
		}
		playSpellEffects(EffectPosition.PROJECTILE, fireball);
		playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, from, fireball.getLocation(), caster, fireball);
		
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(null, from, target, power);
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		if (event.isCancelled()) return;
		
		Entity entityRaw = event.getEntity();
		if (!(entityRaw instanceof Fireball)) return;
		final Fireball fireball = (Fireball)entityRaw;
		if (!fireballs.containsKey(fireball)) return;
		
		playSpellEffects(EffectPosition.TARGET, fireball.getLocation());
		
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
					((LivingEntity)entity).damage(Math.round(noExplosionDamage * power), (LivingEntity)fireball.getShooter());
				}
			}
			if (!noFire) {
				final HashSet<Block> fires = new HashSet<>();
				for (int x = loc.getBlockX() - 1; x <= loc.getBlockX() + 1; x++) {
					for (int y = loc.getBlockY() - 1; y <= loc.getBlockY() + 1; y++) {
						for (int z = loc.getBlockZ() - 1; z <= loc.getBlockZ() + 1; z++) {
							if (loc.getWorld().getBlockAt(x, y, z).getType() == Material.AIR) {
								Block b = loc.getWorld().getBlockAt(x, y, z);
								BlockUtils.setTypeAndData(b, Material.FIRE, false);
								fires.add(b);
							}
						}
					}						
				}
				fireball.remove();
				if (!fires.isEmpty()) {
					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, () -> fires.stream().filter(b -> b.getType() == Material.FIRE).forEachOrdered(b -> b.setType(Material.AIR)), TimeUtil.TICKS_PER_SECOND);
				}
			}
		} else {
			if (noFire) {
				event.setFire(false);
			} else {
				event.setFire(true);
			}
			if (explosionSize > 0) event.setRadius(explosionSize);
		}
		if (noExplosion) {
			// Remove immediately
			fireballs.remove(fireball);
		} else {
			// Schedule removal (gotta wait for damage events)
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, () -> fireballs.remove(fireball), 1);
		}
	}

	@EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
	public void onEntityDamage(EntityDamageEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity)) return;
		if (!(event instanceof EntityDamageByEntityEvent)) return;
		
		EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent)event;
		if (event.getCause() != DamageCause.ENTITY_EXPLOSION && event.getCause() != DamageCause.PROJECTILE) return;
		Entity damager = evt.getDamager();
		if (!(damager instanceof Fireball || damager instanceof SmallFireball)) return;
		Fireball fireball = (Fireball)damager;
		ProjectileSource shooter = fireball.getShooter();
		if (!(shooter instanceof Player)) return;
		if (!fireballs.containsKey(fireball)) return;
		
		float power = fireballs.get(fireball);
		if (!validTargetList.canTarget((Player)shooter, event.getEntity())) {
			event.setCancelled(true);
		} else if (damageMultiplier > 0) {
			event.setDamage(Math.round(event.getDamage() * damageMultiplier * power));
		}
	}
	
	@Override
	public void turnOff() {
		MagicSpells.cancelTask(taskId);
	}
	
	private Location offsetLocation(Location loc) {
		Location ret = loc;
		if (useRelativeCastLocationOffset) ret = Util.applyRelativeOffset(ret, relativeCastLocationOffset);
		if (useAbsoluteCastLocationOffset) ret = Util.applyAbsoluteOffset(ret, absoluteCastLocationOffset);
		return ret;
	}
	
}
