package com.nisovin.magicspells.util;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import com.nisovin.magicspells.util.bounds.Space;

public class BoundingBox implements Space {

	private World world;
	private double lowX;
	private double lowY;
	private double lowZ;
	private double highX;
	private double highY;
	private double highZ;
	private double horizontalRadius;
	private double verticalRadius;
	
	public BoundingBox(Block center, double radius) {
		this(center.getLocation().add(0.5, 0, 0.5), radius, radius);
	}
	
	public BoundingBox(Location center, double radius) {
		this(center, radius, radius);
	}
	
	public BoundingBox(Block center, double horizontalRadius, double verticalRadius) {
		this(center.getLocation().add(0.5, 0, 0.5), horizontalRadius, verticalRadius);
	}
	
	public BoundingBox(Location center, double horizontalRadius, double verticalRadius) {
		this.horizontalRadius = horizontalRadius;
		this.verticalRadius = verticalRadius;
		setCenter(center);
	}
	
	public BoundingBox(Location corner1, Location corner2) {
		world = corner1.getWorld();
		lowX = Math.min(corner1.getX(), corner2.getX());
		highX = Math.max(corner1.getX(), corner2.getX());
		lowY = Math.min(corner1.getY(), corner2.getY());
		highY = Math.max(corner1.getY(), corner2.getY());
		lowZ = Math.min(corner1.getZ(), corner2.getZ());
		highZ = Math.max(corner1.getZ(), corner2.getZ());
	}
	
	public void setCenter(Location center) {
		world = center.getWorld();
		lowX = center.getX() - horizontalRadius;
		lowY = center.getY() - verticalRadius;
		lowZ = center.getZ() - horizontalRadius;
		highX = center.getX() + horizontalRadius;
		highY = center.getY() + verticalRadius;
		highZ = center.getZ() + horizontalRadius;
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

	public double getHorizontalRadius() {
		return horizontalRadius;
	}

	public double getVerticalRadius() {
		return verticalRadius;
	}
	
}
