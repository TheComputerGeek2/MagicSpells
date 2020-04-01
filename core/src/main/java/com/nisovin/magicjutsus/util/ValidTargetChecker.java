package com.nisovin.magicjutsus.util;

import org.bukkit.entity.LivingEntity;

@FunctionalInterface
public interface ValidTargetChecker {

	boolean isValidTarget(LivingEntity entity);

}
