package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class ModifyCooldownSpell extends TargetedSpell implements TargetedEntitySpell {

	private SpellFilter filter;

	private final ConfigData<Float> seconds;
	private final ConfigData<Float> multiplier;

	private final ConfigData<Boolean> powerAffectsSeconds;
	private final ConfigData<Boolean> powerAffectsMultiplier;

	public ModifyCooldownSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		seconds = getConfigDataFloat("seconds", 1F);
		multiplier = getConfigDataFloat("multiplier", 0F);

		powerAffectsSeconds = getConfigDataBoolean("power-affects-seconds", true);
		powerAffectsMultiplier = getConfigDataBoolean("power-affects-multiplier", true);
	}

	@Override
	protected void initialize() {
		super.initialize();

		filter = getConfigSpellFilter();
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		float sec = seconds.get(data);
		if (powerAffectsSeconds.get(data)) sec *= data.power();

		float mult = multiplier.get(data);
		if (powerAffectsMultiplier.get(data)) mult /= data.power();

		for (Spell spell : MagicSpells.spells()) {
			if (!spell.onCooldown(data.target()) || !filter.check(spell)) continue;

			float cd = spell.getCooldown(data.target()) - sec;
			if (mult > 0) cd *= mult;
			if (cd < 0) cd = 0;
			spell.setCooldown(data.target(), cd, false);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
