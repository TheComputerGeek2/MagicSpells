package com.nisovin.magicjutsus.materials;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class MagicItemMaterial extends MagicMaterial {
	
	Material type;
	MaterialData matData;
	short duraData;

	public MagicItemMaterial(Material type, short data) {
		this.type = type;
		this.duraData = data;
	}
	
	public MagicItemMaterial(MaterialData data) {
		this.type = data.getItemType();
		this.matData = data;
	}
	
	public short getDurability() {
		return this.duraData;
	}
	
	@Override
	public Material getMaterial() {
		return this.type;
	}
	
	@Override
	public MaterialData getMaterialData() {
		if (this.matData != null) return this.matData;
		return new MaterialData(this.type);
	}

	@Override
	public ItemStack toItemStack(int quantity) {
		MaterialData matData = getMaterialData();
		if (matData != null) return matData.toItemStack(quantity);
		return new ItemStack(getMaterial(), quantity, getDurability());
	}
	
	@Override
	public boolean equals(ItemStack item) {
		if (this.matData != null) {
			ItemStack i = this.matData.toItemStack();
			return i.getType() == item.getType() && i.getDurability() == item.getDurability();
		}
		return this.type == item.getType() && this.duraData == item.getDurability();
	}
	
}
