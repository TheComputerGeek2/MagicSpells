package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.spelleffects.SpellEffect;

public class SoundPersonalEffect extends SpellEffect {

	private String sound;
	private float pitch;
	private float volume;
	private SoundCategory category;

	private boolean broadcast;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		sound = config.getString("sound", "entity.llama.spit");
		pitch = (float) config.getDouble("pitch", 1.0F);
		volume = (float) config.getDouble("volume", 1.0F);
		broadcast = config.getBoolean("broadcast", false);
		try {
			category = SoundCategory.valueOf(config.getString("category", "master").toUpperCase());
		}
		catch (IllegalArgumentException ignored) {
			category = SoundCategory.MASTER;
		}
	}

	@Override
	public Runnable playEffectEntity(Entity entity) {
		if (broadcast) Util.forEachPlayerOnline(this::send);
		else if (entity instanceof Player) send((Player) entity);
		return null;
	}
	
	private void send(Player player) {
		player.playSound(player.getLocation(), sound, category, volume, pitch);
	}
	
}
