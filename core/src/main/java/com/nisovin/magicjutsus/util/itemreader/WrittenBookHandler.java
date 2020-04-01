package com.nisovin.magicjutsus.util.itemreader;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class WrittenBookHandler {

	public static ItemMeta process(ConfigurationSection config, ItemMeta meta) {
		if (!(meta instanceof BookMeta)) return meta;
		
		BookMeta bmeta = (BookMeta)meta;
		
		if (config.contains("title") && config.isString("title")) bmeta.setTitle(ChatColor.translateAlternateColorCodes('&', config.getString("title")));
		if (config.contains("author") && config.isString("author")) bmeta.setAuthor(ChatColor.translateAlternateColorCodes('&', config.getString("author")));
		if (config.contains("pages") && config.isList("pages")) {
			List<String> pages = config.getStringList("pages");
			for (int i = 0; i < pages.size(); i++) {
				pages.set(i, ChatColor.translateAlternateColorCodes('&', pages.get(i)));
			}
			bmeta.setPages(pages);
		}
		return bmeta;
	}
	
}
