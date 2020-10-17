package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.spelleffects.SpellEffect;

public class SoundEffect extends SpellEffect {

	private String sound;
	private float pitch;
	private float volume;
	private SoundCategory category;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		sound = config.getString("sound", "entity.llama.spit");
		pitch = (float) config.getDouble("pitch", 1.0F);
		volume = (float) config.getDouble("volume", 1.0F);
		try {
			category = SoundCategory.valueOf(config.getString("category", "master").toUpperCase());
		}
		catch (IllegalArgumentException ignored) {
			category = SoundCategory.MASTER;
		}
	}

	@Override
	public Runnable playEffectLocation(Location location) {
		World world = location.getWorld();
		if (world == null) return null;
		world.playSound(location, sound, category, volume, pitch);
		return null;
	}

}
