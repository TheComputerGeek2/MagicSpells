package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.entity.Slime;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.VariableMod;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.ValidTargetChecker;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;

public class SlimeSizeJutsu extends TargetedJutsu implements TargetedEntityJutsu {
	
	private VariableMod variableMod;

	private int minSize;
	private int maxSize;
	
	private static ValidTargetChecker isSlimeChecker = (LivingEntity entity) -> entity instanceof Slime;
	
	public SlimeSizeJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		variableMod = new VariableMod(getConfigString("size", "=5"));
		
		minSize = getConfigInt("min-size", 0);
		maxSize = getConfigInt("max-size", 20);

		if (minSize < 0) minSize = 0;
		if (maxSize < minSize) maxSize = minSize;
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);

			LivingEntity targetEntity = targetInfo.getTarget();
			setSize(livingEntity, targetEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		setSize(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		setSize(null, target);
		return true;
	}
	
	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return isSlimeChecker;
	}

	private void setSize(LivingEntity caster, LivingEntity target) {
		if (!(target instanceof Slime)) return;
		if (!(caster instanceof Player)) return;

		Slime slime = (Slime) target;
		double rawOutputValue = variableMod.getValue((Player) caster, null, slime.getSize());
		int finalSize = Util.clampValue(minSize, maxSize, (int) rawOutputValue);
		slime.setSize(finalSize);
	}

}
