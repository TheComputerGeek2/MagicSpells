package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class CombustSpell extends TargetedSpell implements TargetedEntitySpell {

	private Map<UUID, CombustData> combusting;

	private ConfigData<Integer> fireTicks;
	private ConfigData<Double> fireTickDamage;

	private boolean checkPlugins;
	private boolean preventImmunity;
	private boolean powerAffectsFireTicks;
	private boolean powerAffectsFireTickDamage;

	public CombustSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		fireTicks = getConfigDataInt("fire-ticks", 100);
		fireTickDamage = getConfigDataDouble("fire-tick-damage", 1);

		checkPlugins = getConfigBoolean("check-plugins", true);
		preventImmunity = getConfigBoolean("prevent-immunity", true);
		powerAffectsFireTicks = getConfigBoolean("power-affects-fire-ticks", true);
		powerAffectsFireTickDamage = getConfigBoolean("power-affects-fire-tick-damage", true);

		combusting = new HashMap<>();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
			if (target == null) return noTarget(caster);
			boolean combusted = combust(caster, target.getTarget(), target.getPower(), args);
			if (!combusted) return noTarget(caster);

			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return combust(caster, target, power, args);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		return combust(null, target, power, args);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	private boolean combust(LivingEntity caster, final LivingEntity target, float power, String[] args) {
		if (checkPlugins && caster != null) {
			MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(caster, target, DamageCause.ENTITY_ATTACK, 1, this);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
		}

		int duration = fireTicks.get(caster, target, power, args);
		if (powerAffectsFireTicks) duration = Math.round(duration * power);
		target.setFireTicks(duration);

		combusting.put(target.getUniqueId(), new CombustData(caster, power, args));

		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);

		MagicSpells.scheduleDelayedTask(() -> combusting.remove(target.getUniqueId()), duration + 2);

		return true;
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getCause() != DamageCause.FIRE_TICK) return;

		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity target)) return;

		CombustData data = combusting.get(target.getUniqueId());
		if (data == null) return;

		double fireTickDamage = this.fireTickDamage.get(data.caster, target, data.power, data.args);
		if (powerAffectsFireTickDamage) fireTickDamage = fireTickDamage * data.power;

		EventUtil.call(new SpellApplyDamageEvent(this, data.caster, target, fireTickDamage, DamageCause.FIRE_TICK, ""));
		event.setDamage(fireTickDamage);

		if (preventImmunity) MagicSpells.scheduleDelayedTask(() -> target.setNoDamageTicks(0), 0);
	}

	private record CombustData(LivingEntity caster, float power, String[] args) {}

}
