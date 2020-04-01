package com.nisovin.magicjutsus.reagents;

import org.bukkit.entity.Player;

public abstract class Reagent {
	
	public abstract boolean has(Player player, int amount);
	
	public abstract boolean remove(Player player, int amount);
	
}
