package com.nisovin.magicjutsus.util.itemreader;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.configuration.ConfigurationSection;

public class BannerHandler {

	public static ItemMeta process(ConfigurationSection config, ItemMeta meta) {
		if (!(meta instanceof BannerMeta)) return meta;
		
		BannerMeta bmeta = (BannerMeta)meta;
		
		if (config.contains("color") && config.isString("color")) {
			String s = config.getString("color").toLowerCase();
			for (DyeColor c : DyeColor.values()) {
				// TODO use objects.equals
				if (c != null && c.name().replace("_", "").toLowerCase().equals(s)) {
					bmeta.setBaseColor(c);
					//Pattern pattern = new Pattern(c, PatternType.BASE);
					//bmeta.setPattern(0, pattern);
					break;
				}
			}
		}
		if (config.contains("patterns") && config.isList("patterns")) {
			List<String> patterns = config.getStringList("patterns");
			for (String patternData : patterns) {
				if (patternData.contains(" ")) {
					String[] split = patternData.split(" ");
					DyeColor color = null;
					String splitZeroLowercase = split[0].toLowerCase();
					for (DyeColor c : DyeColor.values()) {
						// TODO use objects.equals
						if (c != null && c.name().replace("_", "").toLowerCase().equals(splitZeroLowercase)) {
							color = c;
							break;
						}
					}
					PatternType pattern = PatternType.getByIdentifier(split[1]);
					if (pattern == null) {
						for (PatternType p : PatternType.values()) {
							if (p != null && p.name().equalsIgnoreCase(split[1])) {
								pattern = p;
								break;
							}
						}
					}
					if (color != null && pattern != null) bmeta.addPattern(new Pattern(color, pattern));
				}
			}
		}
		return bmeta;
	}
	
}
