package com.nisovin.magicspells.spells.targeted;

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

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class LevitateSpell extends TargetedSpell implements TargetedEntitySpell {

	private Map<UUID, Levitator> levitating;

	private int tickRate;
	private int duration;

	private float yOffset;
	private float minDistance;
	private float distanceChange;
	private float maxDistanceSquared;

	private boolean cancelOnSpellCast;
	private boolean cancelOnItemSwitch;
	private boolean cancelOnTakeDamage;

	private SpellFilter filter;
	
	public LevitateSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		tickRate = getConfigInt("tick-rate", 5);
		duration = getConfigInt("duration", 10);
		if (duration < tickRate) duration = tickRate;

		yOffset = getConfigFloat("y-offset", 0F);
		minDistance = getConfigFloat("min-distance", 1F);
		distanceChange = getConfigFloat("distance-change", 0F);
		maxDistanceSquared = getConfigFloat("max-distance", 200);

		cancelOnSpellCast = getConfigBoolean("cancel-on-spell-cast", false);
		cancelOnItemSwitch = getConfigBoolean("cancel-on-item-switch", true);
		cancelOnTakeDamage = getConfigBoolean("cancel-on-take-damage", true);

		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> tagList = getConfigStringList("spell-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-spell-tags", null);
		filter = new SpellFilter(spells, deniedSpells, tagList, deniedTagList);

		maxDistanceSquared *= maxDistanceSquared;
		
		levitating = new HashMap<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (cancelOnItemSwitch) registerEvents(new ItemSwitchListener());
		if (cancelOnSpellCast) registerEvents(new SpellCastListener());
		if (cancelOnTakeDamage) registerEvents(new DamageListener());
	}

	@Override
	public void turnOff() {
		Util.forEachValueOrdered(levitating, Levitator::stop);
		levitating.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (isLevitating(caster)) {
			levitating.remove(caster.getUniqueId()).stop();
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
			if (target == null) return noTarget(caster);
			
			levitate(caster, target.getTarget(), target.getPower(), args);
			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		levitate(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		levitate(caster, target, power, null);
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

	private void levitate(LivingEntity caster, LivingEntity target, float power, String[] args) {
		double distance = caster.getLocation().distance(target.getLocation());
		Levitator lev = new Levitator(caster, target, duration / tickRate, distance);
		levitating.put(caster.getUniqueId(), lev);
		playSpellEffects(caster, target);
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
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
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
			taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickRate);
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
			MagicSpells.cancelTask(taskId);
		}
		
	}

}
