package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.function.Supplier;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;

public class ClaritySpell extends BuffSpell {

	private final Map<UUID, Supplier<Float>> entities;

	private final ConfigData<Float> multiplier;

	private final ConfigData<Boolean> constantMultiplier;
	private final ConfigData<Boolean> powerAffectsMultiplier;

	private SpellFilter filter;

	public ClaritySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		multiplier = getConfigDataFloat("multiplier", 0.5F);

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
			float multiplier = this.multiplier.get(data);
			if (powerAffectsMultiplier.get(data)) {
				if (multiplier > 1) multiplier *= data.power();
				else if (multiplier < 1) multiplier /= data.power();
			}

			float finalMultiplier = multiplier;
			supplier = () -> finalMultiplier;
		} else supplier = () -> {
			float multiplier = this.multiplier.get(data);
			if (powerAffectsMultiplier.get(data)) {
				if (multiplier > 1) multiplier *= data.power();
				else if (multiplier < 1) multiplier /= data.power();
			}

			return multiplier;
		};

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
	protected void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		entities.clear();
	}

	@EventHandler(ignoreCancelled = true)
	public void onSpellCast(SpellCastEvent event) {
		LivingEntity caster = event.getCaster();
		if (!isActive(caster) || !filter.check(event.getSpell())) return;

		float multiplier = entities.get(caster.getUniqueId()).get();

		SpellReagents reagents = event.getReagents();
		if (reagents != null) event.setReagents(reagents.multiply(multiplier));

		addUseAndChargeCost(caster);
	}

	public SpellFilter getFilter() {
		return filter;
	}

	public void setFilter(SpellFilter filter) {
		this.filter = filter;
	}

}
