package com.nisovin.magicjutsus.mana;

import org.bukkit.ChatColor;

public class ManaRank {
	
	private String name;
	private String prefix;

	private char symbol;

	private int barSize;
	private int maxMana;
	private int startingMana;
	private int regenAmount;
	private int regenInterval;

	private ChatColor colorFull;
	private ChatColor colorEmpty;

	ManaRank() {

	}

	ManaRank(String name, String prefix, char symbol, int barSize, int maxMana, int startingMana, int regenAmount, int regenInterval, ChatColor colorFull, ChatColor colorEmpty) {
		this.name = name;
		this.prefix = prefix;
		this.symbol = symbol;
		this.barSize = barSize;
		this.maxMana = maxMana;
		this.startingMana = startingMana;
		this.regenAmount = regenAmount;
		this.regenInterval = regenInterval;
		this.colorFull = colorFull;
		this.colorEmpty = colorEmpty;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
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

	public ChatColor getColorFull() {
		return colorFull;
	}

	public void setColorFull(ChatColor colorFull) {
		this.colorFull = colorFull;
	}

	public ChatColor getColorEmpty() {
		return colorEmpty;
	}

	public void setColorEmpty(ChatColor colorEmpty) {
		this.colorEmpty = colorEmpty;
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
			+ ",colorFull=" + colorFull
			+ ",colorEmpty=" + colorEmpty
			+ ']';
	}
	
}
