package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;

public class SkinJutsu extends TargetedJutsu implements TargetedEntityJutsu {
	
	private String texture;
	private String signature;
	
	public SkinJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		texture = getConfigString("texture", null);
		signature = getConfigString("signature", null);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(livingEntity, power);
			if (targetInfo == null || targetInfo.getTarget() == null) return noTarget(livingEntity);

			MagicJutsus.getVolatileCodeHandler().setSkin(targetInfo.getTarget(), texture, signature);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(target instanceof Player)) return false;
		MagicJutsus.getVolatileCodeHandler().setSkin((Player) target, texture, signature);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!(target instanceof Player)) return false;
		MagicJutsus.getVolatileCodeHandler().setSkin((Player) target, texture, signature);
		return true;
	}

}
