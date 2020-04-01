package com.nisovin.magicjutsus.mana;

import org.bukkit.entity.Player;

import com.nisovin.magicjutsus.castmodifiers.ModifierSet;

public abstract class ManaHandler {

	public abstract void initialize();
	
	public abstract void createManaBar(Player player);
	
	public abstract boolean updateManaRankIfNecessary(Player player);
	
	public abstract int getMaxMana(Player player);
	
	public abstract void setMaxMana(Player player, int amount);
	
	public abstract int getRegenAmount(Player player);
	
	public abstract void setRegenAmount(Player player, int amount);
	
	public abstract int getMana(Player player);
	
	public abstract boolean hasMana(Player player, int amount);
	
	public abstract boolean removeMana(Player player, int amount, ManaChangeReason reason);
	
	public abstract boolean addMana(Player player, int amount, ManaChangeReason reason);
	
	public abstract boolean setMana(Player player, int amount, ManaChangeReason reason);
	
	public void showMana(Player player) {
		showMana(player, false);
	}
	
	public abstract void showMana(Player player, boolean forceShowInChat);
	
	public ModifierSet getModifiers() {
		return null;
	}
	
	public abstract void turnOff();
	
}
