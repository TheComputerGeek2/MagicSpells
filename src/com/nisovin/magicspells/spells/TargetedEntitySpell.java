package com.nisovin.magicspells.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public interface TargetedEntitySpell {
	
	public boolean castAtEntity(@Nullable Player caster, LivingEntity target, float power);
	
	public boolean castAtEntity(LivingEntity target, float power);
	
}
