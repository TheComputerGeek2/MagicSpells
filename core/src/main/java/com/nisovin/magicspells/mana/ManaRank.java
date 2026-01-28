package com.nisovin.magicspells.mana;

import net.kyori.adventure.text.format.Style;

public class ManaRank {
	
	private String name;

	private String barFormat;
	private char symbol;
	private int barSize;

	private int maxMana;
	private int startingMana;
	private int regenAmount;
	private int regenInterval;

	ManaRank() {

	}

	ManaRank(String name, char symbol, int barSize, String manaBarFormat, int maxMana, int startingMana, int regenAmount, int regenInterval) {
		this.name = name;

		this.symbol = symbol;
		this.barSize = barSize;
		this.barFormat = manaBarFormat;

		this.maxMana = maxMana;
		this.regenAmount = regenAmount;
		this.startingMana = startingMana;
		this.regenInterval = regenInterval;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBarFormat() {
		return barFormat;
	}

	public void setBarFormat(String barFormat) {
		this.barFormat = barFormat;
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
	
	@Override
	public String toString() {
		return "ManaRank:["
			+ "name=" + name
			+ ",symbol=" + symbol
			+ ",barSize=" + barSize
			+ ",barFormat=" + barFormat
			+ ",maxMana=" + maxMana
			+ ",startingMana=" + startingMana
			+ ",regenAmount=" + regenAmount
			+ ",regenInterval=" + regenInterval
			+ ']';
	}
	
}
