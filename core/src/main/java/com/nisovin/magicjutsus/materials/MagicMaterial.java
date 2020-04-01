package com.nisovin.magicjutsus.materials;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.Objects;

public abstract class MagicMaterial {
	
	public abstract Material getMaterial();
	
	public abstract MaterialData getMaterialData();
	
	public final void setBlock(Block block) {
		setBlock(block, true);
	}
	
	public void setBlock(Block block, boolean applyPhysics) { /* NO OP */}
	
	public FallingBlock spawnFallingBlock(Location location) { return null; }
	
	public final ItemStack toItemStack() {
		return toItemStack(1);
	}
	
	public abstract ItemStack toItemStack(int quantity);
	
	public final boolean equals(Block block) {
		return equals(block.getState().getData());
	}
	
	public boolean equals(MaterialData matData) {
		MaterialData d = getMaterialData();
		return Objects.equals(d, matData);
	}
	
	public boolean equals(ItemStack itemStack) {
		MaterialData d = getMaterialData();
		if (d != null) {
			ItemStack i = d.toItemStack();
			return i.getType() == itemStack.getType() && i.getDurability() == itemStack.getDurability();
		} else {
			return false;
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MagicMaterial) {
			MagicMaterial m = (MagicMaterial)o;
			return m.getMaterial() == getMaterial() && m.getMaterialData().equals(getMaterialData());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getMaterial().hashCode();
	}
	
	public static MagicMaterial fromItemStack(ItemStack item) {
		if (item.getType().isBlock()) {
			return new MagicBlockMaterial(item.getData());
		}
		return new MagicItemMaterial(item.getData());
	}
	
	public static MagicMaterial fromBlock(Block block) {
		return new MagicBlockMaterial(block.getState().getData());
	}
	
}
