package com.nisovin.magicspells.util.bounds;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

public class SpaceCombined extends LinkedList<Space> implements Space {
	
	public SpaceCombined(Space... spaces) {
		this(Arrays.asList(spaces));
	}
	
	public SpaceCombined(Collection<? extends Space> spaces) {
		super(spaces);
	}
	
	@Override
	public boolean contains(Location location) {
		return stream().anyMatch(space -> space.contains(location));
	}
	
	@Override
	public boolean contains(Entity entity) {
		return stream().anyMatch(space -> space.contains(entity));
	}
	
	@Override
	public boolean contains(Block block) {
		return stream().anyMatch(space -> space.contains(block));
	}
	
}
