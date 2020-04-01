package com.nisovin.magicjutsus.util;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import com.nisovin.magicjutsus.util.bounds.Space;

public class BoundingBox implements Space {

	private World world;
	private double lowX;
	private double lowY;
	private double lowZ;
	private double highX;
	private double highY;
	private double highZ;
	private double horizRadius;
	private double vertRadius;
	
	public BoundingBox(Block center, double radius) {
		this(center.getLocation().add(0.5, 0, 0.5), radius, radius);
	}
	
	public BoundingBox(Location center, double radius) {
		this(center, radius, radius);
	}
	
	public BoundingBox(Block center, double horizRadius, double vertRadius) {
		this(center.getLocation().add(0.5, 0, 0.5), horizRadius, vertRadius);
	}
	
	public BoundingBox(Location center, double horizRadius, double vertRadius) {
		this.horizRadius = horizRadius;
		this.vertRadius = vertRadius;
		setCenter(center);
	}
	
	public BoundingBox(Location corner1, Location corner2) {
		world = corner1.getWorld();
		lowX = min(corner1.getX(), corner2.getX());
		highX = max(corner1.getX(), corner2.getX());
		lowY = min(corner1.getY(), corner2.getY());
		highY = max(corner1.getY(), corner2.getY());
		lowZ = min(corner1.getZ(), corner2.getZ());
		highZ = max(corner1.getZ(), corner2.getZ());
	}
	
	public void setCenter(Location center) {
		world = center.getWorld();
		lowX = center.getX() - horizRadius;
		lowY = center.getY() - vertRadius;
		lowZ = center.getZ() - horizRadius;
		highX = center.getX() + horizRadius;
		highY = center.getY() + vertRadius;
		highZ = center.getZ() + horizRadius;
	}
	
	public void expand(double amount) {
		lowX -= amount;
		lowY -= amount;
		lowZ -= amount;
		highX += amount;
		highY += amount;
		highZ += amount;
	}
	
	@Override
	public boolean contains(Location location) {
		if (!location.getWorld().equals(world)) return false;
		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();
		return lowX <= x && x <= highX && lowY <= y && y <= highY && lowZ <= z && z <= highZ;
	}
	
	@Override
	public boolean contains(Entity entity) {
		return contains(entity.getLocation());
	}
	
	@Override
	public boolean contains(Block block) {
		return contains(block.getLocation().add(0.5, 0, 0.5));
	}
	
	private double min(double d1, double d2) {
		return d1 < d2 ? d1 : d2;
	}
	
	private double max(double d1, double d2) {
		return d1 > d2 ? d1 : d2;
	}
	
}
