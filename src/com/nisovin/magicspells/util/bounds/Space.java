package com.nisovin.magicspells.util.bounds;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public interface Space {
	
	boolean contains(Location location);
	
	boolean contains(Entity entity);
	
	boolean contains(Block block);
	
}
