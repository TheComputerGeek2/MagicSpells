package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.JutsuFilter;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class LevitateJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private Map<UUID, Levitator> levitating;

	private int tickRate;
	private int duration;

	private float yOffset;
	private float minDistance;
	private float distanceChange;
	private float maxDistanceSquared;

	private boolean cancelOnJutsuCast;
	private boolean cancelOnItemSwitch;
	private boolean cancelOnTakeDamage;

	private JutsuFilter filter;
	
	public LevitateJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		tickRate = getConfigInt("tick-rate", 5);
		duration = getConfigInt("duration", 10);
		if (duration < tickRate) duration = tickRate;

		yOffset = getConfigFloat("y-offset", 0F);
		minDistance = getConfigFloat("min-distance", 1F);
		distanceChange = getConfigFloat("distance-change", 0F);
		maxDistanceSquared = getConfigFloat("max-distance", 200);

		cancelOnJutsuCast = getConfigBoolean("cancel-on-jutsu-cast", false);
		cancelOnItemSwitch = getConfigBoolean("cancel-on-item-switch", true);
		cancelOnTakeDamage = getConfigBoolean("cancel-on-take-damage", true);

		List<String> jutsus = getConfigStringList("jutsus", null);
		List<String> deniedJutsus = getConfigStringList("denied-jutsus", null);
		List<String> tagList = getConfigStringList("jutsu-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-jutsu-tags", null);
		filter = new JutsuFilter(jutsus, deniedJutsus, tagList, deniedTagList);

		maxDistanceSquared *= maxDistanceSquared;
		
		levitating = new HashMap<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (cancelOnItemSwitch) registerEvents(new ItemSwitchListener());
		if (cancelOnJutsuCast) registerEvents(new JutsuCastListener());
		if (cancelOnTakeDamage) registerEvents(new DamageListener());
	}

	@Override
	public void turnOff() {
		Util.forEachValueOrdered(levitating, Levitator::stop);
		levitating.clear();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (isLevitating(livingEntity)) {
			levitating.remove(livingEntity.getUniqueId()).stop();
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);
			
			levitate(livingEntity, target.getTarget());
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		levitate(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	public boolean isBeingLevitated(LivingEntity entity) {
		for (Levitator levitator : levitating.values()) {
			if (levitator.target.equals(entity)) return true;
		}
		return false;
	}

	public void removeLevitate(LivingEntity entity) {
		List<LivingEntity> toRemove = new ArrayList<>();
		for (Levitator levitator : levitating.values()) {
			if (!levitator.target.equals(entity)) continue;
			toRemove.add(levitator.caster);
			levitator.stop();
		}
		for (LivingEntity caster : toRemove) {
			levitating.remove(caster.getUniqueId());
		}
		toRemove.clear();
	}

	private boolean isLevitating(LivingEntity entity) {
		return levitating.containsKey(entity.getUniqueId());
	}

	private void levitate(LivingEntity caster, Entity target) {
		double distance = caster.getLocation().distance(target.getLocation());
		Levitator lev = new Levitator(caster, target, duration / tickRate, distance);
		levitating.put(caster.getUniqueId(), lev);
		playJutsuEffects(caster, target);
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player pl = event.getEntity();
		if (!isLevitating(pl)) return;
		levitating.remove(pl.getUniqueId()).stop();
	}
	
	public class ItemSwitchListener implements Listener {
		
		@EventHandler
		public void onItemSwitch(PlayerItemHeldEvent event) {
			Player pl = event.getPlayer();
			if (!isLevitating(pl)) return;
			levitating.remove(pl.getUniqueId()).stop();
		}
		
	}
	
	public class JutsuCastListener implements Listener {
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onJutsuCast(JutsuCastEvent event) {
			LivingEntity caster = event.getCaster();
			if (!isLevitating(caster)) return;
			if (!filter.check(event.getJutsu())) return;
			levitating.remove(caster.getUniqueId()).stop();
		}
		
	}
	
	public class DamageListener implements Listener {
		
		@EventHandler
		public void onEntityDamage(EntityDamageByEntityEvent event) {
			Entity entity = event.getEntity();
			if (!(entity instanceof Player)) return;
			if (!isLevitating((LivingEntity) entity)) return;
			levitating.remove(entity.getUniqueId()).stop();
		}
		
	}
	
	private class Levitator implements Runnable {
		
		private LivingEntity caster;
		private Entity target;
		private double distance;
		private int duration;
		private int counter;
		private int taskId;
		private boolean stopped;
		
		private Levitator(LivingEntity caster, Entity target, int duration, double distance) {
			this.caster = caster;
			this.target = target;
			this.duration = duration;
			this.distance = distance;
			counter = 0;
			stopped = false;
			taskId = MagicJutsus.scheduleRepeatingTask(this, 0, tickRate);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, caster.getLocation(), target.getLocation(), caster, target);
		}
		
		@Override
		public void run() {
			if (stopped) return;
			if (!caster.getWorld().equals(target.getWorld())) return;
			if (caster.getLocation().distanceSquared(target.getLocation()) > maxDistanceSquared) return;
			if (caster.isDead() || !caster.isValid()) {
				stop();
				return;
			}

			if (distanceChange != 0 && distance > minDistance) {
				distance -= distanceChange;
				if (distance < minDistance) distance = minDistance;
			}

			target.setFallDistance(0);
			Vector wantedLocation = caster.getEyeLocation().toVector().add(caster.getLocation().getDirection().multiply(distance)).add(new Vector(0, yOffset, 0));
			Vector v = wantedLocation.subtract(target.getLocation().toVector()).multiply(tickRate / 25F + 0.1);
			target.setVelocity(v);
			counter++;

			if (duration > 0 && counter >= duration) {
				stop();
				levitating.remove(caster.getUniqueId());
			}
		}
		
		private void stop() {
			stopped = true;
			MagicJutsus.cancelTask(taskId);
		}
		
	}

}
