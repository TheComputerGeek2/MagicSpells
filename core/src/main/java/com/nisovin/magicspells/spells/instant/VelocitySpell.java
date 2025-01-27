package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.Multimap;
import com.google.common.collect.ArrayListMultimap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.util.Vector;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class VelocitySpell extends InstantSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private static VelocityMonitor velocityMonitor;

	private final ConfigData<Double> speed;

	private final ConfigData<Boolean> cancelDamage;
	private final ConfigData<Boolean> powerAffectsSpeed;
	private final ConfigData<Boolean> addVelocityInstead;

	public VelocitySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		speed = getConfigDataDouble("speed", 40);

		cancelDamage = getConfigDataBoolean("cancel-damage", true);
		powerAffectsSpeed = getConfigDataBoolean("power-affects-speed", true);
		addVelocityInstead = getConfigDataBoolean("add-velocity-instead", false);
	}

	@Override
	protected void initialize() {
		super.initialize();

		if (velocityMonitor == null) velocityMonitor = new VelocityMonitor();
	}

	@Override
	public CastResult cast(SpellData data) {
		return castAtEntity(data.target(data.caster()));
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		double speed = this.speed.get(data) / 10;
		if (powerAffectsSpeed.get(data)) speed *= data.power();

		Vector velocity = data.target().getLocation().getDirection().normalize().multiply(speed);

		if (addVelocityInstead.get(data)) data.target().setVelocity(data.target().getVelocity().add(velocity));
		else data.target().setVelocity(velocity);

		velocityMonitor.add(new VelocityData(this, data, cancelDamage.get(data)));
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		double speed = this.speed.get(data) / 10;
		if (powerAffectsSpeed.get(data)) speed *= data.power();

		Vector velocity = data.location().getDirection().normalize().multiply(speed);

		if (addVelocityInstead.get(data)) data.target().setVelocity(data.target().getVelocity().add(velocity));
		else data.target().setVelocity(velocity);

		velocityMonitor.add(new VelocityData(this, data, cancelDamage.get(data)));
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	protected void turnOff() {
		if (velocityMonitor != null) {
			velocityMonitor.stop();
			velocityMonitor = null;
		}
	}

	public boolean isJumping(LivingEntity livingEntity) {
		Collection<VelocityData> data = velocityMonitor.jumping.get(livingEntity);
		if (data.isEmpty()) return false;

		for (VelocityData velocityData : data)
			if (velocityData.velocitySpell == this)
				return true;

		return false;
	}

	public static Multimap<LivingEntity, VelocityData> getJumping() {
		return velocityMonitor.jumping;
	}

	private static class VelocityMonitor implements Runnable, Listener {

		private final Multimap<LivingEntity, VelocityData> jumping = ArrayListMultimap.create();
		private final List<VelocityData> queue = new ArrayList<>();

		private boolean running = false;
		private ScheduledTask task = null;

		public void add(VelocityData data) {
			queue.add(data);
			start();
		}

		public void start() {
			if (task != null) return;

			MagicSpells.registerEvents(this);
			task = MagicSpells.scheduleRepeatingTask(this, 0, 1);
		}

		public void stop() {
			if (task == null) return;

			EntityDamageEvent.getHandlerList().unregister(this);
			MagicSpells.cancelTask(task);
			task = null;

			jumping.clear();
			queue.clear();
		}

		@Override
		public void run() {
			running = true;

			Iterator<Map.Entry<LivingEntity, Collection<VelocityData>>> it = jumping.asMap().entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<LivingEntity, Collection<VelocityData>> entry = it.next();

				LivingEntity target = entry.getKey();
				if (!target.isValid()) {
					it.remove();
					continue;
				}

				if (!target.isOnGround()) continue;

				Collection<VelocityData> velocityData = entry.getValue();
				for (VelocityData data : velocityData)
					data.velocitySpell.playSpellEffects(EffectPosition.SPECIAL, target.getLocation(), data.spellData);

				it.remove();
			}

			running = false;

			for (VelocityData data : queue) jumping.put(data.spellData.target(), data);
			queue.clear();

			if (jumping.isEmpty()) stop();
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void onFall(EntityDamageEvent event) {
			if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
			if (!(event.getEntity() instanceof LivingEntity target) || !target.isOnGround()) return;

			Collection<VelocityData> jumpingData = jumping.get(target);
			Iterator<VelocityData> it = jumpingData.iterator();

			while (it.hasNext()) {
				VelocityData data = it.next();
				if (data.cancelDamage) {
					event.setCancelled(true);
					if (running) return;
				}

				if (running) continue;

				data.velocitySpell.playSpellEffects(EffectPosition.SPECIAL, target.getLocation(), data.spellData);
				it.remove();
			}
		}

	}

	public record VelocityData(VelocitySpell velocitySpell, SpellData spellData, boolean cancelDamage) {
	}

}
