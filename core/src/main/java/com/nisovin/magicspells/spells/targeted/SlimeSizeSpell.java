package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Slime;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class SlimeSizeSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final ValidTargetChecker SLIME = entity -> entity instanceof Slime;

	private final VariableMod variableMod;

	private final ConfigData<Integer> minSize;
	private final ConfigData<Integer> maxSize;

	public SlimeSizeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String size = getConfigString("size", "=5");
		variableMod = new VariableMod(size);

		minSize = getConfigDataInt("min-size", 0);
		maxSize = getConfigDataInt("max-size", 20);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data, SLIME);
		if (info.noTarget()) return noTarget(info);

		return setSize((Slime) data.target(), info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!SLIME.isValidTarget(data.target())) return noTarget(data);

		return setSize((Slime) data.target(), data);
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return SLIME;
	}

	private CastResult setSize(Slime target, SpellData data) {
		int minSize = this.minSize.get(data);
		int maxSize = this.maxSize.get(data);

		if (minSize < 0) minSize = 0;
		if (maxSize < minSize) maxSize = minSize;

		double rawOutputValue = variableMod.getValue(data, target.getSize());
		int finalSize = Math.min(Math.max((int) rawOutputValue, minSize), maxSize);
		target.setSize(finalSize);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
