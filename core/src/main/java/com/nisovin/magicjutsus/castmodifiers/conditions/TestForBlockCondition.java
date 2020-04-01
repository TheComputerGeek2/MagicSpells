package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.MagicLocation;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class TestForBlockCondition extends Condition {

	private MagicLocation location;
	private Material blockType;
	
	@Override
	public boolean setVar(String var) {
		try {
			String[] varsplit = var.split("=");
			String[] locsplit = varsplit[0].split(",");
			location = new MagicLocation(locsplit[0], Integer.parseInt(locsplit[1]), Integer.parseInt(locsplit[2]), Integer.parseInt(locsplit[3]));
			blockType = Material.getMaterial(varsplit[1].toUpperCase());
			if (blockType == null || !blockType.isBlock()) return false;
			return true;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		Location loc = location.getLocation();
		if (loc == null) return false;
		if (blockType.equals(loc.getBlock().getType())) return true;
		return false;
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
