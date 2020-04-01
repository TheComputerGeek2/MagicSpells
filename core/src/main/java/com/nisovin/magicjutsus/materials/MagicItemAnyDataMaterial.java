package com.nisovin.magicjutsus.materials;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class MagicItemAnyDataMaterial extends MagicItemMaterial {

	public MagicItemAnyDataMaterial(MaterialData data) {
		super(data);
	}
	
	public MagicItemAnyDataMaterial(Material type) {
		super(type, (short)0);
	}
	
	@Override
	public boolean equals(ItemStack item) {
		if (this.matData != null) return this.matData.getItemType() == item.getType();
		return this.type == item.getType();
	}

}
