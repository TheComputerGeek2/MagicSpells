package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.TargetBooleanState;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class EntitySilenceSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private TargetBooleanState targetBooleanState;
	
	public EntitySilenceSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		targetBooleanState = TargetBooleanState.getFromName(getConfigString("target-state", "toggle"));
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power, args);
			if (targetInfo == null) return noTarget(caster);

			LivingEntity target = targetInfo.getTarget();
			if (target == null) return noTarget(caster);
			target.setSilent(targetBooleanState.getBooleanState(target.isSilent()));
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		target.setSilent(targetBooleanState.getBooleanState(target.isSilent()));
		return true;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		target.setSilent(targetBooleanState.getBooleanState(target.isSilent()));
		return true;
	}
	
}
