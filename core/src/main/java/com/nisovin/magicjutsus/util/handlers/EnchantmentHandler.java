package com.nisovin.magicjutsus.util.handlers;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

public class EnchantmentHandler {

	public static Enchantment getEnchantment(String name) {
		return Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
	}

}
