package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.TargetBooleanState;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;

public class FlyJutsu extends TargetedJutsu implements TargetedEntityJutsu {
	
	private TargetBooleanState targetBooleanState;
	
	public FlyJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		targetBooleanState = TargetBooleanState.getFromName(getConfigString("target-state", "toggle"));
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);
			Player target = targetInfo.getTarget();
			if (target == null) return noTarget(livingEntity);

			target.setFlying(targetBooleanState.getBooleanState(target.isFlying()));
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(target instanceof Player)) return false;
		((Player) target).setFlying(targetBooleanState.getBooleanState(((Player) target).isFlying()));
		return true;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!(target instanceof Player)) return false;
		((Player) target).setFlying(targetBooleanState.getBooleanState(((Player) target).isFlying()));
		return true;
	}
	
}
