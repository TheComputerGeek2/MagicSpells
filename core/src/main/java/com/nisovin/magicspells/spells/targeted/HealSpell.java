package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.ValidTargetChecker;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsEntityRegainHealthEvent;

public class HealSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Double> healAmount;
	private final ConfigData<Double> healPercent;

	private final boolean checkPlugins;
	private final boolean cancelIfFull;
	private final boolean powerAffectsHealAmount;

	private final String strMaxHealth;

	private final ValidTargetChecker checker;

	public HealSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		healAmount = getConfigDataDouble("heal-amount", 10);
		healPercent = getConfigDataDouble("heal-percent", 0);

		checkPlugins = getConfigBoolean("check-plugins", true);
		cancelIfFull = getConfigBoolean("cancel-if-full", true);
		powerAffectsHealAmount = getConfigBoolean("power-affects-heal-amount", true);

		strMaxHealth = getConfigString("str-max-health", "%t is already at max health.");

		checker = (LivingEntity entity) -> entity.getHealth() < Util.getMaxHealth(entity);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power, checker);
			if (targetInfo == null) return noTarget(caster);
			LivingEntity target = targetInfo.getTarget();
			power = targetInfo.getPower();
			if (cancelIfFull && target.getHealth() == Util.getMaxHealth(target)) return noTarget(caster, formatMessage(strMaxHealth, "%t", getTargetName(target)));
			boolean healed = heal(caster, target, power, args);
			if (!healed) return noTarget(caster);
			sendMessages(caster, target, args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (validTargetList.canTarget(caster, target) && target.getHealth() < Util.getMaxHealth(target))
			return heal(caster, target, power, args);

		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (validTargetList.canTarget(target) && target.getHealth() < Util.getMaxHealth(target))
			return heal(null, target, power, args);

		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return checker;
	}

	private boolean heal(LivingEntity caster, LivingEntity target, float power, String[] args) {
		double health = target.getHealth();
		double amount;

		double healPercent = this.healPercent.get(caster, target, power, args);
		if (healPercent == 0) {
			amount = this.healAmount.get(caster, target, power, args);
			if (powerAffectsHealAmount) amount *= power;
		} else amount = (Util.getMaxHealth(caster) - health) * (healPercent / 100);

		if (checkPlugins) {
			MagicSpellsEntityRegainHealthEvent event = new MagicSpellsEntityRegainHealthEvent(target, amount, RegainReason.CUSTOM);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
			amount = event.getAmount();
		}

		health += amount;
		if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
		target.setHealth(health);

		if (caster == null) playSpellEffects(EffectPosition.TARGET, target);
		else playSpellEffects(caster, target);
		return true;
	}

}
