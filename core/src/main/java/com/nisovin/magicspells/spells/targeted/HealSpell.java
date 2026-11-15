package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.MagicSpellsEntityRegainHealthEvent;

public class HealSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Double> healAmount;
	private final ConfigData<Double> healPercent;

	private final ConfigData<Boolean> cancelIfFull;
	private final ConfigData<Boolean> ignoreIfFull;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> powerAffectsHealAmount;

	private final String strMaxHealth;

	public HealSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		healAmount = getConfigDataDouble("heal-amount", 10);
		healPercent = getConfigDataDouble("heal-percent", 0);

		checkPlugins = getConfigDataBoolean("check-plugins", true);
		cancelIfFull = getConfigDataBoolean("cancel-if-full", true);
		ignoreIfFull = getConfigDataBoolean("ignore-if-full", false);
		powerAffectsHealAmount = getConfigDataBoolean("power-affects-heal-amount", true);

		strMaxHealth = getConfigString("str-max-health", "%t is already at max health.");
	}

	@Override
	public CastResult cast(SpellData data) {
		ValidTargetChecker checker = ignoreIfFull.get(data) ? e -> e.getHealth() < Util.getMaxHealth(e) : null;
		TargetInfo<LivingEntity> info = getTargetedEntity(data, checker);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		double maxHealth = Util.getMaxHealth(data.target());
		double health = data.target().getHealth();

		if (cancelIfFull.get(data) && health >= maxHealth)
			return noTarget(strMaxHealth, data);

		double healAmount;
		double healPercent = this.healPercent.get(data);
		if (healPercent == 0) {
			healAmount = this.healAmount.get(data);
			if (powerAffectsHealAmount.get(data)) healAmount *= data.power();
		} else healAmount = maxHealth * (healPercent / 100);

		if (checkPlugins.get(data)) {
			MagicSpellsEntityRegainHealthEvent event = new MagicSpellsEntityRegainHealthEvent(data.target(), healAmount, RegainReason.CUSTOM);
			if (!event.callEvent()) return noTarget(data);

			healAmount = event.getAmount();
		}

		data.target().setHealth(Math.clamp(health + healAmount, 0, maxHealth));
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
