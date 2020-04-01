package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.events.JutsuApplyDamageEvent;

public class PotionEffectJutsu extends TargetedJutsu implements TargetedEntityJutsu {
	
	private PotionEffectType type;

	private int duration;
	private int strength;

	private boolean hidden;
	private boolean ambient;
	private boolean targeted;
	private boolean jutsuPowerAffectsDuration;
	private boolean jutsuPowerAffectsStrength;
	
	public PotionEffectJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		type = Util.getPotionEffectType(getConfigString("type", "1"));

		duration = getConfigInt("duration", 0);
		strength = getConfigInt("strength", 0);

		hidden = getConfigBoolean("hidden", false);
		ambient = getConfigBoolean("ambient", false);
		targeted = getConfigBoolean("targeted", false);
		jutsuPowerAffectsDuration = getConfigBoolean("jutsu-power-affects-duration", true);
		jutsuPowerAffectsStrength = getConfigBoolean("jutsu-power-affects-strength", true);
	}
	
	public PotionEffectType getPotionType() {
		return type;
	}
	
	public int getDuration() {
		return duration;
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			LivingEntity target = null;
			if (targeted) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
				if (targetInfo != null) {
					target = targetInfo.getTarget();
					power = targetInfo.getPower();
				}
			} else target = livingEntity;

			if (target == null) return noTarget(livingEntity);

			int dur = jutsuPowerAffectsDuration ? Math.round(duration * power) : duration;
			int str = jutsuPowerAffectsStrength ? Math.round(strength * power) : strength;
			
			applyPotionEffect(livingEntity, target, new PotionEffect(type, dur, str, ambient, !hidden));
			if (targeted) playJutsuEffects(livingEntity, target);
			else playJutsuEffects(EffectPosition.CASTER, livingEntity);

			sendMessages(livingEntity, target);
			return PostCastAction.NO_MESSAGES;
		}		
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		int dur = jutsuPowerAffectsDuration ? Math.round(duration * power) : duration;
		int str = jutsuPowerAffectsStrength ? Math.round(strength * power) : strength;
		PotionEffect effect = new PotionEffect(type, dur, str, ambient, !hidden);
		if (targeted) {
			applyPotionEffect(caster, target, effect);
			playJutsuEffects(caster, target);
		} else {
			applyPotionEffect(caster, caster, effect);
			playJutsuEffects(EffectPosition.CASTER, caster);
		}
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		int dur = jutsuPowerAffectsDuration ? Math.round(duration * power) : duration;
		int str = jutsuPowerAffectsStrength ? Math.round(strength * power) : strength;
		PotionEffect effect = new PotionEffect(type, dur, str, ambient, !hidden);
		applyPotionEffect(null, target, effect);
		playJutsuEffects(EffectPosition.TARGET, target);
		return true;
	}

	private void applyPotionEffect(LivingEntity caster, LivingEntity target, PotionEffect effect) {
		DamageCause cause = null;
		if (effect.getType() == PotionEffectType.POISON) cause = DamageCause.POISON;
		else if (effect.getType() == PotionEffectType.WITHER) cause = DamageCause.WITHER;
		if (cause != null) EventUtil.call(new JutsuApplyDamageEvent(this, caster, target, effect.getAmplifier(), cause, ""));
		target.addPotionEffect(effect, true);
	}

}
