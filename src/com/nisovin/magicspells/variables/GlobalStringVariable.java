package com.nisovin.magicspells.variables;

import org.bukkit.configuration.ConfigurationSection;

public class GlobalStringVariable extends Variable {
	
	String data = "";
	
	@Override
	public void loadExtraData(ConfigurationSection section) {
		super.loadExtraData(section);
		this.defaultStringValue = section.getString("default-value", "");
	}
	
	@Override
	public String getStringValue(String player) {
		String ret = this.data;
		if (ret == null) ret = this.defaultStringValue;
		return ret;
	}
	
	@Override
	public void parseAndSet(String player, String textValue) {
		this.data = textValue;
	}
	
	@Override
	public void reset(String player) {
		this.data = null;
	}
	
	@Override
	protected void init() {
		
	}
	
	@Override
	public boolean modify(String player, double amount) {
		return false;
	}

	@Override
	public void set(String player, double amount) {
		
	}

	@Override
	public double getValue(String player) {
		return 0;
	}
	
}
