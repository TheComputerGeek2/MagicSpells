package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;

import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class TestForBlockCondition extends Condition {

	private MagicLocation location;
	private BlockData blockData;

	@Override
	public boolean initialize(String var) {
		try {
			String[] vars = var.split("=");
			String[] locs = vars[0].split(",");

			location = new MagicLocation(locs[0], Integer.parseInt(locs[1]), Integer.parseInt(locs[2]), Integer.parseInt(locs[3]));
			blockData = Bukkit.createBlockData(vars[1].toLowerCase());

			return blockData.getMaterial().isBlock();
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return testForBlock();
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return testForBlock();
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return testForBlock();
	}

	public boolean testForBlock() {
		return location != null && location.getLocation().getBlock().getBlockData().matches(blockData);
	}

}
