package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class OverBlockCondition extends Condition {

	private Set<BlockData> blockDataSet;
	private int depth;

	@Override
	public boolean initialize(String var) {
		String[] args = var.split(";", 2);
		if (args.length != 2) return false;

		try {
			depth = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}

		blockDataSet = new HashSet<>();
		for (String split : args[0].split(",(?![^\\[]*])")) {
			BlockData data;
			try {
				data = Bukkit.createBlockData(split.trim().toLowerCase());
			} catch (IllegalArgumentException e) {
				return false;
			}

			if (!data.getMaterial().isBlock()) return false;

			blockDataSet.add(data);
		}

		return !blockDataSet.isEmpty();
	}

	@Override
	public boolean check(LivingEntity caster) {
		return overBlock(caster.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return overBlock(target.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return overBlock(location);
	}

	private boolean overBlock(Location location) {
		Block block = location.clone().subtract(0, 1, 0).getBlock();

		for (int i = 0; i < depth; i++) {
			BlockData blockData = block.getBlockData();

			for (BlockData data : blockDataSet) {
				if (blockData.matches(data)) return true;
			}

			block = block.getRelative(BlockFace.DOWN);
		}

		return false;
	}

}
