package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OnFireCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return livingEntity.getFireTicks() > 0;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return target.getFireTicks() > 0;
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return location.getBlock().getType() == Material.FIRE || location.getBlock().getRelative(BlockFace.UP).getType() == Material.FIRE;
	}

}
