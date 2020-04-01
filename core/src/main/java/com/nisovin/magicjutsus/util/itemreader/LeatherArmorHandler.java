package com.nisovin.magicjutsus.util.itemreader;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import com.nisovin.magicjutsus.DebugHandler;

public class LeatherArmorHandler {

	public static ItemMeta process(ConfigurationSection config, ItemMeta meta) {
		if (!(meta instanceof LeatherArmorMeta)) return meta;
		
		LeatherArmorMeta lameta = (LeatherArmorMeta)meta;
		
		if (config.contains("color") && config.isString("color")) {
			try {
				int color = Integer.parseInt(config.getString("color").replace("#", ""), 16);
				lameta.setColor(Color.fromRGB(color));
			} catch (NumberFormatException e) {
				//TODO try processing by name if rgb fails
				DebugHandler.debugNumberFormat(e);
			}
		}
		return lameta;
	}
	
}
