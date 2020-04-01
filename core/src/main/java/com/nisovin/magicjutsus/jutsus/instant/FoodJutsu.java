package com.nisovin.magicjutsus.jutsus.instant;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class FoodJutsu extends InstantJutsu {

	private int food;
	private float saturation;
	private float maxSaturation;
	
	public FoodJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		food = getConfigInt("food", 4);
		saturation = getConfigFloat("saturation", 2.5F);
		maxSaturation = getConfigFloat("max-saturation", 0F);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			int f = player.getFoodLevel() + food;
			if (f > 20) f = 20;
			player.setFoodLevel(f);
			
			float s = player.getSaturation() + saturation;
			if (maxSaturation > 0 && saturation > maxSaturation) saturation = maxSaturation;
			player.setSaturation(s);
			playJutsuEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
