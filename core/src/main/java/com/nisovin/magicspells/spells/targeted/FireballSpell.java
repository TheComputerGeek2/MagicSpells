package com.nisovin.magicspells.spells.targeted;

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

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class FireballSpell extends TargetedSpell implements TargetedEntityFromLocationSpell {

	private Map<Fireball, SpellData> fireballs;

	private int taskId;

	private ConfigData<Float> explosionSize;

	private ConfigData<Double> damageMultiplier;
	private ConfigData<Double> noExplosionDamage;
	private ConfigData<Double> noExplosionDamageRange;

	private boolean noFire;
	private boolean noExplosion;
	private boolean checkPlugins;
	private boolean smallFireball;
	private boolean fireballGravity;
	private boolean noExplosionEffect;
	private boolean requireEntityTarget;
	private boolean doOffsetTargetingCorrections;
	private boolean powerAffectsDamageMultiplier;
	private boolean powerAffectsNoExplosionDamage;
	private boolean useRelativeCastLocationOffset;
	private boolean useAbsoluteCastLocationOffset;

	private Vector relativeCastLocationOffset;
	private Vector absoluteCastLocationOffset;

	public FireballSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		fireballs = new HashMap<>();

		explosionSize = getConfigDataFloat("explosion-size", 0);
		damageMultiplier = getConfigDataDouble("damage-multiplier", 0);

		noExplosionDamage = getConfigDataDouble("no-explosion-damage", 5);
		noExplosionDamageRange = getConfigDataDouble("no-explosion-damage-range", 3);

		noFire = getConfigBoolean("no-fire", true);
		noExplosion = getConfigBoolean("no-explosion", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		smallFireball = getConfigBoolean("small-fireball", false);
		fireballGravity = getConfigBoolean("gravity", false);
		noExplosionEffect = getConfigBoolean("no-explosion-effect", true);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		doOffsetTargetingCorrections = getConfigBoolean("do-offset-targeting-corrections", true);
		powerAffectsDamageMultiplier = getConfigBoolean("power-affects-damage-multiplier", true);
		powerAffectsNoExplosionDamage = getConfigBoolean("power-affects-no-explosion-damage", true);
		useRelativeCastLocationOffset = getConfigBoolean("use-relative-cast-location-offset", false);
		useAbsoluteCastLocationOffset = getConfigBoolean("use-absolute-cast-location-offset", false);

		relativeCastLocationOffset = getConfigVector("relative-cast-position-offset", "0,0,0");
		absoluteCastLocationOffset = getConfigVector("absolute-cast-position-offset", "0,0,0");

		taskId = MagicSpells.scheduleRepeatingTask(() -> fireballs.entrySet().removeIf(fireballFloatEntry ->
				fireballFloatEntry.getKey().isDead()), TimeUtil.TICKS_PER_MINUTE, TimeUtil.TICKS_PER_MINUTE);
	}

	@Override
	public void turnOff() {
		MagicSpells.cancelTask(taskId);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location targetLoc = null;
			boolean selfTarget = false;
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power);
				if (targetInfo == null) return noTarget(caster);

				LivingEntity entity = targetInfo.getTarget();
				power = targetInfo.getPower();
				if (entity == null) return noTarget(caster);
				if (checkPlugins) {
					// Run a pvp damage check
					MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(caster, entity, DamageCause.ENTITY_ATTACK, 1D, this);
					EventUtil.call(event);
					if (event.isCancelled()) return noTarget(caster);
				}
				targetLoc = entity.getLocation();
				if (entity.equals(caster)) selfTarget = true;
			}

			// Create fireball
			Location loc;
			Location pLoc = caster.getLocation();
			if (!selfTarget) {
				loc = caster.getEyeLocation().toVector().add(pLoc.getDirection().multiply(2)).toLocation(caster.getWorld(), pLoc.getYaw(), pLoc.getPitch());
				loc = offsetLocation(loc);
				loc = applyOffsetTargetingCorrection(loc, targetLoc);
			} else {
				loc = pLoc.toVector().add(pLoc.getDirection().setY(0).multiply(2)).toLocation(caster.getWorld(), pLoc.getYaw() + 180, 0);
			}
			Fireball fireball;
			if (smallFireball && caster instanceof Player) {
				fireball = caster.launchProjectile(SmallFireball.class);
				caster.getWorld().playEffect(caster.getLocation(), Effect.GHAST_SHOOT, 0);
			} else {
				fireball = caster.getWorld().spawn(loc, Fireball.class);
				caster.getWorld().playEffect(caster.getLocation(), Effect.GHAST_SHOOT, 0);
				fireballs.put(fireball, new SpellData(power, args));
			}

			fireball.setShooter(caster);
			fireball.setGravity(fireballGravity);

			playSpellEffects(EffectPosition.CASTER, caster);
			playSpellEffects(EffectPosition.PROJECTILE, fireball);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, String[] args) {
		from = offsetLocation(from);
		Vector facing = target.getLocation().toVector().subtract(from.toVector()).normalize();
		Location loc = from.clone();
		Util.setLocationFacingFromVector(loc, facing);
		loc.add(facing.multiply(2));

		Fireball fireball = from.getWorld().spawn(loc, Fireball.class);
		fireball.setGravity(fireballGravity);
		if (caster != null) fireball.setShooter(caster);
		fireballs.put(fireball, new SpellData(power, args));

		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster);
		else playSpellEffects(EffectPosition.CASTER, from);

		playSpellEffects(EffectPosition.PROJECTILE, fireball);
		playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, from, fireball.getLocation(), caster, fireball);

		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(caster, from, target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(null, from, target, power, null);
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

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		Entity entityRaw = event.getEntity();
		if (!(entityRaw instanceof Fireball fireball)) return;
		if (!fireballs.containsKey(fireball)) return;

		playSpellEffects(EffectPosition.TARGET, fireball.getLocation());

		LivingEntity caster = fireball.getShooter() instanceof LivingEntity le ? le : null;
		SpellData data = fireballs.get(fireball);

		if (noExplosion) {
			event.setCancelled(true);
			Location loc = fireball.getLocation();
			if (noExplosionEffect) loc.getWorld().createExplosion(loc, 0);


			double noExplosionDamageRange = this.noExplosionDamageRange.get(caster, null, data.power(), data.args());
			List<Entity> inRange = fireball.getNearbyEntities(noExplosionDamageRange, noExplosionDamageRange, noExplosionDamageRange);
			for (Entity entity : inRange) {
				if (!(entity instanceof LivingEntity target)) continue;
				if (!validTargetList.canTarget(entity)) continue;

				double noExplosionDamage = this.noExplosionDamage.get(caster, target, data.power(), data.args());
				if (powerAffectsNoExplosionDamage) noExplosionDamage *= data.power();

				target.damage(noExplosionDamage, caster);
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
					MagicSpells.scheduleDelayedTask(() -> fires.stream().filter(b -> b.getType() == Material.FIRE).forEachOrdered(b -> b.setType(Material.AIR)), TimeUtil.TICKS_PER_SECOND);
				}
			}
		} else {
			event.setFire(!noFire);

			float explosionSize = this.explosionSize.get(caster, null, data.power(), data.args());
			if (explosionSize > 0) event.setRadius(explosionSize);
		}

		if (noExplosion) fireballs.remove(fireball);
		else MagicSpells.scheduleDelayedTask(() -> fireballs.remove(fireball), 1);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity target)) return;
		if (!(event instanceof EntityDamageByEntityEvent evt)) return;
		if (event.getCause() != DamageCause.ENTITY_EXPLOSION && event.getCause() != DamageCause.PROJECTILE) return;

		Entity damager = evt.getDamager();
		if (!(damager instanceof Fireball fireball) || !fireballs.containsKey(fireball)) return;

		ProjectileSource shooter = fireball.getShooter();
		if (!(shooter instanceof LivingEntity caster)) return;

		SpellData data = fireballs.get(fireball);

		if (!validTargetList.canTarget(caster, target)) event.setCancelled(true);
		else {
			double damageMultiplier = this.damageMultiplier.get(caster, target, data.power(), data.args());
			if (damageMultiplier > 0) {
				if (powerAffectsDamageMultiplier) damageMultiplier *= data.power();
				event.setDamage(event.getDamage() * damageMultiplier);
			}
		}
	}

}
