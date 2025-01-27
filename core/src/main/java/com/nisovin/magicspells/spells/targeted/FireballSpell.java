package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
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

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class FireballSpell extends TargetedSpell implements TargetedEntityFromLocationSpell {

	private final Map<Fireball, SpellData> fireballs;

	private final ScheduledTask task;

	private final ConfigData<Float> explosionSize;

	private final ConfigData<Double> damageMultiplier;
	private final ConfigData<Double> noExplosionDamage;
	private final ConfigData<Double> noExplosionDamageRange;

	private final ConfigData<Boolean> noFire;
	private final ConfigData<Boolean> noExplosion;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> smallFireball;
	private final ConfigData<Boolean> fireballGravity;
	private final ConfigData<Boolean> noExplosionEffect;
	private final ConfigData<Boolean> requireEntityTarget;
	private final ConfigData<Boolean> doOffsetTargetingCorrections;
	private final ConfigData<Boolean> powerAffectsDamageMultiplier;
	private final ConfigData<Boolean> powerAffectsNoExplosionDamage;
	private final ConfigData<Boolean> useRelativeCastLocationOffset;
	private final ConfigData<Boolean> useAbsoluteCastLocationOffset;

	private final ConfigData<Vector> relativeCastLocationOffset;
	private final ConfigData<Vector> absoluteCastLocationOffset;

	public FireballSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		fireballs = new HashMap<>();

		explosionSize = getConfigDataFloat("explosion-size", 0);
		damageMultiplier = getConfigDataDouble("damage-multiplier", 0);

		noExplosionDamage = getConfigDataDouble("no-explosion-damage", 5);
		noExplosionDamageRange = getConfigDataDouble("no-explosion-damage-range", 3);

		noFire = getConfigDataBoolean("no-fire", true);
		noExplosion = getConfigDataBoolean("no-explosion", true);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		smallFireball = getConfigDataBoolean("small-fireball", false);
		fireballGravity = getConfigDataBoolean("gravity", false);
		noExplosionEffect = getConfigDataBoolean("no-explosion-effect", true);
		requireEntityTarget = getConfigDataBoolean("require-entity-target", false);
		doOffsetTargetingCorrections = getConfigDataBoolean("do-offset-targeting-corrections", true);
		powerAffectsDamageMultiplier = getConfigDataBoolean("power-affects-damage-multiplier", true);
		powerAffectsNoExplosionDamage = getConfigDataBoolean("power-affects-no-explosion-damage", true);
		useRelativeCastLocationOffset = getConfigDataBoolean("use-relative-cast-location-offset", false);
		useAbsoluteCastLocationOffset = getConfigDataBoolean("use-absolute-cast-location-offset", false);

		relativeCastLocationOffset = getConfigDataVector("relative-cast-position-offset", new Vector());
		absoluteCastLocationOffset = getConfigDataVector("absolute-cast-position-offset", new Vector());

		task = MagicSpells.scheduleRepeatingTask(() -> fireballs.entrySet().removeIf(fireballFloatEntry ->
			fireballFloatEntry.getKey().isDead()), TimeUtil.TICKS_PER_MINUTE, TimeUtil.TICKS_PER_MINUTE);
	}

	@Override
	public void turnOff() {
		MagicSpells.cancelTask(task);
	}

	@Override
	public CastResult cast(SpellData data) {
		if (requireEntityTarget.get(data)) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();

			if (checkPlugins.get(data) && checkFakeDamageEvent(data.caster(), data.target(), DamageCause.PROJECTILE, 1d))
				return noTarget(data);

			Location origin = data.caster().getEyeLocation();
			if (data.caster().equals(data.target())) {
				origin.add(origin.getDirection().setY(0).multiply(2));
				origin.setYaw(origin.getYaw() + 180);
				origin.setPitch(0);

				launchFireball(origin, data.location(origin));
				return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
			}

			return castAtEntityFromLocation(data.location(origin));
		}

		Location origin = data.caster().getEyeLocation();
		data = applyOffsets(origin, data.location(origin));

		origin.add(origin.getDirection().multiply(2));
		data = data.location(origin);

		launchFireball(origin, data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		Location origin = data.location();

		Vector facing = data.target().getLocation().subtract(origin).toVector();
		origin.setDirection(facing);

		data = applyOffsets(origin, data.location(origin));

		origin.add(origin.getDirection().multiply(2));
		data = data.location(origin);

		launchFireball(origin, data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public void launchFireball(Location origin, SpellData data) {
		Class<? extends Fireball> fireballClass = smallFireball.get(data) ? SmallFireball.class : Fireball.class;

		Fireball fireball = origin.getWorld().spawn(origin, fireballClass, fb -> {
			fb.setGravity(fireballGravity.get(data));
			if (data.hasCaster()) fb.setShooter(data.caster());
		});
		fireballs.put(fireball, data);

		playSpellEffects(data);
		playSpellEffects(EffectPosition.PROJECTILE, fireball, data);
		playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, origin, fireball.getLocation(), data.caster(), fireball, data);
	}

	private SpellData applyOffsets(Location location, SpellData data) {
		if (useRelativeCastLocationOffset.get(data)) {
			Util.applyRelativeOffset(location, relativeCastLocationOffset.get(data));
			data = data.location(location);
		}

		if (useAbsoluteCastLocationOffset.get(data)) {
			Util.applyAbsoluteOffset(location, absoluteCastLocationOffset.get(data));
			data = data.location(location);
		}

		if (data.hasTarget() && doOffsetTargetingCorrections.get(data)) {
			Vector dir = data.target().getLocation().subtract(location).toVector();
			location.setDirection(dir);
			data = data.location(location);
		}

		return data;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		Entity entityRaw = event.getEntity();
		if (!(entityRaw instanceof Fireball fireball)) return;
		if (!fireballs.containsKey(fireball)) return;

		LivingEntity caster = fireball.getShooter() instanceof LivingEntity le ? le : null;
		SpellData data = fireballs.get(fireball);

		playSpellEffects(EffectPosition.TARGET, fireball.getLocation(), new SpellData(caster, data.power(), data.args()));

		if (noExplosion.get(data)) {
			event.setCancelled(true);
			Location loc = fireball.getLocation();
			if (noExplosionEffect.get(data)) loc.getWorld().createExplosion(loc, 0);

			double noExplosionDamageRange = this.noExplosionDamageRange.get(data);
			List<Entity> inRange = fireball.getNearbyEntities(noExplosionDamageRange, noExplosionDamageRange, noExplosionDamageRange);
			for (Entity entity : inRange) {
				if (!(entity instanceof LivingEntity target)) continue;
				if (!validTargetList.canTarget(caster, entity)) continue;

				double noExplosionDamage = this.noExplosionDamage.get(data);
				if (powerAffectsNoExplosionDamage.get(data)) noExplosionDamage *= data.power();

				target.damage(noExplosionDamage, caster);
			}

			if (!noFire.get(data)) {
				Set<Block> fires = new HashSet<>();
				for (int x = loc.getBlockX() - 1; x <= loc.getBlockX() + 1; x++) {
					for (int y = loc.getBlockY() - 1; y <= loc.getBlockY() + 1; y++) {
						for (int z = loc.getBlockZ() - 1; z <= loc.getBlockZ() + 1; z++) {
							if (!loc.getWorld().getBlockAt(x, y, z).getType().isAir()) continue;
							Block b = loc.getWorld().getBlockAt(x, y, z);
							BlockUtils.setTypeAndData(b, Material.FIRE, Material.FIRE.createBlockData(), false);
							fires.add(b);
						}
					}
				}
				fireball.remove();
				if (!fires.isEmpty()) {
					MagicSpells.scheduleDelayedTask(() -> fires.stream().filter(b -> b.getType() == Material.FIRE).forEachOrdered(b -> b.setType(Material.AIR)), TimeUtil.TICKS_PER_SECOND, loc);
				}
			}
		} else {
			event.setFire(!noFire.get(data));

			float explosionSize = this.explosionSize.get(data);
			if (explosionSize > 0) event.setRadius(explosionSize);
		}

		if (noExplosion.get(data)) fireballs.remove(fireball);
		else MagicSpells.scheduleDelayedTask(() -> fireballs.remove(fireball), 1, fireball);
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
			double damageMultiplier = this.damageMultiplier.get(data);
			if (damageMultiplier > 0) {
				if (powerAffectsDamageMultiplier.get(data)) damageMultiplier *= data.power();
				event.setDamage(event.getDamage() * damageMultiplier);
			}
		}
	}

}
