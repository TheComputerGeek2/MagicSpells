package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class SwitchHealthSpell extends TargetedSpell implements TargetedEntitySpell {

	private boolean requireLesserHealthPercent;
	private boolean requireGreaterHealthPercent;

	public SwitchHealthSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		requireLesserHealthPercent = getConfigBoolean("require-lesser-health-percent", false);
		requireGreaterHealthPercent = getConfigBoolean("require-greater-health-percent", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power, args);
			if (target == null) return noTarget(caster);

			boolean ok = switchHealth(caster, target.getTarget());
			if (!ok) return noTarget(caster);

			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return switchHealth(caster, target);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private boolean switchHealth(LivingEntity caster, LivingEntity target) {
		if (caster.isDead() || target.isDead()) return false;
		double casterPct = caster.getHealth() / Util.getMaxHealth(caster);
		double targetPct = target.getHealth() / Util.getMaxHealth(target);
		if (requireGreaterHealthPercent && casterPct < targetPct) return false;
		if (requireLesserHealthPercent && casterPct > targetPct) return false;
		caster.setHealth(targetPct * Util.getMaxHealth(caster));
		target.setHealth(casterPct * Util.getMaxHealth(target));
		playSpellEffects(caster, target);
		return true;
	}

}
