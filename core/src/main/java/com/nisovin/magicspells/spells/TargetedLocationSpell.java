package com.nisovin.magicspells.spells;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public interface TargetedLocationSpell {

	default boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		return castAtLocation(caster, target, power);
	}

	default boolean castAtLocation(Location target, float power, String[] args) {
		return castAtLocation(target, power);
	}

	@Deprecated
	boolean castAtLocation(LivingEntity caster, Location target, float power);

	@Deprecated
	boolean castAtLocation(Location target, float power);

}
