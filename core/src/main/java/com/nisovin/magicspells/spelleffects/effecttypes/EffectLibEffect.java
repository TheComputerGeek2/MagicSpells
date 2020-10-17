package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.SpellEffect;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;

public class EffectLibEffect extends SpellEffect {

	protected ConfigurationSection effectLibSection;
	protected EffectManager manager;
	protected String className;
	
	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		effectLibSection = config.getConfigurationSection("effectlib");
		manager = MagicSpells.getEffectManager();
		className = effectLibSection.getString("class");
	}

	@Override
	protected Runnable playEffectLocation(Location location) {
		updateManager();
		manager.start(className, effectLibSection, location);
		return null;
	}

	@Override
	protected Effect playEffectLibLocation(Location location) {
		updateManager();
		return manager.start(className, effectLibSection, location);
	}

	protected void updateManager() {
		if (manager == null || manager.isDisposed()) manager = MagicSpells.getEffectManager();
	}
	
}
