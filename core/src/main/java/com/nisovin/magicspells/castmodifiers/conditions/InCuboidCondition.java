package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class InCuboidCondition extends Condition {

	private String worldName;
	private int minX;
	private int minY;
	private int minZ;
	private int maxX;
	private int maxY;
	private int maxZ;
	
	@Override
	public boolean initialize(String var) {
		try {
			String[] split = var.split(",");
			worldName = split[0];

			int x1 = Integer.parseInt(split[1]);
			int y1 = Integer.parseInt(split[2]);
			int z1 = Integer.parseInt(split[3]);

			int x2 = Integer.parseInt(split[4]);
			int y2 = Integer.parseInt(split[5]);
			int z2 = Integer.parseInt(split[6]);

			if (x1 < x2) {
				minX = x1;
				maxX = x2;
			} else {
				minX = x2;
				maxX = x1;
			}

			if (y1 < y2) {
				minY = y1;
				maxY = y2;
			} else {
				minY = y2;
				maxY = y1;
			}

			if (z1 < z2) {
				minZ = z1;
				maxZ = z2;
			} else {
				minZ = z2;
				maxZ = z1;
			}

			return true;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkCuboid(caster.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkCuboid(target.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return checkCuboid(location);
	}

	private boolean checkCuboid(Location location) {
		World world = Bukkit.getWorld(worldName);

		if (world == null) return false;
		if (!world.equals(location.getWorld())) return false;

		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();

		return
				minX <= x && x <= maxX &&
				minY <= y && y <= maxY &&
				minZ <= z && z <= maxZ;
	}

}
