package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.events.JutsuApplyDamageEvent;
import com.nisovin.magicjutsus.events.MagicJutsusEntityDamageByEntityEvent;

public class CombustJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private Map<UUID, CombustData> combusting;

	private int fireTicks;
	private int fireTickDamage;

	private boolean checkPlugins;
	private boolean preventImmunity;

	public CombustJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		fireTicks = getConfigInt("fire-ticks", 100);
		fireTickDamage = getConfigInt("fire-tick-damage", 1);

		checkPlugins = getConfigBoolean("check-plugins", true);
		preventImmunity = getConfigBoolean("prevent-immunity", true);

		combusting = new HashMap<>();
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);
			boolean combusted = combust(livingEntity, target.getTarget(), target.getPower());
			if (!combusted) return noTarget(livingEntity);

			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return combust(caster, target, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		return combust(null, target, power);
	}
	
	private boolean combust(LivingEntity livingEntity, final LivingEntity target, float power) {
		if (checkPlugins && livingEntity != null) {
			MagicJutsusEntityDamageByEntityEvent event = new MagicJutsusEntityDamageByEntityEvent(livingEntity, target, DamageCause.ENTITY_ATTACK, 1);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
		}
		
		int duration = Math.round(fireTicks * power);
		combusting.put(target.getUniqueId(), new CombustData(power));

		EventUtil.call(new JutsuApplyDamageEvent(this, livingEntity, target, fireTickDamage, DamageCause.FIRE_TICK, ""));
		target.setFireTicks(duration);

		if (livingEntity != null) playJutsuEffects(livingEntity, target);
		else playJutsuEffects(EffectPosition.TARGET, target);

		MagicJutsus.scheduleDelayedTask(() -> {
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
		
		event.setDamage(Math.round(fireTickDamage * data.power));
		if (preventImmunity) MagicJutsus.scheduleDelayedTask(() -> ((LivingEntity) entity).setNoDamageTicks(0), 0);
	}

	public int getDuration() {
		return fireTicks;
	}
	
	private class CombustData {
		
		private float power;
		
		CombustData(float power) {
			this.power = power;
		}
		
	}
	
}
