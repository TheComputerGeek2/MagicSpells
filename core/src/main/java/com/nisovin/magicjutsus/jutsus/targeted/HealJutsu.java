package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.util.ValidTargetChecker;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.events.MagicJutsusEntityRegainHealthEvent;

public class HealJutsu extends TargetedJutsu implements TargetedEntityJutsu {
	
	private double healAmount;

	private boolean checkPlugins;
	private boolean cancelIfFull;

	private String strMaxHealth;

	private ValidTargetChecker checker;

	public HealJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		healAmount = getConfigFloat("heal-amount", 10);

		checkPlugins = getConfigBoolean("check-plugins", true);
		cancelIfFull = getConfigBoolean("cancel-if-full", true);

		strMaxHealth = getConfigString("str-max-health", "%t is already at max health.");

		checker = (LivingEntity entity) -> entity.getHealth() < Util.getMaxHealth(entity);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power, checker);
			if (targetInfo == null) return noTarget(livingEntity);
			LivingEntity target = targetInfo.getTarget();
			power = targetInfo.getPower();
			if (cancelIfFull && target.getHealth() == Util.getMaxHealth(target)) return noTarget(livingEntity, formatMessage(strMaxHealth, "%t", getTargetName(target)));
			boolean healed = heal(livingEntity, target, power);
			if (!healed) return noTarget(livingEntity);
			sendMessages(livingEntity, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (validTargetList.canTarget(caster, target) && target.getHealth() < Util.getMaxHealth(target)) return heal(caster, target, power);
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (validTargetList.canTarget(target) && target.getHealth() < Util.getMaxHealth(target)) return heal(null, target, power);
		return false;
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return checker;
	}
	
	private boolean heal(LivingEntity livingEntity, LivingEntity target, float power) {
		double health = target.getHealth();
		double amt = healAmount * power;

		if (checkPlugins) {
			MagicJutsusEntityRegainHealthEvent evt = new MagicJutsusEntityRegainHealthEvent(target, amt, RegainReason.CUSTOM);
			EventUtil.call(evt);
			if (evt.isCancelled()) return false;
			amt = evt.getAmount();
		}

		health += amt;
		if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
		target.setHealth(health);

		if (livingEntity == null) playJutsuEffects(EffectPosition.TARGET, target);
		else playJutsuEffects(livingEntity, target);
		return true;
	}

}
