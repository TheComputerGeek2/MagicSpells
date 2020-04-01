package com.nisovin.magicjutsus.jutsus;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public interface TargetedLocationJutsu {
	
	boolean castAtLocation(LivingEntity caster, Location target, float power);

	boolean castAtLocation(Location target, float power);
	
}
