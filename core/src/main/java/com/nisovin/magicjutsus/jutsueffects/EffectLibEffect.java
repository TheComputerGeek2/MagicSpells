package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.MagicJutsus;

import de.slikey.effectlib.EffectManager;

public class EffectLibEffect extends JutsuEffect {

	ConfigurationSection effectLibSection;
	EffectManager manager = MagicJutsus.getEffectManager();
	String className;
	
	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		effectLibSection = config.getConfigurationSection("effectlib");
		className = effectLibSection.getString("class");
	}

	@Override
	protected Runnable playEffectLocation(Location location) {
		manager.start(className, effectLibSection, location);
		return null;
	}
	
}
