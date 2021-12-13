package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class ModifyCooldownSpell extends TargetedSpell implements TargetedEntitySpell {

	private final SpellFilter filter;
	
	private final float seconds;
	private final float multiplier;
	
	public ModifyCooldownSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		seconds = getConfigFloat("seconds", 1F);
		multiplier = getConfigFloat("multiplier", 0F);

		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> tagList = getConfigStringList("spell-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-spell-tags", null);
		filter = new SpellFilter(spells, deniedSpells, tagList, deniedTagList);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
			if (target == null) return noTarget(caster);
			modifyCooldowns(caster, target.getTarget(), target.getPower(), args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		modifyCooldowns(caster, target, power, args);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		modifyCooldowns(null, target, power, args);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	private void modifyCooldowns(LivingEntity caster, LivingEntity target, float power, String[] args) {
		float sec = seconds * power;
		float mult = multiplier * (1F / power);

		for (Spell spell : MagicSpells.spells()) {
			if (!spell.onCooldown(target)) continue;
			if (!filter.check(spell)) continue;

			float cd = spell.getCooldown(target) - sec;
			if (mult > 0) cd *= mult;
			if (cd < 0) cd = 0;
			spell.setCooldown(target, cd, false);
		}
	}

}
