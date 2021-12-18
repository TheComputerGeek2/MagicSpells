package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class RiptideSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConfigData<Integer> duration;

	public RiptideSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		duration = getConfigDataInt("duration", 40);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state != SpellCastState.NORMAL) return PostCastAction.HANDLE_NORMALLY;

		TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
		if (target == null) return noTarget(caster);
		playSpellEffects(caster, target.getTarget());

		MagicSpells.getVolatileCodeHandler().startAutoSpinAttack(target.getTarget(), duration.get(caster, null, target.getPower(), args));
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		MagicSpells.getVolatileCodeHandler().startAutoSpinAttack(target, duration.get(caster, target, power, args));
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		MagicSpells.getVolatileCodeHandler().startAutoSpinAttack(target, duration.get(caster, target, power, null));
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		MagicSpells.getVolatileCodeHandler().startAutoSpinAttack(target, duration.get(null, target, power, args));
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		MagicSpells.getVolatileCodeHandler().startAutoSpinAttack(target, duration.get(null, target, power, null));
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

}
