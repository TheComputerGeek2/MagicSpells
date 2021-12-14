package com.nisovin.magicspells.spells.instant;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class FoodSpell extends InstantSpell {

	private ConfigData<Integer> food;

	private ConfigData<Float> saturation;
	private ConfigData<Float> maxSaturation;
	
	public FoodSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		food = getConfigDataInt("food", 4);
		saturation = getConfigDataFloat("saturation", 2.5F);
		maxSaturation = getConfigDataFloat("max-saturation", 0F);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player player) {
			int f = Math.min(player.getFoodLevel() + food.get(caster, null, power, args), 20);
			player.setFoodLevel(f);

			float saturation = this.saturation.get(caster, null, power, args);
			float maxSaturation = this.maxSaturation.get(caster, null, power, args);

			float s = Math.min(player.getSaturation() + saturation, maxSaturation);
			player.setSaturation(s);

			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
