package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class FlamewalkSpell extends BuffSpell {

	private final Map<UUID, FlamewalkData> entities;

	private final ConfigData<Double> radius;

	private final int tickInterval;
	private final ConfigData<Integer> fireTicks;

	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> constantRadius;
	private final ConfigData<Boolean> constantFireTicks;
	private final ConfigData<Boolean> powerAffectsFireTicks;

	private Burner burner;

	public FlamewalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		fireTicks = getConfigDataInt("fire-ticks", 80);
		tickInterval = getConfigInt("tick-interval", 100);

		radius = getConfigDataDouble("radius", 8);

		checkPlugins = getConfigDataBoolean("check-plugins", true);
		constantRadius = getConfigDataBoolean("constant-radius", true);
		constantFireTicks = getConfigDataBoolean("constant-fire-ticks", true);
		powerAffectsFireTicks = getConfigDataBoolean("power-affects-fire-ticks", true);

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		if (burner == null) burner = new Burner();

		boolean constantRadius = this.constantRadius.get(data);
		boolean constantFireTicks = this.constantFireTicks.get(data);

		int fireTicks = 0;
		if (constantFireTicks) {
			fireTicks = this.fireTicks.get(data);
			if (powerAffectsFireTicks.get(data)) fireTicks = Math.round(fireTicks * data.power());
		}

		entities.put(data.target().getUniqueId(), new FlamewalkData(
			data,
			checkPlugins.get(data),
			constantRadius ? radius.get(data) : 0,
			constantRadius,
			fireTicks,
			constantFireTicks
		));

		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		return castBuff(data);
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

	private record FlamewalkData(SpellData spellData, boolean checkPlugins, double radius, boolean constantRadius, int fireTicks, boolean constantFireTicks) {
	}

	private class Burner implements Runnable {

		private final ScheduledTask task;

		private Burner() {
			task = MagicSpells.scheduleRepeatingTask(this, tickInterval, tickInterval);
		}

		public void stop() {
			MagicSpells.cancelTask(task);
		}

		@Override
		public void run() {
			for (UUID id : entities.keySet()) {
				Entity e = Bukkit.getEntity(id);
				if (!(e instanceof LivingEntity caster)) continue;

				if (isExpired(caster)) {
					turnOff(caster);
					continue;
				}

				FlamewalkData data = entities.get(caster.getUniqueId());
				playSpellEffects(EffectPosition.DELAYED, caster, data.spellData);

				double radius = data.constantRadius ? data.radius : FlamewalkSpell.this.radius.get(data.spellData);
				radius = Math.min(radius, MagicSpells.getGlobalRadius());

				for (Entity entity : caster.getNearbyEntities(radius, radius, radius)) {
					if (!(entity instanceof LivingEntity target) || !validTargetList.canTarget(caster, target)) continue;

					if (data.checkPlugins && checkFakeDamageEvent(caster, target))
						continue;

					SpellTargetEvent targetEvent = new SpellTargetEvent(FlamewalkSpell.this, data.spellData, target);
					if (!targetEvent.callEvent()) continue;

					SpellData subData = targetEvent.getSpellData();

					int fireTicks;
					if (!data.constantFireTicks) {
						fireTicks = FlamewalkSpell.this.fireTicks.get(subData);
						if (powerAffectsFireTicks.get(subData)) fireTicks = Math.round(fireTicks * subData.power());
					} else fireTicks = data.fireTicks;

					target.setFireTicks(fireTicks);
					addUseAndChargeCost(caster);

					playSpellEffects(EffectPosition.TARGET, target, subData);
					playSpellEffectsTrail(caster.getLocation(), entity.getLocation(), subData);
				}

			}

		}

	}

}
