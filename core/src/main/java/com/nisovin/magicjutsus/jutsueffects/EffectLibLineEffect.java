package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.configuration.ConfigurationSection;

// TODO use non deprecated methods
public class EffectLibLineEffect extends EffectLibEffect {

	private boolean forceStaticOriginLocation;
	private boolean forceStaticTargetLocation;
	
	@Override
	public void loadFromConfig(ConfigurationSection section) {
		super.loadFromConfig(section);
		forceStaticOriginLocation = section.getBoolean("static-origin-location", true);
		forceStaticTargetLocation = section.getBoolean("static-target-location", false);
	}
	
	@Override
	public Runnable playEffect(Location location1, Location location2) {
		manager.start(className, effectLibSection, location1, location2, null, null, null);
		return null;
	}
	
	@Override
	public void playTrackingLinePatterns(Location origin, Location target, Entity originEntity, Entity targetEntity) {
		if (forceStaticOriginLocation) {
			if (origin == null && originEntity != null) origin = originEntity.getLocation();
			originEntity = null;
		}
		if (forceStaticTargetLocation) {
			if (target == null && targetEntity != null) target = targetEntity.getLocation();
			targetEntity = null;
		}
		manager.start(className, effectLibSection, origin, target, originEntity, targetEntity, null);
	}
	
}
