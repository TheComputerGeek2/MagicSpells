package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.castmodifiers.Condition;

public class OnFireCondition extends Condition {

	@Override
	public boolean setVar(String var) {
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
