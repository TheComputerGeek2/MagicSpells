package com.nisovin.magicspells.util.config;

import org.bukkit.entity.LivingEntity;

public interface ConfigData<T> {

	T get(LivingEntity caster, LivingEntity target, float power, String[] args);

	default boolean isConstant() {
		return true;
	}

	default boolean isTargeted() {
		return false;
	}

}
