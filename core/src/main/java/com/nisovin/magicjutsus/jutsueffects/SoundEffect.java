package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class SoundEffect extends JutsuEffect {

	private String sound;

	private float pitch;
	private float volume;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		sound = config.getString("sound", "entity.llama.spit");
		pitch = (float) config.getDouble("pitch", 1.0F);
		volume = (float) config.getDouble("volume", 1.0F);
	}
	
	@Override
	public Runnable playEffectLocation(Location location) {
		location.getWorld().playSound(location, sound, volume, pitch);
		return null;
	}
	
}
