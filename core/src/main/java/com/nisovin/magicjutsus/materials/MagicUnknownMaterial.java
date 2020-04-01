package com.nisovin.magicjutsus.materials;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.Objects;

public class MagicUnknownMaterial extends MagicMaterial {
	
	Material material;
	short data;
	
	public MagicUnknownMaterial(Material type, short data) {
		this.material = type;
		this.data = data;
	}
	
	@Override
	public Material getMaterial() {
		return this.material;
	}
	
	@Override
	public MaterialData getMaterialData() {
		return this.material.getNewData((byte) this.data);
	}
	
	@Override
	public void setBlock(Block block, boolean applyPhysics) {
		//if (this.data < 16) block.setTypeIdAndData(this.type, (byte)this.data, applyPhysics);
		if (this.data < 16) {
			block.setType(material);
			block.setBlockData((BlockData) material.getNewData((byte) this.data));
		}
	}
	
	@Override
	public FallingBlock spawnFallingBlock(Location location) {
		return location.getWorld().spawnFallingBlock(location, getMaterialData());
	}
	
	@Override
	public ItemStack toItemStack(int quantity) {
		return new ItemStack(this.material, quantity, this.data);
	}
	
	@Override
	public boolean equals(MaterialData matData) {
		return matData.getItemType() == this.material && matData.getData() == this.data;
	}
	
	@Override
	public boolean equals(ItemStack itemStack) {
		return itemStack.getType() == this.material && itemStack.getDurability() == this.data;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(
			this.material,
			":",
			this.data
		);
	}
	
}
