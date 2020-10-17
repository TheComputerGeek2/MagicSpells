package com.nisovin.magicspells.variables;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.configuration.ConfigurationSection;

public abstract class Variable {

	protected double defaultValue = 0;
	protected String defaultStringValue = 0D + "";

	protected double minValue = 0;
	protected double maxValue = Double.MAX_VALUE;

	protected boolean expBar;
	protected boolean permanent;

	protected Objective objective;

	protected String bossBarTitle;
	protected BarStyle bossBarStyle;
	protected BarColor bossBarColor;
	protected String bossBarNamespacedKey;

	public Variable() {
		// No op
	}

	public final void init(double defaultValue, double minValue, double maxValue, boolean permanent, Objective objective, boolean expBar, String bossBarTitle, BarStyle bossBarStyle, BarColor bossBarColor, String bossBarNamespacedKey) {
		this.defaultValue = defaultValue;
		this.defaultStringValue = defaultValue + "";
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.permanent = permanent;
		this.objective = objective;
		this.expBar = expBar;
		this.bossBarTitle = bossBarTitle;
		this.bossBarStyle = bossBarStyle;
		this.bossBarColor = bossBarColor;
		this.bossBarNamespacedKey = bossBarNamespacedKey;
		init();
	}

	protected void init() {
		// No op
	}

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

	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

	public double getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(double defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDefaultStringValue() {
		return defaultStringValue;
	}

	public void setDefaultStringValue(String defaultStringValue) {
		this.defaultStringValue = defaultStringValue;
	}

	public boolean isPermanent() {
		return permanent;
	}

	public void setPermanent(boolean permanent) {
		this.permanent = permanent;
	}

	public Objective getObjective() {
		return objective;
	}

	public void setObjective(Objective objective) {
		this.objective = objective;
	}

	public boolean isDisplayedOnExpBar() {
		return expBar;
	}

	public void displayOnExpBar(boolean expBar) {
		this.expBar = expBar;
	}

	public String getBossBarTitle() {
		return bossBarTitle;
	}

	public void setBossBarTitle(String bossBarTitle) {
		this.bossBarTitle = bossBarTitle;
	}

	public BarStyle getBossBarStyle() {
		return bossBarStyle;
	}

	public void setBossBarStyle(BarStyle bossBarStyle) {
		this.bossBarStyle = bossBarStyle;
	}

	public BarColor getBossBarColor() {
		return bossBarColor;
	}

	public void setBossBarColor(BarColor bossBarColor) {
		this.bossBarColor = bossBarColor;
	}

	public String getBossBarNamespacedKey() {
		return bossBarNamespacedKey;
	}

	public void setBossBarNamespacedKey(String bossBarNamespacedKey) {
		this.bossBarNamespacedKey = bossBarNamespacedKey;
	}

}
