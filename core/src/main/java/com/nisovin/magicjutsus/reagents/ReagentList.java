package com.nisovin.magicjutsus.reagents;

import java.util.Map;

import org.bukkit.entity.Player;

public class ReagentList {

	private Map<Reagent, Integer> reagents;
	
	public boolean has(Player player) {
		for (Map.Entry<Reagent, Integer> entry : reagents.entrySet()) {
			if (!entry.getKey().has(player, entry.getValue())) {
				return false;
			}
		}
		return true;
	}
	
	public void remove(Player player) {
		for (Map.Entry<Reagent, Integer> entry : reagents.entrySet()) {
			entry.getKey().remove(player, entry.getValue());
		}
	}
	
	public void multiply(float num) {
		for (Reagent reagent : reagents.keySet()) {
			int amount = reagents.get(reagent);
			reagents.put(reagent, Math.round(amount * num));
		}
	}
	
}
