package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;

public class PotionEffectSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private PotionEffectType type;

	private int duration;
	private int strength;

	private boolean hidden;
	private boolean ambient;
	private boolean targeted;
	private boolean spellPowerAffectsDuration;
	private boolean spellPowerAffectsStrength;
	
	public PotionEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		type = Util.getPotionEffectType(getConfigString("type", "1"));

		duration = getConfigInt("duration", 0);
		strength = getConfigInt("strength", 0);

		hidden = getConfigBoolean("hidden", false);
		ambient = getConfigBoolean("ambient", false);
		targeted = getConfigBoolean("targeted", false);
		spellPowerAffectsDuration = getConfigBoolean("spell-power-affects-duration", true);
		spellPowerAffectsStrength = getConfigBoolean("spell-power-affects-strength", true);
	}
	
	public PotionEffectType getPotionType() {
		return type;
	}
	
	public int getDuration() {
		return duration;
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = null;
			if (targeted) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power);
				if (targetInfo != null) {
					target = targetInfo.getTarget();
					power = targetInfo.getPower();
				}
			} else target = caster;

			if (target == null) return noTarget(caster);

			int dur = spellPowerAffectsDuration ? Math.round(duration * power) : duration;
			int str = spellPowerAffectsStrength ? Math.round(strength * power) : strength;
			
			applyPotionEffect(caster, target, new PotionEffect(type, dur, str, ambient, !hidden));
			if (targeted) playSpellEffects(caster, target);
			else playSpellEffects(EffectPosition.CASTER, caster);

			sendMessages(caster, target, args);
			return PostCastAction.NO_MESSAGES;
		}		
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		int dur = spellPowerAffectsDuration ? Math.round(duration * power) : duration;
		int str = spellPowerAffectsStrength ? Math.round(strength * power) : strength;
		PotionEffect effect = new PotionEffect(type, dur, str, ambient, !hidden);
		if (targeted) {
			applyPotionEffect(caster, target, effect);
			playSpellEffects(caster, target);
		} else {
			applyPotionEffect(caster, caster, effect);
			playSpellEffects(EffectPosition.CASTER, caster);
		}
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		int dur = spellPowerAffectsDuration ? Math.round(duration * power) : duration;
		int str = spellPowerAffectsStrength ? Math.round(strength * power) : strength;
		PotionEffect effect = new PotionEffect(type, dur, str, ambient, !hidden);
		applyPotionEffect(null, target, effect);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	private void applyPotionEffect(LivingEntity caster, LivingEntity target, PotionEffect effect) {
		DamageCause cause = null;
		if (effect.getType() == PotionEffectType.POISON) cause = DamageCause.POISON;
		else if (effect.getType() == PotionEffectType.WITHER) cause = DamageCause.WITHER;
		if (cause != null) EventUtil.call(new SpellApplyDamageEvent(this, caster, target, effect.getAmplifier(), cause, ""));
		target.addPotionEffect(effect);
	}

}
