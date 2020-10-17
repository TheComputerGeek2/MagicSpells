package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.configuration.ConfigurationSection;

import de.slikey.effectlib.util.DynamicLocation;

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
		updateManager();
		manager.start(className, effectLibSection, new DynamicLocation(location1), new DynamicLocation(location2), (ConfigurationSection) null, null);
		return null;
	}
	
	@Override
	public void playTrackingLinePatterns(Location origin, Location target, Entity originEntity, Entity targetEntity) {
		updateManager();
		if (forceStaticOriginLocation) {
			if (origin == null && originEntity != null) origin = originEntity.getLocation();
			originEntity = null;
		}
		if (forceStaticTargetLocation) {
			if (target == null && targetEntity != null) target = targetEntity.getLocation();
			targetEntity = null;
		}
		manager.start(className, effectLibSection, new DynamicLocation(origin, originEntity), new DynamicLocation(target, targetEntity), (ConfigurationSection) null, null);
	}
	
}
