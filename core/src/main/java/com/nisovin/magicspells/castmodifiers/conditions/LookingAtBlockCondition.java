package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.castmodifiers.Condition;

public class LookingAtBlockCondition extends Condition {

	private Material blockType;
	private int dist = 4;
	
	@Override
	public boolean initialize(String var) {
		try {
			String[] split = var.split(",");
			blockType = Util.getMaterial(split[0]);

			if (blockType == null || !blockType.isBlock()) return false;
			if (split.length > 1) dist = Integer.parseInt(split[1]);

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
