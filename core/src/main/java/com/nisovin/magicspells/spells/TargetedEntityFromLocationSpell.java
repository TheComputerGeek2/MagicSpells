package com.nisovin.magicspells.spells;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public interface TargetedEntityFromLocationSpell {

	default boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, String[] args) {
		return castAtEntityFromLocation(caster, from, target, power);
	}

	default boolean castAtEntityFromLocation(Location from, LivingEntity target, float power, String[] args) {
		return castAtEntityFromLocation(from, target, power);
	}

	@Deprecated
	boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power);

	@Deprecated
	boolean castAtEntityFromLocation(Location from, LivingEntity target, float power);

}
