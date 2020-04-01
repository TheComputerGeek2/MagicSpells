package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class LookingAtBlockCondition extends Condition {

	private Material blockType;
	private int dist = 4;
	
	@Override
	public boolean setVar(String var) {
		try {
			String[] varsplit = var.split(",");
			blockType = Material.getMaterial(varsplit[0].toUpperCase());
			if (blockType == null || !blockType.isBlock()) return false;
			if (varsplit.length > 1) {
				dist = Integer.parseInt(varsplit[1]);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		Block block = BlockUtils.getTargetBlock(null, target, dist);
		return blockType.equals(block.getType());
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}
	
}
