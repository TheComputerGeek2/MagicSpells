package com.nisovin.magicjutsus.util.itemreader;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;

public class LoreHandler {

	public static ItemMeta process(ConfigurationSection config, ItemMeta meta) {
		if (!config.contains("lore")) return meta;
		if (config.isList("lore")) {
			List<String> lore = config.getStringList("lore");
			for (int i = 0; i < lore.size(); i++) {
				lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
			}
			meta.setLore(lore);
		} else if (config.isString("lore")) {
			List<String> lore = new ArrayList<>();
			lore.add(ChatColor.translateAlternateColorCodes('&', config.getString("lore")));
			meta.setLore(lore);
		}
		return meta;
	}
	
}
