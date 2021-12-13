package com.nisovin.magicspells.spells;

import org.bukkit.entity.LivingEntity;

public interface TargetedEntitySpell {

	default boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		return castAtEntity(caster, target, power);
	}

	default boolean castAtEntity(LivingEntity target, float power, String[] args) {
		return castAtEntity(target, power);
	}

	@Deprecated
	boolean castAtEntity(LivingEntity caster, LivingEntity target, float power);

	@Deprecated
	boolean castAtEntity(LivingEntity target, float power);

}
