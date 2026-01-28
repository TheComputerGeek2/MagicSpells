package com.nisovin.magicspells.util.itemreader;

import net.kyori.adventure.text.Component;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.NAME;

public class NameHandler {

	private static final String CONFIG_NAME = NAME.toString();

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
		if (!config.isString(CONFIG_NAME)) return;
		meta.displayName(Util.getItemMiniMessage(config.getString(CONFIG_NAME)));
		data.setAttribute(NAME, meta.displayName());
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!data.hasAttribute(NAME)) return;
		meta.displayName((Component) data.getAttribute(NAME));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (!meta.hasDisplayName()) return;
		data.setAttribute(NAME, meta.displayName());
	}
	
}
