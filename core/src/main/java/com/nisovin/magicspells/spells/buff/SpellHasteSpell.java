package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;

public class SpellHasteSpell extends BuffSpell {

	private final Map<UUID, HasteData> entities;

	private final ConfigData<Float> castTimeModAmt;
	private final ConfigData<Float> cooldownModAmt;

	private final ConfigData<Boolean> constantCastTimeModAmt;
	private final ConfigData<Boolean> constantCooldownModAmt;
	private final ConfigData<Boolean> powerAffectsCastTimeModAmt;
	private final ConfigData<Boolean> powerAffectsCooldownModAmt;

	private SpellFilter filter;

	public SpellHasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		castTimeModAmt = getConfigDataFloat("cast-time-mod-amt", -25);
		cooldownModAmt = getConfigDataFloat("cooldown-mod-amt", -25);

		constantCastTimeModAmt = getConfigDataBoolean("constant-cast-time-mod-amt", true);
		constantCooldownModAmt = getConfigDataBoolean("constant-cooldown-mod-amt", true);
		powerAffectsCastTimeModAmt = getConfigDataBoolean("power-affects-cast-time-mod-amt", true);
		powerAffectsCooldownModAmt = getConfigDataBoolean("power-affects-cooldown-mod-amt", true);

		entities = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		filter = getConfigSpellFilter();
	}

	@Override
	public boolean castBuff(SpellData data) {
		boolean constantCastTimeModAmt = this.constantCastTimeModAmt.get(data);
		boolean constantCooldownModAmt = this.constantCooldownModAmt.get(data);

		float castTimeModAmt = 0;
		if (constantCastTimeModAmt) {
			castTimeModAmt = this.castTimeModAmt.get(data);
			if (powerAffectsCastTimeModAmt.get(data)) castTimeModAmt *= data.power();
			castTimeModAmt /= 100;
		}

		float cooldownModAmt = 0;
		if (constantCooldownModAmt) {
			cooldownModAmt = this.cooldownModAmt.get(data);
			if (powerAffectsCooldownModAmt.get(data)) cooldownModAmt *= data.power();
			cooldownModAmt /= 100;
		}

		entities.put(data.target().getUniqueId(), new HasteData(data, castTimeModAmt, cooldownModAmt, constantCastTimeModAmt, constantCooldownModAmt));

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

	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellSpeedCast(SpellCastEvent event) {
		if (!filter.check(event.getSpell())) return;

		LivingEntity caster = event.getCaster();
		HasteData data = entities.get(caster.getUniqueId());
		if (data == null) return;

		boolean modified = false;

		float castTimeModAmt = data.castTimeModAmt;
		if (!data.constantCastTimeModAmt) {
			castTimeModAmt = this.castTimeModAmt.get(data.spellData);
			if (powerAffectsCastTimeModAmt.get(data.spellData)) castTimeModAmt *= data.spellData.power();
			castTimeModAmt /= 100;
		}

		float cooldownModAmt = data.cooldownModAmt;
		if (!data.constantCooldownModAmt) {
			cooldownModAmt = this.cooldownModAmt.get(data.spellData);
			if (powerAffectsCooldownModAmt.get(data.spellData)) cooldownModAmt *= data.spellData.power();
			cooldownModAmt /= 100;
		}

		if (castTimeModAmt != 0) {
			int castTime = event.getCastTime();
			castTime = Math.max(Math.round(castTime + castTime * castTimeModAmt), 0);

			event.setCastTime(castTime);
			modified = true;
		}

		if (cooldownModAmt != 0) {
			float cooldown = event.getCooldown();
			cooldown = Math.max(cooldown + cooldown * cooldownModAmt, 0);

			event.setCooldown(cooldown);
			modified = true;
		}

		if (modified) addUseAndChargeCost(caster);
	}

	public Map<UUID, HasteData> getEntities() {
		return entities;
	}

	public SpellFilter getFilter() {
		return filter;
	}

	public void setFilter(SpellFilter filter) {
		this.filter = filter;
	}

	public record HasteData(SpellData spellData, float castTimeModAmt, float cooldownModAmt, boolean constantCastTimeModAmt, boolean constantCooldownModAmt) {
	}

}
