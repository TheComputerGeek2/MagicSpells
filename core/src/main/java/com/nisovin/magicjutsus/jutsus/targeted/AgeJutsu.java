package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;

public class AgeJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private int rawAge;
	private boolean setMaturity;
	private boolean applyAgeLock;

	public AgeJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		rawAge = getConfigInt("age", 0);
		setMaturity = getConfigBoolean("set-maturity", true);
		applyAgeLock = getConfigBoolean("apply-age-lock", false);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> targetEntityInfo = getTargetedEntity(livingEntity, power);
			if (targetEntityInfo == null || targetEntityInfo.getTarget() == null) return noTarget(livingEntity);
			if (!(targetEntityInfo.getTarget() instanceof Ageable)) return noTarget(livingEntity);

			Ageable a = (Ageable) targetEntityInfo.getTarget();
			applyAgeChanges(a);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(target instanceof Ageable)) return false;
		applyAgeChanges((Ageable) target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(null, target, power);
	}

	private void applyAgeChanges(Ageable a) {
		if (setMaturity) a.setAge(rawAge);
		if (applyAgeLock) a.setAgeLock(true);
	}

}
