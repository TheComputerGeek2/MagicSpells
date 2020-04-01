package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsus.JutsuDamageJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.events.JutsuApplyDamageEvent;
import com.nisovin.magicjutsus.events.MagicJutsusEntityDamageByEntityEvent;

public class DotJutsu extends TargetedJutsu implements TargetedEntityJutsu, JutsuDamageJutsu {

	private Map<UUID, Dot> activeDots;

	private int delay;
	private int interval;
	private int duration;

	private float damage;

	private boolean preventKnockback;

	private String jutsuDamageType;

	public DotJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		delay = getConfigInt("delay", 1);
		interval = getConfigInt("interval", 20);
		duration = getConfigInt("duration", 200);

		damage = getConfigFloat("damage", 2);

		preventKnockback = getConfigBoolean("prevent-knockback", false);

		jutsuDamageType = getConfigString("jutsu-damage-type", "");

		activeDots = new HashMap<>();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);
			applyDot(livingEntity, targetInfo.getTarget(), targetInfo.getPower());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		applyDot(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		applyDot(null, target, power);
		return true;
	}

	@Override
	public String getJutsuDamageType() {
		return jutsuDamageType;
	}

	public boolean isActive(LivingEntity entity) {
		return activeDots.containsKey(entity.getUniqueId());
	}

	public void cancelDot(LivingEntity entity) {
		if (!isActive(entity)) return;
		Dot dot = activeDots.get(entity.getUniqueId());
		dot.cancel();
	}
	
	private void applyDot(LivingEntity caster, LivingEntity target, float power) {
		Dot dot = activeDots.get(target.getUniqueId());
		if (dot != null) {
			dot.dur = 0;
			dot.power = power;
		} else {
			dot = new Dot(caster, target, power);
			activeDots.put(target.getUniqueId(), dot);
		}

		if (caster != null) playJutsuEffects(caster, target);
		else playJutsuEffects(EffectPosition.TARGET, target);
	}
	
	@EventHandler
	private void onDeath(PlayerDeathEvent event) {
		Dot dot = activeDots.get(event.getEntity().getUniqueId());
		if (dot != null) dot.cancel();
	}
	
	private class Dot implements Runnable {
		
		private LivingEntity caster;
		private LivingEntity target;
		private float power;

		private int taskId;
		private int dur = 0;

		private Dot(LivingEntity caster, LivingEntity target, float power) {
			this.caster = caster;
			this.target = target;
			this.power = power;
			taskId = MagicJutsus.scheduleRepeatingTask(this, delay, interval);
		}
		
		@Override
		public void run() {
			dur += interval;
			if (dur > duration) {
				cancel();
				return;
			}

			if (target.isDead() || !target.isValid()) {
				cancel();
				return;
			}

			double dam = damage * power;
			JutsuApplyDamageEvent event = new JutsuApplyDamageEvent(DotJutsu.this, caster, target, dam, DamageCause.MAGIC, jutsuDamageType);
			EventUtil.call(event);
			dam = event.getFinalDamage();

			if (preventKnockback) {
				MagicJutsusEntityDamageByEntityEvent devent = new MagicJutsusEntityDamageByEntityEvent(caster, target, DamageCause.ENTITY_ATTACK, damage);
				EventUtil.call(devent);
				if (!devent.isCancelled()) target.damage(devent.getDamage());
			} else target.damage(dam, caster);

			target.setNoDamageTicks(0);
			playJutsuEffects(EffectPosition.DELAYED, target);
		}

		private void cancel() {
			MagicJutsus.cancelTask(taskId);
			activeDots.remove(target.getUniqueId());
		}

	}

}
