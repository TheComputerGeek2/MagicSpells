package com.nisovin.magicjutsus.materials;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.Objects;

public class MagicUnknownAnyDataMaterial extends MagicUnknownMaterial {

	public MagicUnknownAnyDataMaterial(Material type) {
		super(type, (short)0);
	}
	
	@Override
	public boolean equals(MaterialData matData) {
		return matData.getItemType() == this.material;
	}
	
	@Override
	public boolean equals(ItemStack itemStack) {
		return itemStack.getType() == this.material;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(
			this.material,
			":*"
		);
	}

}
