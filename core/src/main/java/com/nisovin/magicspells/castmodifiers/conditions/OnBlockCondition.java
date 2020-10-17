package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.castmodifiers.Condition;

public class OnBlockCondition extends Condition {

	private Set<Material> materials;
	private Material material;

	@Override
	public boolean initialize(String var) {
		if (var.contains(",")) {
			materials = new HashSet<>();
			String[] split = var.split(",");
			for (String s : split) {
				Material mat = Util.getMaterial(s);
				if (mat == null) return false;
				if (!mat.isBlock()) return false;
				materials.add(mat);
			}
			return true;
		}

		material = Util.getMaterial(var);
		return material != null && material.isBlock();
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		Block block = target.getLocation().subtract(0, 1, 0).getBlock();
		if (material != null) return material.equals(block.getType());

		return materials.contains(block.getType());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
