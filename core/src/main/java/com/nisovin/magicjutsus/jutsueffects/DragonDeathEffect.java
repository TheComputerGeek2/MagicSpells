package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.MagicJutsus;

public class DragonDeathEffect extends JutsuEffect {

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		//TODO make a config loading schema
	}

	@Override
	public Runnable playEffectLocation(Location location) {
		// TODO non volatile
		MagicJutsus.getVolatileCodeHandler().playDragonDeathEffect(location);
		return null;
	}

}
