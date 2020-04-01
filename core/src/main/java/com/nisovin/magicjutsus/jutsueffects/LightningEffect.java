package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class LightningEffect extends JutsuEffect {

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		// TODO make a config loading schema
	}

	@Override
	public Runnable playEffectLocation(Location location) {
		location.getWorld().strikeLightningEffect(location);
		return null;
	}

}
