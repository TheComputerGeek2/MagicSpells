package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OnBlockCondition extends Condition {

	private Set<BlockData> blockDataSet;
	private BlockData blockData;

	@Override
	public boolean initialize(String var) {
		String[] split = var.split(",(?![^\\[]*])");

		if (split.length > 1) {
			blockDataSet = new HashSet<>();

			BlockData data;
			for (String s : split) {
				try {
					data = Bukkit.createBlockData(s.trim().toLowerCase());
				} catch (IllegalArgumentException e) {
					return false;
				}

				if (!data.getMaterial().isBlock()) return false;

				blockDataSet.add(data);
			}

			return true;
		}

		try {
			blockData = Bukkit.createBlockData(var.trim().toLowerCase());
		} catch (IllegalArgumentException e) {
			return false;
		}

		return blockData.getMaterial().isBlock();
	}

	@Override
	public boolean check(LivingEntity caster) {
		return onBlock(caster.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return onBlock(target.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return onBlock(location.clone());
	}

	private boolean onBlock(Location location) {
		BlockData bd = location.subtract(0, 1, 0).getBlock().getBlockData();
		if (blockData != null) return bd.matches(blockData);

		for (BlockData data : blockDataSet) {
			if (bd.matches(data)) return true;
		}

		return false;
	}

}
