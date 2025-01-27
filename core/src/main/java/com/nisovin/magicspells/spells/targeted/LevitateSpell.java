package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
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

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class LevitateSpell extends TargetedSpell implements TargetedEntitySpell {

	private final Map<UUID, Levitator> levitating;

	private final ConfigData<Integer> tickRate;
	private final ConfigData<Integer> duration;

	private final ConfigData<Double> yOffset;
	private final ConfigData<Double> minDistance;
	private final ConfigData<Double> maxDistance;
	private final ConfigData<Double> distanceChange;

	private final boolean cancelOnSpellCast;
	private final boolean cancelOnItemSwitch;
	private final boolean cancelOnTakeDamage;

	private SpellFilter filter;

	public LevitateSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		tickRate = getConfigDataInt("tick-rate", 5);
		duration = getConfigDataInt("duration", 10);

		yOffset = getConfigDataDouble("y-offset", 0F);
		minDistance = getConfigDataDouble("min-distance", 1F);
		maxDistance = getConfigDataDouble("max-distance", 200);
		distanceChange = getConfigDataDouble("distance-change", 0F);

		cancelOnSpellCast = getConfigBoolean("cancel-on-spell-cast", false);
		cancelOnItemSwitch = getConfigBoolean("cancel-on-item-switch", true);
		cancelOnTakeDamage = getConfigBoolean("cancel-on-take-damage", true);

		levitating = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		filter = getConfigSpellFilter();

		if (cancelOnItemSwitch) registerEvents(new ItemSwitchListener());
		if (cancelOnSpellCast) registerEvents(new SpellCastListener());
		if (cancelOnTakeDamage) registerEvents(new DamageListener());
	}

	@Override
	public void turnOff() {
		levitating.values().forEach(Levitator::stop);
		levitating.clear();
	}

	@Override
	public CastResult cast(SpellCastState state, SpellData data) {
		if (isLevitating(data.caster())) {
			levitating.remove(data.caster().getUniqueId()).stop();
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (state != SpellCastState.NORMAL) return new CastResult(PostCastAction.HANDLE_NORMALLY, data);

		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult cast(SpellData data) {
		return cast(SpellCastState.NORMAL, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		LivingEntity caster = data.caster();
		LivingEntity target = data.target();

		int duration = this.duration.get(data);
		int tickRate = this.tickRate.get(data);
		if (duration < tickRate) duration = tickRate;

		double distance = caster.getLocation().distance(target.getLocation());
		Levitator lev = new Levitator(data, duration / tickRate, tickRate, distance);
		levitating.put(caster.getUniqueId(), lev);

		playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, caster.getLocation(), target.getLocation(), caster, target, data);
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public boolean isBeingLevitated(LivingEntity entity) {
		for (Levitator levitator : levitating.values()) {
			if (levitator.data.target().equals(entity)) return true;
		}
		return false;
	}

	public void removeLevitate(LivingEntity entity) {
		List<LivingEntity> toRemove = new ArrayList<>();
		for (Levitator levitator : levitating.values()) {
			if (!levitator.data.target().equals(entity)) continue;
			toRemove.add(levitator.data.caster());
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

	public class SpellCastListener implements Listener {

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onSpellCast(SpellCastEvent event) {
			LivingEntity caster = event.getCaster();
			if (!isLevitating(caster)) return;
			if (!filter.check(event.getSpell())) return;
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

		private final SpellData data;

		private final double distanceChange;
		private final double maxDistanceSq;
		private final double minDistance;
		private final double yOffset;
		private final int duration;
		private final int tickRate;
		private final ScheduledTask task;

		private double distance;
		private int counter;
		private boolean stopped;

		private Levitator(SpellData data, int duration, int tickRate, double distance) {
			this.duration = duration;
			this.distance = distance;
			this.tickRate = tickRate;
			this.data = data;

			counter = 0;
			stopped = false;

			double maxDistance = LevitateSpell.this.maxDistance.get(data);
			maxDistanceSq = maxDistance * maxDistance;

			distanceChange = LevitateSpell.this.distanceChange.get(data);
			minDistance = LevitateSpell.this.minDistance.get(data);
			yOffset = LevitateSpell.this.yOffset.get(data);

			task = MagicSpells.scheduleRepeatingTask(this, 0, tickRate, data.caster());
		}

		@Override
		public void run() {
			if (stopped) return;

			counter++;

			if (duration > 0 && counter >= duration) {
				stop();
				levitating.remove(data.caster().getUniqueId());
			}

			if (!data.caster().getWorld().equals(data.target().getWorld())) return;
			if (data.caster().getLocation().distanceSquared(data.target().getLocation()) > maxDistanceSq) return;
			if (!data.caster().isValid()) {
				stop();
				return;
			}

			if (distanceChange != 0 && distance > minDistance) {
				distance -= distanceChange;
				if (distance < minDistance) distance = minDistance;
			}

			data.target().setFallDistance(0);

			Location targetLoc = data.caster().getEyeLocation();
			Vector offset = targetLoc.getDirection().multiply(distance);
			offset.setY(offset.getY() + yOffset);
			targetLoc.add(offset);

			Vector velocity = targetLoc.subtract(data.target().getLocation()).multiply(tickRate / 25f + 0.1).toVector();
			data.target().setVelocity(velocity);
		}

		private void stop() {
			stopped = true;
			MagicSpells.cancelTask(task);
		}

	}

}
