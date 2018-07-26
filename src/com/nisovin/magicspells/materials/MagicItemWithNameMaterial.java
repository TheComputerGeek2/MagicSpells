package com.nisovin.magicspells.materials;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

public class MagicItemWithNameMaterial extends MagicMaterial {

	MagicMaterial material;
	String name;
	
	public MagicItemWithNameMaterial(MagicMaterial material, String name) {
		this.material = material;
		this.name = ChatColor.translateAlternateColorCodes('&', name);
	}
	
	@Override
	public Material getMaterial() {
		return this.material.getMaterial();
	}


	@Override
	public ItemStack toItemStack(int quantity) {
		ItemStack item = this.material.toItemStack(quantity);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(this.name);
		item.setItemMeta(meta);
		return item;
	}
	
	@Override
	public boolean equals(ItemStack item) {
		if (!this.material.equals(item)) return false;
		String iname = item.getItemMeta().getDisplayName();
		if (iname == null || iname.isEmpty()) return false;
		return iname.equals(this.name);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MagicItemWithNameMaterial) {
			MagicItemWithNameMaterial m = (MagicItemWithNameMaterial)o;
			return m.name.equals(this.name);
		}
		return false;
	}

}
