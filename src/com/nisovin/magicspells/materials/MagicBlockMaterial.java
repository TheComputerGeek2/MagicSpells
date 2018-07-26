package com.nisovin.magicspells.materials;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class MagicBlockMaterial extends MagicMaterial {
	Material material;

	public MagicBlockMaterial(Material material) {
		this.material = material;
	}
	
	@Override
	public Material getMaterial() {
		return material;
	}

	@Override
	public void setBlock(Block block, boolean applyPhysics) {
		BlockState state = block.getState();
		state.setType(material);
		state.update(true, applyPhysics);
	}
	
	@Override
	public FallingBlock spawnFallingBlock(Location location) {
		return location.getWorld().spawnFallingBlock(location, new MaterialData(material));
	}

	@Override
	public ItemStack toItemStack(int quantity) {
		return new ItemStack(material, quantity);
	}
	
}
