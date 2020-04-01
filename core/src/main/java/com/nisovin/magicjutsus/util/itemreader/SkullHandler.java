package com.nisovin.magicjutsus.util.itemreader;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.nisovin.magicjutsus.MagicJutsus;

public class SkullHandler {

	public static ItemMeta process(ConfigurationSection config, ItemMeta meta) {
		if (!(meta instanceof SkullMeta)) return meta;
		
		SkullMeta smeta = (SkullMeta)meta;
		
		if (config.contains("skullowner") && config.isString("skullowner")) smeta.setOwner(config.getString("skullowner"));
		
		String uuid = null;
		if (config.contains("uuid") && config.isString("uuid")) uuid = config.getString("uuid");
		
		String texture = null;
		if (config.contains("texture") && config.isString("texture")) texture = config.getString("texture");
		
		String signature = null;
		if (config.contains("signature") && config.isString("signature")) signature = config.getString("signature");
		if (texture != null) MagicJutsus.getVolatileCodeHandler().setTexture(smeta, texture, signature, uuid, smeta.getOwner());
		return smeta;
	}
	
}
