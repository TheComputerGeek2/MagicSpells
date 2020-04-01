package com.nisovin.magicjutsus.util.itemreader.alternative;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

public class AlternativeReaderManager {
	
	private static Map<String, ItemConfigTransformer> readers = new HashMap<>();
	private static final String KEY_PROCESSOR = "type";
	
	private static void register(ItemConfigTransformer transformer) {
		String key = transformer.getReaderKey();
		readers.put(key, transformer);
	}
	
	static {
		register(new SpigotReader());
	}
	
	public static ItemConfigTransformer getReader(String type) {
		if (type == null) return null;
		return readers.get(type.toLowerCase());
	}
	
	public static ItemStack deserialize(ConfigurationSection configurationSection) {
		if (configurationSection == null) return null;
		
		ItemConfigTransformer transformer = getReader(configurationSection.getString(KEY_PROCESSOR));
		if (transformer == null) return null;
		
		return transformer.deserialize(configurationSection);
	}
	
	// Converts ItemStack objects into ConfigurationSections based upon the specified processor
	// Outputs from this should either be null or reversible through deserialize
	public static ConfigurationSection serialize(String processor, ItemStack itemStack) {
		ItemConfigTransformer serializer = getReader(processor);
		if (serializer == null) return null;
		
		ConfigurationSection ret = serializer.serialize(itemStack);
		if (ret == null) return null;
		
		// Apply the processor key to the section here to reduce code in the processors
		ret.set(KEY_PROCESSOR, serializer.getReaderKey());
		return ret;
	}
	
}
