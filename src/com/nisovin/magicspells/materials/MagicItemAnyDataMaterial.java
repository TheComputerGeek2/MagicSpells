package com.nisovin.magicspells.materials;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class MagicItemAnyDataMaterial extends MagicItemMaterial {
	
	public MagicItemAnyDataMaterial(Material type) {
		super(type, (short) 0);
	}
	
	@Override
	public boolean equals(ItemStack item) {
		if (this.type != null) return this.type == item.getType();
		return this.type == item.getType();
	}

}
