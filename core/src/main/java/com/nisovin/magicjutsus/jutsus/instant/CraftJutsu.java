package com.nisovin.magicjutsus.jutsus.instant;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;

public class CraftJutsu extends InstantJutsu {

	public CraftJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			((Player) livingEntity).openWorkbench(null, true);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
