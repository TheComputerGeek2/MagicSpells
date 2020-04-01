package com.nisovin.magicjutsus.util.managers;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import com.nisovin.magicjutsus.MagicJutsus;

public class ExperienceBarManager {

	private Map<Player, Object> locks = new HashMap<>();
	
	public void update(Player player, int level, float percent) {
		update(player, level, percent, null);
	}
	
	public void update(Player player, int level, float percent, Object object) {
		Object lock = locks.get(player);
		if (lock == null || Objects.equals(object, lock)) {
			if (player.getOpenInventory().getType() != InventoryType.ENCHANTING) {
				MagicJutsus.getVolatileCodeHandler().setExperienceBar(player, level, percent);
			}
		}
	}
	
	public void lock(Player player, Object object) {
		Object lock = locks.get(player);
		if (lock == null || lock.equals(object)) {
			locks.put(player, object);
		}
	}
	
	public void unlock(Player player, Object object) {
		Object lock = locks.get(player);
		if (lock == null) return;
		if (lock.equals(object)) locks.remove(player);
	}
	
}
