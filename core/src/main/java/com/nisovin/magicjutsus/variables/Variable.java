package com.nisovin.magicjutsus.variables;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.configuration.ConfigurationSection;

public abstract class Variable {

	protected double defaultValue = 0;
	protected String defaultStringValue = 0D + "";
	protected double maxValue = Double.MAX_VALUE;
	protected double minValue = 0;
	protected boolean permanent;
	protected Objective objective;
	protected String bossBar;
	protected boolean expBar;
	
	public Variable() {
		// No op
	}
	
	public final void init(double defaultValue, double minValue, double maxValue, boolean permanent, Objective objective, String bossBar, boolean expBar) {
		this.defaultValue = defaultValue;
		this.defaultStringValue = defaultValue + "";
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.permanent = permanent;
		this.objective = objective;
		this.bossBar = bossBar;
		this.expBar = expBar;
		init();
	}
	
	protected void init() {
		// No op
	}
	
	public final boolean modify(Player player, double amount) {
		return modify(player.getName(), amount);
	}
	
	public abstract boolean modify(String player, double amount);
	
	public final void set(Player player, double amount) {
		set(player.getName(), amount);
	}
	
	public void parseAndSet(Player player, String textValue) {
		parseAndSet(player.getName(), textValue);
	}
	
	public abstract void set(String player, double amount);
	
	public void parseAndSet(String player, String textValue) {
		set(player, Double.parseDouble(textValue));
	}
	
	public double getValue(Player player) {
		if (player == null) return getValue("");
		return getValue(player.getName());
	}
	
	public abstract double getValue(String player);
	
	public final void reset(Player player) {
		reset(player.getName());
	}
	
	public abstract void reset(String player);
	
	public void loadExtraData(ConfigurationSection section) {
		// No op from here, but subclasses may add behavior
	}
	
	public String getStringValue(Player player) {
		return getStringValue(player.getName());
	}
	
	public String getStringValue(String player) {
		return getValue(player) + "";
	}

	public double getMaxValue() {
		return maxValue;
	}

	public double getMinValue() {
		return minValue;
	}
}
