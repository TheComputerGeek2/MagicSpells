package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.Breedable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class AgeSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConfigData<Integer> rawAge;
	private boolean setMaturity;
	private boolean applyAgeLock;

	public AgeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		rawAge = getConfigDataInt("age", 0);
		setMaturity = getConfigBoolean("set-maturity", true);
		applyAgeLock = getConfigBoolean("apply-age-lock", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetEntityInfo = getTargetedEntity(caster, power);
			if (targetEntityInfo == null || targetEntityInfo.getTarget() == null) return noTarget(caster);

			LivingEntity target = targetEntityInfo.getTarget();
			if (!(target instanceof Ageable ageable)) return noTarget(caster);
			applyAgeChanges(caster, target, ageable, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!(target instanceof Ageable ageable)) return false;
		applyAgeChanges(caster, target, ageable, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		return castAtEntity(null, target, power, args);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(null, target, power, null);
	}

	private void applyAgeChanges(LivingEntity caster, LivingEntity target, Ageable ageable, float power, String[] args) {
		if (setMaturity) ageable.setAge(rawAge.get(caster, target, power, args));
		((Breedable) ageable).setAgeLock(applyAgeLock);
	}

}
