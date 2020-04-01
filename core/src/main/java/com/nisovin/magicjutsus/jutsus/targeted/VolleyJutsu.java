package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuPreImpactEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityFromLocationJutsu;

public class VolleyJutsu extends TargetedJutsu implements TargetedLocationJutsu, TargetedEntityFromLocationJutsu {

	private static final String METADATA_KEY = "MagicJutsusSource";
	
	private VolleyJutsu thisJutsu;

	private int fire;
	private int speed;
	private int arrows;
	private int spread;
	private int removeDelay;
	private int shootInterval;
	private int knockbackStrength;

	private double damage;

	private float yOffset;

	private boolean gravity;
	private boolean critical;
	private boolean noTarget;
	private boolean powerAffectsSpeed;
	private boolean powerAffectsArrowCount;

	public VolleyJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		thisJutsu = this;

		fire = getConfigInt("fire", 0);
		speed = getConfigInt("speed", 20);
		arrows = getConfigInt("arrows", 10);
		spread = getConfigInt("spread", 150);
		removeDelay = getConfigInt("remove-delay", 0);
		shootInterval = getConfigInt("shoot-interval", 0);
		knockbackStrength = getConfigInt("knockback-strength", 0);

		damage = getConfigDouble("damage", 4);

		yOffset = getConfigFloat("y-offset", 3);

		gravity = getConfigBoolean("gravity", true);
		critical = getConfigBoolean("critical", false);
		noTarget = getConfigBoolean("no-target", false);
		powerAffectsSpeed = getConfigBoolean("power-affects-speed", false);
		powerAffectsArrowCount = getConfigBoolean("power-affects-arrow-count", true);
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity caster, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			if (noTarget) {
				volley(caster, caster.getLocation(), null, power);
				return PostCastAction.HANDLE_NORMALLY;
			}

			Block target;
			try {
				target = getTargetedBlock(caster, power);
			} catch (IllegalStateException e) {
				target = null;
			}
			if (target == null || BlockUtils.isAir(target.getType())) return noTarget(caster);
			volley(caster, caster.getLocation(), target.getLocation(), power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		if (noTarget) return false;
		volley(caster, caster.getLocation(), target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		if (noTarget) return false;
		volley(caster, from, target.getLocation(), power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		if (noTarget) return false;
		volley(null, from, target.getLocation(), power);
		return true;
	}

	private void volley(LivingEntity caster, Location from, Location target, float power) {
		Location spawn = from.clone().add(0, yOffset, 0);
		Vector v;

		if (noTarget || target == null) v = from.getDirection();
		else v = target.toVector().subtract(spawn.toVector()).normalize();

		if (shootInterval <= 0) {
			List<Arrow> arrowList = new ArrayList<>();

			int castingArrows = powerAffectsArrowCount ? Math.round(arrows * power) : arrows;
			for (int i = 0; i < castingArrows; i++) {
				float speed = this.speed / 10F;
				if (powerAffectsSpeed) speed *= power;
				Arrow a = from.getWorld().spawnArrow(spawn, v, speed, spread / 10.0F);
				a.setVelocity(a.getVelocity());
				a.setKnockbackStrength(knockbackStrength);
				a.setCritical(critical);
				a.setGravity(gravity);
				a.setDamage(damage);
				if (caster != null) a.setShooter(caster);
				if (fire > 0) a.setFireTicks(fire);
				a.setMetadata(METADATA_KEY, new FixedMetadataValue(MagicJutsus.plugin, "VolleyJutsu" + internalName));
				if (removeDelay > 0) arrowList.add(a);
				playJutsuEffects(EffectPosition.PROJECTILE, a);
				playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, spawn, a.getLocation(), caster, a);
			}

			if (removeDelay > 0) {
				MagicJutsus.scheduleDelayedTask(() -> {
					for (Arrow a : arrowList) {
						a.remove();
					}
					arrowList.clear();
				}, removeDelay);
			}

		} else new ArrowShooter(caster, spawn, v, power);

		if (caster != null) {
			if (target != null) playJutsuEffects(caster, target);
			else playJutsuEffects(EffectPosition.CASTER, caster);
		} else {
			playJutsuEffects(EffectPosition.CASTER, from);
			if (target != null) playJutsuEffects(EffectPosition.TARGET, target);
		}
	}
	
	@EventHandler
	public void onArrowHit(EntityDamageByEntityEvent event) {
		if (event.getCause() != DamageCause.PROJECTILE) return;
		if (!(event.getEntity() instanceof LivingEntity)) return;
		Entity damagerEntity = event.getDamager();
		if (!(damagerEntity instanceof Arrow) || !damagerEntity.hasMetadata(METADATA_KEY)) return;
		MetadataValue meta = damagerEntity.getMetadata(METADATA_KEY).iterator().next();
		if (!meta.value().equals("VolleyJutsu" + internalName)) return;

		Arrow a = (Arrow) damagerEntity;
		event.setDamage(damage);
		JutsuPreImpactEvent preImpactEvent = new JutsuPreImpactEvent(thisJutsu, thisJutsu, (LivingEntity) a.getShooter(), (LivingEntity) event.getEntity(), 1);
		EventUtil.call(preImpactEvent);
		if (!preImpactEvent.getRedirected()) return;

		event.setCancelled(true);
		a.setVelocity(a.getVelocity().multiply(-1));
		a.teleport(a.getLocation().add(a.getVelocity()));
	}
	
	private class ArrowShooter implements Runnable {

		private LivingEntity caster;
		private Location spawn;
		private Vector dir;
		private float speedShooter;
		private int arrowsShooter;
		private int taskId;
		private int count;
		private Map<Integer, Arrow> arrowMap;

		private ArrowShooter(LivingEntity caster, Location spawn, Vector dir, float power) {
			this.caster = caster;
			this.spawn = spawn;
			this.dir = dir;
			this.speedShooter = thisJutsu.speed / 10F;
			this.arrowsShooter = powerAffectsArrowCount ? Math.round(thisJutsu.arrows * power) : thisJutsu.arrows;
			if (powerAffectsSpeed) this.speedShooter *= power;
			this.count = 0;
			
			if (removeDelay > 0) this.arrowMap = new HashMap<>();

			this.taskId = MagicJutsus.scheduleRepeatingTask(this, 0, shootInterval);
		}
		
		@Override
		public void run() {			
			if (count < arrowsShooter) {
				Arrow a = spawn.getWorld().spawnArrow(spawn, dir, speedShooter, spread / 10.0F);
				a.setKnockbackStrength(knockbackStrength);
				a.setCritical(critical);
				a.setGravity(gravity);
				a.setDamage(damage);
				playJutsuEffects(EffectPosition.PROJECTILE, a);
				playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, caster.getLocation(), a.getLocation(), caster, a);
				a.setVelocity(a.getVelocity());
				if (caster != null) a.setShooter(caster);
				if (fire > 0) a.setFireTicks(fire);
				a.setMetadata(METADATA_KEY, new FixedMetadataValue(MagicJutsus.plugin, "VolleyJutsu" + internalName));
				if (removeDelay > 0) arrowMap.put(count, a);
			}
			
			if (removeDelay > 0) {
				int old = count - removeDelay;
				if (old > 0) {
					Arrow a = arrowMap.remove(old);
					if (a != null) a.remove();
				}
			}

			if (count >= arrowsShooter + removeDelay) MagicJutsus.cancelTask(taskId);

			count++;
		}
		
	}
	
}
