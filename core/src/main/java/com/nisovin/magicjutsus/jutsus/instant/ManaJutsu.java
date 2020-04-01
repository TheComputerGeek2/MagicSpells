package com.nisovin.magicjutsus.jutsus.instant;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.mana.ManaChangeReason;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class ManaJutsu extends InstantJutsu {

	private int mana;
	
	public ManaJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		mana = getConfigInt("mana", 25);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			int amount = Math.round(mana * power);
			boolean added = MagicJutsus.getManaHandler().addMana(player, amount, ManaChangeReason.OTHER);
			if (!added) return PostCastAction.ALREADY_HANDLED;
			playJutsuEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
