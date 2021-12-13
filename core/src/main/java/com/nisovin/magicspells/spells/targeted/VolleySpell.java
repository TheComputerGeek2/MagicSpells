package com.nisovin.magicspells.spells.targeted;

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

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellPreImpactEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class VolleySpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntityFromLocationSpell {

	private static final String METADATA_KEY = "MagicSpellsSource";
	
	private VolleySpell thisSpell;

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

	public VolleySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		thisSpell = this;

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
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (noTarget) {
				volley(caster, caster.getLocation(), null, power, args);
				return PostCastAction.HANDLE_NORMALLY;
			}

			Block target;
			try {
				target = getTargetedBlock(caster, power);
			} catch (IllegalStateException e) {
				target = null;
			}
			if (target == null || BlockUtils.isAir(target.getType())) return noTarget(caster);
			volley(caster, caster.getLocation(), target.getLocation(), power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		if (noTarget) return false;
		volley(caster, caster.getLocation(), target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, String[] args) {
		if (noTarget) return false;
		volley(caster, from, target.getLocation(), power, args);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(caster, from, target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power, String[] args) {
		if (noTarget) return false;
		volley(null, from, target.getLocation(), power, args);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(from, target, power, null);
	}

	private void volley(LivingEntity caster, Location from, Location target, float power, String[] args) {
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
				a.setMetadata(METADATA_KEY, new FixedMetadataValue(MagicSpells.plugin, "VolleySpell" + internalName));
				if (removeDelay > 0) arrowList.add(a);
				playSpellEffects(EffectPosition.PROJECTILE, a);
				playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, spawn, a.getLocation(), caster, a);
			}

			if (removeDelay > 0) {
				MagicSpells.scheduleDelayedTask(() -> {
					for (Arrow a : arrowList) {
						a.remove();
					}
					arrowList.clear();
				}, removeDelay);
			}

		} else new ArrowShooter(caster, spawn, v, power);

		if (caster != null) {
			if (target != null) playSpellEffects(caster, target);
			else playSpellEffects(EffectPosition.CASTER, caster);
		} else {
			playSpellEffects(EffectPosition.CASTER, from);
			if (target != null) playSpellEffects(EffectPosition.TARGET, target);
		}
	}
	
	@EventHandler
	public void onArrowHit(EntityDamageByEntityEvent event) {
		if (event.getCause() != DamageCause.PROJECTILE) return;
		if (!(event.getEntity() instanceof LivingEntity)) return;
		Entity damagerEntity = event.getDamager();
		if (!(damagerEntity instanceof Arrow a) || !damagerEntity.hasMetadata(METADATA_KEY)) return;
		MetadataValue meta = damagerEntity.getMetadata(METADATA_KEY).iterator().next();
		if (!meta.value().equals("VolleySpell" + internalName)) return;

		event.setDamage(damage);
		SpellPreImpactEvent preImpactEvent = new SpellPreImpactEvent(thisSpell, thisSpell, (LivingEntity) a.getShooter(), (LivingEntity) event.getEntity(), 1);
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
			this.speedShooter = thisSpell.speed / 10F;
			this.arrowsShooter = powerAffectsArrowCount ? Math.round(thisSpell.arrows * power) : thisSpell.arrows;
			if (powerAffectsSpeed) this.speedShooter *= power;
			this.count = 0;
			
			if (removeDelay > 0) this.arrowMap = new HashMap<>();

			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, shootInterval);
		}
		
		@Override
		public void run() {			
			if (count < arrowsShooter) {
				Arrow a = spawn.getWorld().spawnArrow(spawn, dir, speedShooter, spread / 10.0F);
				a.setKnockbackStrength(knockbackStrength);
				a.setCritical(critical);
				a.setGravity(gravity);
				a.setDamage(damage);
				playSpellEffects(EffectPosition.PROJECTILE, a);
				playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, caster.getLocation(), a.getLocation(), caster, a);
				a.setVelocity(a.getVelocity());
				if (caster != null) a.setShooter(caster);
				if (fire > 0) a.setFireTicks(fire);
				a.setMetadata(METADATA_KEY, new FixedMetadataValue(MagicSpells.plugin, "VolleySpell" + internalName));
				if (removeDelay > 0) arrowMap.put(count, a);
			}
			
			if (removeDelay > 0) {
				int old = count - removeDelay;
				if (old > 0) {
					Arrow a = arrowMap.remove(old);
					if (a != null) a.remove();
				}
			}

			if (count >= arrowsShooter + removeDelay) MagicSpells.cancelTask(taskId);

			count++;
		}
		
	}
	
}
