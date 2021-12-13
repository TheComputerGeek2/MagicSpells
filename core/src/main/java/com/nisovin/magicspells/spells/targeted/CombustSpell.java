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
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class CombustSpell extends TargetedSpell implements TargetedEntitySpell {

	private Map<UUID, CombustData> combusting;

	private int fireTicks;
	private double fireTickDamage;

	private boolean checkPlugins;
	private boolean preventImmunity;

	public CombustSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		fireTicks = getConfigInt("fire-ticks", 100);
		fireTickDamage = getConfigDouble("fire-tick-damage", 1);

		checkPlugins = getConfigBoolean("check-plugins", true);
		preventImmunity = getConfigBoolean("prevent-immunity", true);

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
		
		int duration = Math.round(fireTicks * power);
		combusting.put(target.getUniqueId(), new CombustData(power));

		EventUtil.call(new SpellApplyDamageEvent(this, caster, target, fireTickDamage, DamageCause.FIRE_TICK, ""));
		target.setFireTicks(duration);

		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);

		MagicSpells.scheduleDelayedTask(() -> {
			CombustData data = combusting.get(target.getUniqueId());
			if (data != null) combusting.remove(target.getUniqueId());
		}, duration + 2);

		return true;
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(final EntityDamageEvent event) {
		if (event.getCause() != DamageCause.FIRE_TICK) return;
		
		final Entity entity = event.getEntity();
		CombustData data = combusting.get(entity.getUniqueId());
		if (data == null) return;
		
		event.setDamage(fireTickDamage * data.power);
		if (preventImmunity) MagicSpells.scheduleDelayedTask(() -> ((LivingEntity) entity).setNoDamageTicks(0), 0);
	}

	public int getDuration() {
		return fireTicks;
	}

	private record CombustData(float power) {}
	
}
