package com.nisovin.magicspells.util.itemreader;

import org.bukkit.Color;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.COLOR;

public class LeatherArmorHandler {

	private final static String CONFIG_NAME = COLOR.toString();

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof LeatherArmorMeta armorMeta)) return;
		if (!config.isString(CONFIG_NAME)) return;

		try {
			int color = Integer.parseInt(config.getString(CONFIG_NAME, "").replace("#", ""), 16);
			Color c = Color.fromRGB(color);

			armorMeta.setColor(c);
			if (data != null) data.setAttribute(COLOR, c);
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
		}
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof LeatherArmorMeta armorMeta)) return;
		if (!data.hasAttribute(COLOR)) return;
		armorMeta.setColor((Color) data.getAttribute(COLOR));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof LeatherArmorMeta)) return;

		Color color = ((LeatherArmorMeta) meta).getColor();

		String hex = Integer.toHexString(color.getRed()).toUpperCase() +
				Integer.toHexString(color.getGreen()).toUpperCase() +
				Integer.toHexString(color.getBlue()).toUpperCase();

		// default color is null
		if (!hex.equals("A06540")) data.setAttribute(COLOR, color);
	}

	public static Color getColor(ItemMeta meta) {
		if (!(meta instanceof LeatherArmorMeta)) return null;

		Color color = ((LeatherArmorMeta) meta).getColor();
		String hex = Integer.toHexString(color.getRed()).toUpperCase() +
				Integer.toHexString(color.getGreen()).toUpperCase() +
				Integer.toHexString(color.getBlue()).toUpperCase();

		if (hex.equals("A06540")) return null;
		return color;
	}
	
}
