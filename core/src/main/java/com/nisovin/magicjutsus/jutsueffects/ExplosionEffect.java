package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class ExplosionEffect extends JutsuEffect {

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		// TODO make a config loading schema
	}

	@Override
	public Runnable playEffectLocation(Location location) {
		location.getWorld().createExplosion(location, 0F);
		return null;
	}

}
