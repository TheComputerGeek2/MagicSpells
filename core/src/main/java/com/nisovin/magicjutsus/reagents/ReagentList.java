package com.nisovin.magicjutsus.reagents;

import java.util.Map;

import org.bukkit.entity.Player;

public class ReagentList {

	Map<Reagent, Integer> reagents;
	
	public boolean has(Player player) {
		for (Map.Entry<Reagent, Integer> entry : this.reagents.entrySet()) {
			if (!entry.getKey().has(player, entry.getValue())) {
				return false;
			}
		}
		return true;
	}
	
	public void remove(Player player) {
		for (Map.Entry<Reagent, Integer> entry : this.reagents.entrySet()) {
			entry.getKey().remove(player, entry.getValue());
		}
	}
	
	public void multiply(float num) {
		for (Reagent reagent : this.reagents.keySet()) {
			int amount = this.reagents.get(reagent);
			this.reagents.put(reagent, Math.round(amount * num));
		}
	}
	
}
