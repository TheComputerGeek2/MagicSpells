package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class OverGroundCondition extends Condition {

	private int depth;

	@Override
	public boolean initialize(String var) {
		try {
			depth = Integer.parseInt(var);
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}

		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return overGround(caster.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return overGround(target.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return overGround(location);
	}

	private boolean overGround(Location location) {
		Block block = location.clone().subtract(0, 1, 0).getBlock();

		Material material;
		for (int i = 0; i < depth; i++) {

			material = block.getType();
			if (material.isBlock() && material.isSolid() && material.isCollidable()) {
				return true;
			}

			block = block.getRelative(BlockFace.DOWN);
		}

		return false;
	}

}
