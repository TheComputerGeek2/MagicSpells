package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class EnderSignalEffect extends JutsuEffect {

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		// TODO make a config loading schema
	}

	@Override
	public Runnable playEffectLocation(Location location) {
		location.getWorld().playEffect(location, Effect.ENDER_SIGNAL, 0);
		return null;
	}

}
