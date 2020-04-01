package com.nisovin.magicjutsus.castmodifiers.conditions;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.castmodifiers.Condition;

public class InBlockCondition extends Condition {

	private Set<Material> materials;
	private Material material;

	@Override
	public boolean setVar(String var) {
		if (var.contains(",")) {
			materials = new HashSet<>();
			String[] split = var.split(",");
			for (String s : split) {
				Material mat = Material.getMaterial(s.toUpperCase());
				if (mat == null) return false;
				if (!mat.isBlock()) return false;
				materials.add(mat);
			}
			return true;
		}

		material = Material.getMaterial(var.toUpperCase());
		if (material == null) return false;
		if (!material.isBlock()) return false;
		return true;
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return inBlock(livingEntity.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return inBlock(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return inBlock(location);
	}

	private boolean inBlock(Location location) {
		Block block = location.getBlock();
		if (material != null) return material.equals(block.getType());
		return materials.contains(block.getType());
	}

}
