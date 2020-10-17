package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class TestForBlockCondition extends Condition {

	private MagicLocation location;
	private Material blockType;
	
	@Override
	public boolean initialize(String var) {
		try {
			String[] vars = var.split("=");
			String[] locs = vars[0].split(",");
			location = new MagicLocation(locs[0], Integer.parseInt(locs[1]), Integer.parseInt(locs[2]), Integer.parseInt(locs[3]));
			blockType = Util.getMaterial(vars[1]);
			return blockType != null && blockType.isBlock();
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		Location loc = location.getLocation();
		return loc != null && blockType.equals(loc.getBlock().getType());
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(null);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return check(null);
	}

}
