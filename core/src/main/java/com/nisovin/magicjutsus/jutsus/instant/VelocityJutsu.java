package com.nisovin.magicjutsus.jutsus.instant;

import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class VelocityJutsu extends InstantJutsu {
	
	private double speed;
	private boolean addVelocityInstead;
	
	public VelocityJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		speed = getConfigFloat("speed", 40) / 10F;

		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Vector v = livingEntity.getEyeLocation().getDirection().normalize().multiply(speed * power);
			if (!addVelocityInstead) livingEntity.setVelocity(v);
			else livingEntity.setVelocity(livingEntity.getVelocity().add(v));
			playJutsuEffects(EffectPosition.CASTER, livingEntity);
		}
		
		return PostCastAction.HANDLE_NORMALLY;
	}
	
}
