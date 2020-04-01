package com.nisovin.magicjutsus.util;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtil {

    public static ItemStack addFakeEnchantment(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        item.addUnsafeEnchantment(Enchantment.FROST_WALKER, -1);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
}
