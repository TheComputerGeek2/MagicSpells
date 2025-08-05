package com.nisovin.magicspells.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;

public class ItemUtil {

	public static int getDurability(ItemStack item) {
		return item.getItemMeta() instanceof Damageable damageable ? damageable.getDamage() : 0;
	}

	public static int getCustomModelData(ItemMeta meta) {
		if (meta == null) return 0;
		if (meta.hasCustomModelData()) return meta.getCustomModelData();
		return 0;
	}

}
