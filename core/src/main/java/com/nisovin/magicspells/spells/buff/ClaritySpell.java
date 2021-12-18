package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;

public class ClaritySpell extends BuffSpell {

	private final Map<UUID, SpellData> entities;

	private ConfigData<Float> multiplier;

	private boolean powerAffectsMultiplier;

	private SpellFilter filter;

	public ClaritySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		multiplier = getConfigDataFloat("multiplier", 0.5F);

		powerAffectsMultiplier = getConfigBoolean("power-affects-multiplier", true);

		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> tagList = getConfigStringList("spell-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-spell-tags", null);
		filter = new SpellFilter(spells, deniedSpells, tagList, deniedTagList);

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		entities.put(entity.getUniqueId(), new SpellData(power, args));
		return true;
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
		if (!isActive(caster)) return;
		if (!filter.check(event.getSpell())) return;

		SpellData data = entities.get(caster.getUniqueId());

		float multiplier = this.multiplier.get(caster, null, data.power(), data.args());
		if (powerAffectsMultiplier) {
			if (multiplier < 1) multiplier /= data.power();
			else if (multiplier > 1) multiplier *= data.power();
		}

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
