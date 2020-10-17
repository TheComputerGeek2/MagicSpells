package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.castmodifiers.Condition;

public class RoofCondition extends Condition {

	private int height = 10;
	
	@Override
	public boolean initialize(String var) {
		if (var != null && RegexUtil.matches(RegexUtil.SIMPLE_INT_PATTERN, var)) {
			height = Integer.parseInt(var);
		}
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(target, target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		Block b = location.clone().add(0, 2, 0).getBlock();
		for (int i = 0; i < height; i++) {
			if (!BlockUtils.isAir(b.getType())) return true;
			b = b.getRelative(BlockFace.UP);
		}
		return false;
	}

}
