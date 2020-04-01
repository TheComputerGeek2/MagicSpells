package com.nisovin.magicjutsus.util.itemreader;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.BooleanUtils;
import com.nisovin.magicjutsus.util.handlers.PotionEffectHandler;

public class PotionHandler {

	public static final String POTION_EFFECT_CONFIG_NAME = "potioneffects";
	
	public static final String POTION_ITEM_COLOR_CONFIG_FIELD = "potioncolor";
	
	public static ItemMeta process(ConfigurationSection config, ItemMeta meta) {
		if (!(meta instanceof PotionMeta)) return meta;
		
		PotionMeta pmeta = (PotionMeta) meta;
		
		if (config.contains(POTION_EFFECT_CONFIG_NAME) && config.isList(POTION_EFFECT_CONFIG_NAME)) {
			pmeta.clearCustomEffects();
			List<String> potionEffects = config.getStringList(POTION_EFFECT_CONFIG_NAME);
			for (String potionEffect : potionEffects) {
				PotionEffect eff = buildPotionEffect(potionEffect);
				if (eff == null) continue;
				pmeta.addCustomEffect(eff, true);
			}
		}
		
		if (config.contains(POTION_ITEM_COLOR_CONFIG_FIELD) && config.isString(POTION_ITEM_COLOR_CONFIG_FIELD)) {
			int color = Integer.parseInt(config.getString(POTION_ITEM_COLOR_CONFIG_FIELD).replace("#", ""), 16);
			Color c = Color.fromRGB(color);
			pmeta.setColor(c);
		}
		
		return pmeta;
	}
	
	private static PotionEffect buildPotionEffect(String effectString) {
		String[] data = effectString.split(" ");
		PotionEffectType t = PotionEffectHandler.getPotionEffectType(data[0]);
		
		if (t == null) MagicJutsus.error('\'' + data[0] + "' could not be connected to a potion effect type");
		if (t != null) {
			int level = 0;
			if (data.length > 1) {
				try {
					level = Integer.parseInt(data[1]);
				} catch (NumberFormatException ex) {
					DebugHandler.debugNumberFormat(ex);
				}
			}
			int duration = 600;
			if (data.length > 2) {
				try {
					duration = Integer.parseInt(data[2]);
				} catch (NumberFormatException ex) {
					DebugHandler.debugNumberFormat(ex);
				}
			}
			boolean ambient = false;
			if (data.length > 3 && (BooleanUtils.isYes(data[3]) || data[3].equalsIgnoreCase("ambient"))) ambient = true;
			return new PotionEffect(t, duration, level, ambient);
		}
		return null;
	}
	
}
