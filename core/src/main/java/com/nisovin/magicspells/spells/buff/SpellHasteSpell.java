package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;

public class SpellHasteSpell extends BuffSpell {

	private final Map<UUID, SpellData> entities;

	private ConfigData<Float> castTimeModAmt;
	private ConfigData<Float> cooldownModAmt;

	private boolean powerAffectsCastTimeModAmt;
	private boolean powerAffectsCooldownModAmt;

	private SpellFilter filter;

	public SpellHasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		castTimeModAmt = getConfigDataFloat("cast-time-mod-amt", -25);
		cooldownModAmt = getConfigDataFloat("cooldown-mod-amt", -25);

		powerAffectsCastTimeModAmt = getConfigBoolean("power-affects-cast-time-mod-amt", true);
		powerAffectsCooldownModAmt = getConfigBoolean("power-affects-cooldown-mod-amt", true);

		entities = new HashMap<>();

		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> tagList = getConfigStringList("spell-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-spell-tags", null);

		filter = new SpellFilter(spells, deniedSpells, tagList, deniedTagList);
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
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		entities.clear();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellSpeedCast(SpellCastEvent event) {
		if (!filter.check(event.getSpell())) return;

		LivingEntity caster = event.getCaster();
		if (!isActive(caster)) return;

		SpellData data = entities.get(event.getCaster().getUniqueId());
		if (data == null) return;

		float castTimeModAmt = this.castTimeModAmt.get(caster, null, data.power(), data.args()) / 100f;
		if (castTimeModAmt != 0) {
			int ct = event.getCastTime();

			float newCT = ct + (powerAffectsCastTimeModAmt ? castTimeModAmt * ct * data.power() : castTimeModAmt * ct);
			if (newCT < 0) newCT = 0;

			event.setCastTime(Math.round(newCT));
		}

		float cooldownModAmt = this.cooldownModAmt.get(caster, null, data.power(), data.args()) / 100f;
		if (cooldownModAmt != 0) {
			float cd = event.getCooldown();

			float newCD = cd + (powerAffectsCooldownModAmt ? cooldownModAmt * cd * data.power() : cooldownModAmt * cd);
			if (newCD < 0) newCD = 0;

			event.setCooldown(newCD);
		}
	}

	public Map<UUID, SpellData> getEntities() {
		return entities;
	}

	public SpellFilter getFilter() {
		return filter;
	}

	public void setFilter(SpellFilter filter) {
		this.filter = filter;
	}

}
