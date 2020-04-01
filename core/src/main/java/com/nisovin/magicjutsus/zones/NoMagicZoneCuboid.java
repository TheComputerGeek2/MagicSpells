package com.nisovin.magicjutsus.zones;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class NoMagicZoneCuboid extends NoMagicZone {
	
	private String worldName;
	private int minX;
	private int minY;
	private int minZ;
	private int maxX;
	private int maxY;
	private int maxZ;
	
	@Override
	public void initialize(ConfigurationSection config) {
		worldName = config.getString("world", "");
		
		String[] p1 = config.getString("point1", "0,0,0").replace(" ", "").split(",");
		String[] p2 = config.getString("point2", "0,0,0").replace(" ", "").split(",");
		int x1 = Integer.parseInt(p1[0]);
		int y1 = Integer.parseInt(p1[1]);
		int z1 = Integer.parseInt(p1[2]);
		int x2 = Integer.parseInt(p2[0]);
		int y2 = Integer.parseInt(p2[1]);
		int z2 = Integer.parseInt(p2[2]);
		
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
	}

	@Override
	public boolean inZone(Location location) {
		if (!worldName.equalsIgnoreCase(location.getWorld().getName())) return false;
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		return minX <= x && x <= maxX && minY <= y && y <= maxY && minZ <= z && z <= maxZ;
	}
	
}
