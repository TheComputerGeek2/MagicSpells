package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.TargetBooleanState;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class GlideSpell extends TargetedSpell implements TargetedEntitySpell{
	
	private TargetBooleanState targetState;
	
	public GlideSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		targetState = TargetBooleanState.getFromName(getConfigString("target-state", "toggle"));
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power, args);
			if (targetInfo == null) return noTarget(caster);
			LivingEntity target = targetInfo.getTarget();

			if (target == null) return noTarget(caster);
			target.setGliding(targetState.getBooleanState(target.isGliding()));
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		target.setGliding(targetState.getBooleanState(target.isGliding()));
		return true;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(null, target, power);
	}
	
}
