package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Slime;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.VariableMod;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.ValidTargetChecker;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.config.ConfigData;

public class SlimeSizeSpell extends TargetedSpell implements TargetedEntitySpell {

	private VariableMod variableMod;

	private String size;

	private ConfigData<Integer> minSize;
	private ConfigData<Integer> maxSize;

	private static ValidTargetChecker isSlimeChecker = (LivingEntity entity) -> entity instanceof Slime;

	public SlimeSizeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		size = getConfigString("size", "=5");

		minSize = getConfigDataInt("min-size", 0);
		maxSize = getConfigDataInt("max-size", 20);
	}

	@Override
	public void initializeVariables() {
		super.initializeVariables();

		variableMod = new VariableMod(size);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power);
			if (targetInfo == null) return noTarget(caster);

			setSize(caster, targetInfo.getTarget(), targetInfo.getPower(), args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		setSize(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		setSize(caster, target, power, null);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		setSize(null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		setSize(null, target, power, null);
		return true;
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return isSlimeChecker;
	}

	private void setSize(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!(target instanceof Slime slime)) return;
		if (!(caster instanceof Player player)) return;

		int minSize = this.minSize.get(caster, target, power, args);
		int maxSize = this.maxSize.get(caster, target, power, args);

		if (minSize < 0) minSize = 0;
		if (maxSize < minSize) maxSize = minSize;

		double rawOutputValue = variableMod.getValue(player, null, slime.getSize());
		int finalSize = Util.clampValue(minSize, maxSize, (int) rawOutputValue);
		slime.setSize(finalSize);
	}

}
