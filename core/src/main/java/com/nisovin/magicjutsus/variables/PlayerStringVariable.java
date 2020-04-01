package com.nisovin.magicjutsus.variables;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;

public class PlayerStringVariable extends PlayerVariable {

	private Map<String, String> data;
	
	public PlayerStringVariable() {
		data = new HashMap<>();
	}
	
	@Override
	public void loadExtraData(ConfigurationSection section) {
		super.loadExtraData(section);
		defaultStringValue = section.getString("default-value", "");
	}
	
	@Override
	public String getStringValue(String player) {
		String ret = data.get(player);
		if (ret == null) ret = defaultStringValue;
		return ret;
	}
	
	@Override
	public void parseAndSet(String player, String textValue) {
		data.put(player, textValue);
	}
	
	@Override
	public void reset(String player) {
		data.remove(player);
	}
	
}
