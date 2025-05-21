package com.nisovin.magicspells.mana;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;

public class ManaRank {
	
	private String name;
	private Component prefix;

	private char symbol;

	private int barSize;
	private int maxMana;
	private int startingMana;
	private int regenAmount;
	private int regenInterval;

	private Style styleFull;
	private Style styleEmpty;

	ManaRank() {

	}

	ManaRank(String name, Component prefix, char symbol, int barSize, int maxMana, int startingMana, int regenAmount, int regenInterval, Style styleFull, Style styleEmpty) {
		this.name = name;
		this.prefix = prefix;
		this.symbol = symbol;
		this.barSize = barSize;
		this.maxMana = maxMana;
		this.startingMana = startingMana;
		this.regenAmount = regenAmount;
		this.regenInterval = regenInterval;
		this.styleFull = styleFull;
		this.styleEmpty = styleEmpty;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Component getPrefix() {
		return prefix;
	}

	public void setPrefix(Component prefix) {
		this.prefix = prefix;
	}

	public char getSymbol() {
		return symbol;
	}

	public void setSymbol(char symbol) {
		this.symbol = symbol;
	}

	public int getBarSize() {
		return barSize;
	}

	public void setBarSize(int barSize) {
		this.barSize = barSize;
	}

	public int getMaxMana() {
		return maxMana;
	}

	public void setMaxMana(int maxMana) {
		this.maxMana = maxMana;
	}

	public int getStartingMana() {
		return startingMana;
	}

	public void setStartingMana(int startingMana) {
		this.startingMana = startingMana;
	}

	public int getRegenAmount() {
		return regenAmount;
	}

	public void setRegenAmount(int regenAmount) {
		this.regenAmount = regenAmount;
	}

	public int getRegenInterval() {
		return regenInterval;
	}

	public void setRegenInterval(int regenInterval) {
		this.regenInterval = regenInterval;
	}

	public Style getStyleFull() {
		return styleFull;
	}

	public void setStyleFull(Style styleFull) {
		this.styleFull = styleFull;
	}

	public Style getStyleEmpty() {
		return styleEmpty;
	}

	public void setStyleEmpty(Style styleEmpty) {
		this.styleEmpty = styleEmpty;
	}
	
	@Override
	public String toString() {
		return "ManaRank:["
			+ "name=" + name
			+ ",prefix=" + prefix
			+ ",symbol=" + symbol
			+ ",barSize" + barSize
			+ ",maxMana=" + maxMana
			+ ",startingMana=" + startingMana
			+ ",regenAmount=" + regenAmount
			+ ",regenInterval=" + regenInterval
			+ ",colorFull=" + styleFull
			+ ",colorEmpty=" + styleEmpty
			+ ']';
	}
	
}
