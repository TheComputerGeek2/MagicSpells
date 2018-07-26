package com.nisovin.magicspells.materials;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class MagicItemMaterial extends MagicMaterial {
	
	Material type;
	short duraData;

	public MagicItemMaterial(Material type, short data) {
		this.type = type;
		this.duraData = data;
	}

	
	public short getDurability() {
		return this.duraData;
	}
	
	@Override
	public Material getMaterial() {
		return this.type;
	}

	@Override
	public ItemStack toItemStack(int quantity) {
		return new ItemStack(getMaterial(), quantity, getDurability());
	}
	
	@Override
	public boolean equals(ItemStack item) {
		return this.type == item.getType() && this.duraData == item.getDurability();
	}
	
}
