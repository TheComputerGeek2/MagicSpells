package com.nisovin.magicjutsus.util;

import java.util.Objects;

import org.bukkit.World;
import org.bukkit.Location;

import com.nisovin.magicjutsus.MagicJutsus;

public class MagicLocation {
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private String world;
	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public MagicLocation(String world, int x, int y, int z) {
		this(world, x, y, z, 0, 0);
	}
	
	public MagicLocation(Location l) {
		this(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
	}
	
	public MagicLocation(String world, double x, double y, double z, float yaw, float pitch) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}
	
	// -------------------------------------------- //
	// ACCESS
	// -------------------------------------------- //
	
	public Location getLocation() {
		World realWorld = MagicJutsus.plugin.getServer().getWorld(world);
		if (realWorld == null) return null;
		return new Location(realWorld, x, y, z, yaw, pitch);
	}
	
	public String getWorld() {
		return world;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double getZ() {
		return z;
	}
	
	public float getYaw() {
		return yaw;
	}
	
	public float getPitch() {
		return pitch;
	}
	
	// -------------------------------------------- //
	// HASHCODE
	// -------------------------------------------- //
	
	@Override
	public int hashCode() {
		return Objects.hash(
			world,
			x,
			y,
			z,
			pitch,
			yaw
		);
	}
	
	// -------------------------------------------- //
	// EQUALS
	// -------------------------------------------- //
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MagicLocation) {
			MagicLocation loc = (MagicLocation) o;
			return loc.world.equals(world) && loc.x == x && loc.y == y && loc.z == z && loc.yaw == yaw && loc.pitch == pitch;
		} else if (o instanceof Location) {
			Location loc = (Location) o;
			if (!LocationUtil.isSameWorld(loc, world)) return false;
			if (loc.getX() != x) return false;
			if (loc.getY() != y) return false;
			if (loc.getZ() != z) return false;
			if (loc.getYaw() != yaw) return false;
			if (loc.getPitch() != pitch) return false;
			return true;
		}
		return false;
	}
	
}
