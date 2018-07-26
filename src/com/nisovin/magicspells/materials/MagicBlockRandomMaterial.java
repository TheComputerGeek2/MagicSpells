package com.nisovin.magicspells.materials;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

import java.util.Arrays;

public class MagicBlockRandomMaterial extends MagicBlockMaterial {
	
	MagicMaterial[] materials;
	
	public MagicBlockRandomMaterial(MagicMaterial[] materials) {
		super(null);
		this.materials = materials;
	}
	
	@Override
	public Material getMaterial() {
		return this.materials[ItemNameResolver.rand.nextInt(this.materials.length)].getMaterial();
	}
	
	@Override
	public void setBlock(Block block, boolean applyPhysics) {
		MagicMaterial material = this.materials[ItemNameResolver.rand.nextInt(this.materials.length)];
		BlockState state = block.getState();
		state.setType(block.getType());
		state.update(true, applyPhysics);
	}

	public boolean equals(MaterialData data) {
		return Arrays.stream(this.materials).anyMatch(m -> m.equals(data));
	}
	
}
