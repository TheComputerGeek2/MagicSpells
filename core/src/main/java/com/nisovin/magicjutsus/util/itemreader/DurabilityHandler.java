package com.nisovin.magicjutsus.util.itemreader;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.configuration.ConfigurationSection;

public class DurabilityHandler {

	public static ItemMeta process(ConfigurationSection config, ItemMeta meta) {
		if (!(meta instanceof Damageable)) return meta;
		if (!config.contains("damage")) return meta;
		if (!config.isInt("damage")) return meta;

		((Damageable) meta).setDamage(config.getInt("damage"));

		return meta;
	}

}
