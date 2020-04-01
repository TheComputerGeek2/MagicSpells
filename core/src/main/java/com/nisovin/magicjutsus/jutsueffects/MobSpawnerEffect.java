package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class MobSpawnerEffect extends JutsuEffect {

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		//TODO make a config schema for this effect
	}

	@Override
	public Runnable playEffectLocation(Location location) {
		location.getWorld().playEffect(location, Effect.MOBSPAWNER_FLAMES, 0);
		return null;
	}
	
}
