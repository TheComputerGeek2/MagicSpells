package com.nisovin.magicspells.util.managers;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import com.nisovin.magicspells.MagicSpells;

public class ExperienceBarManager {

	private final Map<Player, Object> locks = new HashMap<>();
	
	public void update(Player player, int level, float percent) {
		update(player, level, percent, null);
	}
	
	public void update(Player player, int level, float percent, Object object) {
		Object lock = locks.get(player);
		if (lock == null || Objects.equals(object, lock)) {
			if (player.getOpenInventory().getType() == InventoryType.ENCHANTING) return;
			if (percent < 0F) percent = 0F;
			if (percent > 1F) percent = 1F;
			MagicSpells.getVolatileCodeHandler().setExperienceBar(player, level, percent);
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
