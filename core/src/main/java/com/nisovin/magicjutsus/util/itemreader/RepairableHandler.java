package com.nisovin.magicjutsus.util.itemreader;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

public class RepairableHandler {

	public static ItemMeta process(ConfigurationSection config, ItemMeta meta) {
		if (!(meta instanceof Repairable)) return meta;
		
		if (config.contains("repaircost") && config.isInt("repaircost")) {
			((Repairable)meta).setRepairCost(config.getInt("repaircost"));
		}
		
		return meta;
	}
	
}
