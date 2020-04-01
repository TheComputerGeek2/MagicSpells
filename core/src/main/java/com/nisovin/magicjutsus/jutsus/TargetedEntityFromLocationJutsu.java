package com.nisovin.magicjutsus.jutsus;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public interface TargetedEntityFromLocationJutsu {
	
	boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power);
	
	boolean castAtEntityFromLocation(Location from, LivingEntity target, float power);

}
