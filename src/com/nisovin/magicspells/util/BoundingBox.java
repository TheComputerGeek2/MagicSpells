package com.nisovin.magicspells.util;

import com.nisovin.magicspells.util.bounds.Space;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class BoundingBox implements Space {

	World world;
	double lowX;
	double lowY;
	double lowZ;
	double highX;
	double highY;
	double highZ;
	double horizRadius;
	double vertRadius;
	
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
		this.world = corner1.getWorld();
		this.lowX = min(corner1.getX(), corner2.getX());
		this.highX = max(corner1.getX(), corner2.getX());
		this.lowY = min(corner1.getY(), corner2.getY());
		this.highY = max(corner1.getY(), corner2.getY());
		this.lowZ = min(corner1.getZ(), corner2.getZ());
		this.highZ = max(corner1.getZ(), corner2.getZ());
	}
	
	public void setCenter(Location center) {
		this.world = center.getWorld();
		this.lowX = center.getX() - this.horizRadius;
		this.lowY = center.getY() - this.vertRadius;
		this.lowZ = center.getZ() - this.horizRadius;
		this.highX = center.getX() + this.horizRadius;
		this.highY = center.getY() + this.vertRadius;
		this.highZ = center.getZ() + this.horizRadius;
	}
	
	public void expand(double amount) {
		this.lowX -= amount;
		this.lowY -= amount;
		this.lowZ -= amount;
		this.highX += amount;
		this.highY += amount;
		this.highZ += amount;
	}
	
	@Override
	public boolean contains(Location location) {
		if (!location.getWorld().equals(this.world)) return false;
		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();
		return this.lowX <= x && x <= this.highX && this.lowY <= y && y <= this.highY && this.lowZ <= z && z <= this.highZ;
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
