package com.nisovin.magicjutsus.jutsus;

import org.bukkit.entity.LivingEntity;

public interface TargetedEntityJutsu {
	
	boolean castAtEntity(LivingEntity caster, LivingEntity target, float power);
	
	boolean castAtEntity(LivingEntity target, float power);
	
}
