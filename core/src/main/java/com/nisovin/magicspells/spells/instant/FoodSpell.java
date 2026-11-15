package com.nisovin.magicspells.spells.instant;

import org.bukkit.entity.HumanEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;

public class FoodSpell extends InstantSpell {

	private final ConfigData<Integer> food;

	private final ConfigData<Float> saturation;
	private final ConfigData<Float> maxSaturation;

	public FoodSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		food = getConfigDataInt("food", 4);
		saturation = getConfigDataFloat("saturation", 2.5F);
		maxSaturation = getConfigDataFloat("max-saturation", 0F);
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof HumanEntity caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		int food = Math.clamp(caster.getFoodLevel() + this.food.get(data), 0, 20);
		caster.setFoodLevel(food);

		float maxSaturation = this.maxSaturation.get(data);
		float saturation = Math.clamp(this.saturation.get(data), 0, maxSaturation);
		caster.setSaturation(saturation);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
