package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class FlamewalkSpell extends BuffSpell {

	private final Map<UUID, SpellData> entities;

	private ConfigData<Integer> fireTicks;
	private ConfigData<Integer> radius;
	private int tickInterval;

	private boolean powerAffectsFireTicks;
	private boolean checkPlugins;

	private Burner burner;

	public FlamewalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataInt("radius", 8);
		fireTicks = getConfigDataInt("fire-ticks", 80);
		tickInterval = getConfigInt("tick-interval", 100);

		checkPlugins = getConfigBoolean("check-plugins", true);
		powerAffectsFireTicks = getConfigBoolean("power-affects-fire-ticks", true);

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		entities.put(entity.getUniqueId(), new SpellData(power, args));
		if (burner == null) burner = new Burner();
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
		if (!entities.isEmpty()) return;
		if (burner == null) return;

		burner.stop();
		burner = null;
	}

	@Override
	protected void turnOff() {
		entities.clear();
		if (burner == null) return;

		burner.stop();
		burner = null;
	}

	public Map<UUID, SpellData> getEntities() {
		return entities;
	}

	public boolean shouldCheckPlugins() {
		return checkPlugins;
	}

	public void setCheckPlugins(boolean checkPlugins) {
		this.checkPlugins = checkPlugins;
	}

	private class Burner implements Runnable {

		private final int taskId;

		private Burner() {
			taskId = MagicSpells.scheduleRepeatingTask(this, tickInterval, tickInterval);
		}

		public void stop() {
			MagicSpells.cancelTask(taskId);
		}

		@Override
		public void run() {
			for (UUID id : entities.keySet()) {
				Entity e = Bukkit.getEntity(id);
				if (!(e instanceof LivingEntity entity)) continue;
				if (isExpired(entity)) {
					turnOff(entity);
					continue;
				}

				SpellData data = entities.get(entity.getUniqueId());
				playSpellEffects(EffectPosition.DELAYED, entity);

				double radius = Math.min(FlamewalkSpell.this.radius.get(entity, null, data.power(), data.args()), MagicSpells.getGlobalRadius());
				List<Entity> entities = entity.getNearbyEntities(radius, radius, radius);
				for (Entity target : entities) {
					if (!(target instanceof LivingEntity livingTarget)) continue;
					if (validTargetList != null && !validTargetList.canTarget(target)) continue;
					if (entity.equals(target)) continue;
					if (checkPlugins) {
						MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(entity, target, DamageCause.ENTITY_ATTACK, 1, FlamewalkSpell.this);
						EventUtil.call(event);
						if (event.isCancelled()) continue;
					}

					int fireTicks = FlamewalkSpell.this.fireTicks.get(entity, livingTarget, data.power(), data.args());
					if (powerAffectsFireTicks) fireTicks = Math.round(fireTicks * data.power());
					target.setFireTicks(fireTicks);

					addUseAndChargeCost(entity);
					playSpellEffects(EffectPosition.TARGET, target);
					playSpellEffectsTrail(entity.getLocation(), target.getLocation());
				}

			}

		}

	}

}
