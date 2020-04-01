package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;

public class SwitchJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private int switchBack;
	
	public SwitchJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		switchBack = getConfigInt("switch-back", 0);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity player, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) return noTarget(player);
			
			playJutsuEffects(player, target.getTarget());
			switchPlaces(player, target.getTarget());
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		switchPlaces(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private void switchPlaces(LivingEntity player, final LivingEntity target) {
		Location targetLoc = target.getLocation();
		Location casterLoc = player.getLocation();
		player.teleport(targetLoc);
		target.teleport(casterLoc);

		if (switchBack <= 0) return;

		MagicJutsus.scheduleDelayedTask(() -> {
			if (player.isDead() || target.isDead()) return;
			Location targetLoc1 = target.getLocation();
			Location casterLoc1 = player.getLocation();
			player.teleport(targetLoc1);
			target.teleport(casterLoc1);
		}, switchBack);
	}

}
