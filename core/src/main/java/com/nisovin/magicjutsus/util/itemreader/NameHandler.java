package com.nisovin.magicjutsus.util.itemreader;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;

public class NameHandler {

	public static ItemMeta process(ConfigurationSection config, ItemMeta meta) {
		if (!config.contains("name") || !config.isString("name")) return meta;
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("name")));
		return meta;
	}
	
}
