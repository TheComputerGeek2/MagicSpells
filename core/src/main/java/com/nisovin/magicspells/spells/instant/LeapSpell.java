package com.nisovin.magicspells.spells.instant;

import java.util.*;

import com.google.common.collect.Multimap;
import com.google.common.collect.ArrayListMultimap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class LeapSpell extends InstantSpell {

	private static LeapMonitor leapMonitor;

	private final String landSpellName;

	private final ConfigData<Float> rotation;
	private final ConfigData<Float> upwardVelocity;
	private final ConfigData<Float> forwardVelocity;

	private final ConfigData<Boolean> clientOnly;
	private final ConfigData<Boolean> cancelDamage;
	private final ConfigData<Boolean> addVelocityInstead;
	private final ConfigData<Boolean> powerAffectsVelocity;

	private Subspell landSpell;

	public LeapSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		rotation = getConfigDataFloat("rotation", 0F);
		upwardVelocity = getConfigDataFloat("upward-velocity", 15F);
		forwardVelocity = getConfigDataFloat("forward-velocity", 40F);

		clientOnly = getConfigDataBoolean("client-only", false);
		cancelDamage = getConfigDataBoolean("cancel-damage", true);
		addVelocityInstead = getConfigDataBoolean("add-velocity-instead", false);
		powerAffectsVelocity = getConfigDataBoolean("power-affects-velocity", true);

		landSpellName = getConfigString("land-spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		landSpell = initSubspell(landSpellName,
				"LeapSpell '" + internalName + "' has an invalid land-spell defined!",
				true);

		if (leapMonitor == null) leapMonitor = new LeapMonitor();
	}

	protected void turnOff() {
		if (leapMonitor != null) {
			leapMonitor.stop();
			leapMonitor = null;
		}
	}

	@Override
	public CastResult cast(SpellData data) {
		Vector v = data.caster().getLocation().getDirection();

		float forwardVelocity = this.forwardVelocity.get(data) / 10;
		if (powerAffectsVelocity.get(data)) forwardVelocity *= data.power();

		float upwardVelocity = this.upwardVelocity.get(data) / 10;
		if (powerAffectsVelocity.get(data)) upwardVelocity *= data.power();

		float rotation = this.rotation.get(data);

		v.setY(0).normalize().multiply(forwardVelocity).setY(upwardVelocity);
		if (rotation != 0) Util.rotateVector(v, rotation);
		v = Util.makeFinite(v);

		if (clientOnly.get(data) && data.caster() instanceof Player caster) {
			MagicSpells.getVolatileCodeHandler().setClientVelocity(caster, v);
		} else {
			if (addVelocityInstead.get(data)) data.caster().setVelocity(data.caster().getVelocity().add(v));
			else data.caster().setVelocity(v);
		}

		leapMonitor.add(new LeapData(this, data, cancelDamage.get(data)));

		playSpellEffects(EffectPosition.CASTER, data.caster(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public static Multimap<LivingEntity, LeapData> getJumping() {
		return leapMonitor.jumping;
	}

	public boolean isJumping(LivingEntity livingEntity) {
		Collection<LeapData> data = leapMonitor.jumping.get(livingEntity);
		if (data.isEmpty()) return false;

		for (LeapData leapData : data)
			if (leapData.leapSpell == this)
				return true;

		return false;
	}

	public Subspell getLandSpell() {
		return landSpell;
	}

	public void setLandSpell(Subspell landSpell) {
		this.landSpell = landSpell;
	}

	private static class LeapMonitor implements Runnable, Listener {

		private final Multimap<LivingEntity, LeapData> jumping = ArrayListMultimap.create();
		private final List<LeapData> queue = new ArrayList<>();

		private boolean running = false;
		private ScheduledTask task = null;

		public void add(LeapData data) {
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

			Iterator<Map.Entry<LivingEntity, Collection<LeapData>>> it = jumping.asMap().entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<LivingEntity, Collection<LeapData>> entry = it.next();

				LivingEntity caster = entry.getKey();
				if (!caster.isValid()) {
					it.remove();
					continue;
				}

				if (!caster.isOnGround()) continue;

				Collection<LeapData> leapData = entry.getValue();
				for (LeapData data : leapData) {
					if (data.leapSpell.landSpell != null) data.leapSpell.landSpell.subcast(data.spellData);
					data.leapSpell.playSpellEffects(EffectPosition.TARGET, caster.getLocation(), data.spellData);
				}

				it.remove();
			}

			running = false;

			for (LeapData data : queue) jumping.put(data.spellData.caster(), data);
			queue.clear();

			if (jumping.isEmpty()) stop();
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void onFall(EntityDamageEvent event) {
			if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
			if (!(event.getEntity() instanceof LivingEntity caster) || !caster.isOnGround()) return;

			Collection<LeapData> jumpingData = jumping.get(caster);
			Iterator<LeapData> it = jumpingData.iterator();

			while (it.hasNext()) {
				LeapData data = it.next();
				if (data.cancelDamage) {
					event.setCancelled(true);
					if (running) return;
				}

				if (running) continue;

				if (data.leapSpell.landSpell != null) data.leapSpell.landSpell.subcast(data.spellData);
				data.leapSpell.playSpellEffects(EffectPosition.TARGET, caster.getLocation(), data.spellData);

				it.remove();
			}
		}

	}

	public record LeapData(LeapSpell leapSpell, SpellData spellData, boolean cancelDamage) {
	}

}
