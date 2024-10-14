package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.function.Supplier;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;

public class EmpowerSpell extends BuffSpell {

	private final Map<UUID, Supplier<Float>> entities;

	private final ConfigData<Float> powerMultiplier;
	private final ConfigData<Float> maxPowerMultiplier;

	private final ConfigData<Boolean> constantMultiplier;
	private final ConfigData<Boolean> powerAffectsMultiplier;

	private SpellFilter filter;

	public EmpowerSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		powerMultiplier = getConfigDataFloat("power-multiplier", 1.5F);
		maxPowerMultiplier = getConfigDataFloat("max-power-multiplier", 1.5F);

		constantMultiplier = getConfigDataBoolean("constant-multiplier", true);
		powerAffectsMultiplier = getConfigDataBoolean("power-affects-multiplier", true);

		entities = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		filter = getConfigSpellFilter();
	}

	@Override
	public boolean castBuff(SpellData data) {
		Supplier<Float> supplier;
		if (constantMultiplier.get(data)) {
			float multiplier = powerMultiplier.get(data);
			if (powerAffectsMultiplier.get(data)) multiplier *= data.power();
			multiplier = Math.min(multiplier, maxPowerMultiplier.get(data));

			float finalMultiplier = multiplier;
			supplier = () -> finalMultiplier;
		} else {
			supplier = () -> {
				float multiplier = powerMultiplier.get(data);
				if (powerAffectsMultiplier.get(data)) multiplier *= data.power();

				return Math.min(multiplier, maxPowerMultiplier.get(data));
			};
		}

		entities.put(data.target().getUniqueId(), supplier);
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
	}

	@Override
	protected void turnOff() {
		entities.clear();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSpellCast(SpellCastEvent event) {
		LivingEntity caster = event.getCaster();
		if (caster == null || !isActive(caster) || !filter.check(event.getSpell())) return;

		event.increasePower(entities.get(caster.getUniqueId()).get());
		addUseAndChargeCost(caster);
	}

	public Map<UUID, Supplier<Float>> getEntities() {
		return entities;
	}

	public SpellFilter getFilter() {
		return filter;
	}

	public void setFilter(SpellFilter filter) {
		this.filter = filter;
	}

}
