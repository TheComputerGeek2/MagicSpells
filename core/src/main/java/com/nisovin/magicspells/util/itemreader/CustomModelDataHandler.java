package com.nisovin.magicspells.util.itemreader;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

public class CustomModelDataHandler {

	public static ItemMeta process(ConfigurationSection config, ItemMeta meta) {
		if (!config.contains("custommodeldata") || !config.isInt("custommodeldata")) return meta;
		meta.setCustomModelData(config.getInt("custommodeldata"));
		return meta;
	}
	
}
