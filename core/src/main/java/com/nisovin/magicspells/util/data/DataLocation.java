package com.nisovin.magicspells.util.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class DataLocation {

	private static final Map<String, Function<Location, String>> dataElements = new HashMap<>();

	static {
		dataElements.put("location.biome", location -> location.getBlock().getBiome().toString());
		dataElements.put("location.block.data", DataLocation::blockData);
		dataElements.put("location.block.type", location -> location.getBlock().getType().name());
		dataElements.put("location.elevation", location -> location.getWorld().getHighestBlockYAt(location) + "");
		dataElements.put("location.light", location -> location.getBlock().getLightLevel() + "");
		dataElements.put("location.light.block", location -> location.getBlock().getLightFromBlocks() + "");
		dataElements.put("location.light.sky", location -> location.getBlock().getLightFromSky() + "");
		dataElements.put("location", Location::toString);
		dataElements.put("location.blockx", location -> location.getBlockX() + "");
		dataElements.put("location.blocky", location -> location.getBlockY() + "");
		dataElements.put("location.blockz", location -> location.getBlockZ() + "");
		dataElements.put("location.pitch", location -> location.getPitch() + "");
		dataElements.put("location.x", location -> location.getX() + "");
		dataElements.put("location.y", location -> location.getY() + "");
		dataElements.put("location.yaw", location -> location.getYaw() + "");
		dataElements.put("location.z", location -> location.getZ() + "");
		dataElements.put("world", location -> location.getWorld().toString());
		dataElements.put("world.name", location -> location.getWorld().getName());
	}

	private static String blockData(Location location) {
		Block block = location.getBlock();
		return block.getBlockData().getAsString();
	}

	public static Function<Location, String> getDataFunction(String elementId) {
		return dataElements.get(elementId);
	}

}