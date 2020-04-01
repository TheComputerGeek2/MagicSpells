package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;

public class WalkwayJutsu extends BuffJutsu {

	private Map<UUID, Platform> platforms;

	private Material material;
	private int size;

	private WalkwayListener listener;
	
	public WalkwayJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		String materialName = getConfigString("platform-type", "OAK_WOOD").toUpperCase();
		material = Material.getMaterial(materialName);
		if (material == null || !material.isBlock()) {
			MagicJutsus.error("WalkwayJutsu '" + internalName + "' has an invalid platform-type defined!");
			material = null;
		}

		size = getConfigInt("size", 6);

		platforms = new HashMap<>();
	}
	
	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		platforms.put(entity.getUniqueId(), new Platform(entity, material, size));
		registerListener();
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return platforms.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		Platform platform = platforms.remove(entity.getUniqueId());
		if (platform == null) return;

		platform.remove();
		unregisterListener();
	}

	@Override
	protected void turnOff() {
		Util.forEachValueOrdered(platforms, Platform::remove);

		platforms.clear();
		unregisterListener();
	}
	
	private void registerListener() {
		if (listener != null) return;
		listener = new WalkwayListener();
		registerEvents(listener);
	}
	
	private void unregisterListener() {
		if (listener == null || !platforms.isEmpty()) return;
		unregisterEvents(listener);
		listener = null;
	}
	
	public class WalkwayListener implements Listener {
	
		@EventHandler(priority=EventPriority.MONITOR)
		public void onPlayerMove(PlayerMoveEvent event) {
			Player player = event.getPlayer();
			Platform carpet = platforms.get(player.getUniqueId());
			if (carpet == null) return;
			boolean moved = carpet.move();
			if (moved) addUseAndChargeCost(player);
		}
	
		@EventHandler(ignoreCancelled=true)
		public void onBlockBreak(BlockBreakEvent event) {
			Block block = event.getBlock();
			for (Platform platform : platforms.values()) {
				if (!platform.blockInPlatform(block)) continue;

				event.setCancelled(true);
				return;
			}
		}
		
	}
	
	private class Platform {
		
		private LivingEntity entity;
		private Material materialPlatform;
		private int sizePlatform;
		private List<Block> platform;
		
		private int prevX;
		private int prevZ;
		private int prevDirX;
		private int prevDirY;
		private int prevDirZ;
		
		private Platform(LivingEntity entity, Material material, int size) {
			this.entity = entity;
			this.materialPlatform = material;
			this.sizePlatform = size;
			this.platform = new ArrayList<>();
			
			move();
		}

		private boolean move() {
			Block origin = entity.getLocation().subtract(0, 1, 0).getBlock();

			int dirX = 0;
			int dirY = 0;
			int dirZ = 0;

			int x = origin.getX();
			int z = origin.getZ();

			Vector dir = entity.getLocation().getDirection().setY(0).normalize();
			if (dir.getX() > .7) dirX = 1;
			else if (dir.getX() < -.7) dirX = -1;
			else dirX = 0;

			if (dir.getZ() > .7) dirZ = 1;
			else if (dir.getZ() < -.7) dirZ = -1;
			else dirZ = 0;

			double pitch = entity.getLocation().getPitch();
			if (this.prevDirY == 0) {
				if (pitch < -40) dirY = 1;
				else if (pitch > 40) dirY = -1;
				else dirY = prevDirY;
			}
			else if (prevDirY == 1 && pitch > -10) dirY = 0;
			else if (prevDirY == -1 && pitch < 10) dirY = 0;
			else dirY = prevDirY;
			
			if (x != prevX || z != prevZ || dirX != prevDirX || dirY != prevDirY || dirZ != prevDirZ) {

				if (BlockUtils.isAir(origin.getType())) {
					// Check for weird stair positioning
					Block up = origin.getRelative(0, 1, 0);
					if (up != null && ((materialPlatform == Material.OAK_WOOD && up.getType() == Material.OAK_STAIRS) || (materialPlatform == Material.COBBLESTONE && up.getType() == Material.COBBLESTONE_STAIRS))) {
						origin = up;
					} else {					
						// Allow down movement when stepping out over an edge
						Block down = origin.getRelative(0, -1, 0);
						if (down != null && !BlockUtils.isAir(down.getType())) origin = down;
					}
				}
				
				drawCarpet(origin, dirX, dirY, dirZ);
				
				prevX = x;
				prevZ = z;
				prevDirX = dirX;
				prevDirY = dirY;
				prevDirZ = dirZ;
				
				return true;
			}
			return false;
		}

		private boolean blockInPlatform(Block block) {
			return platform.contains(block);
		}
		
		public void remove() {
			platform.stream().forEachOrdered(b -> b.setType(Material.AIR));
		}

		private void drawCarpet(Block origin, int dirX, int dirY, int dirZ) {
			// Determine block type and maybe stair direction
			Material mat = materialPlatform;
			if ((materialPlatform == Material.OAK_WOOD || materialPlatform == Material.COBBLESTONE) && dirY != 0) {
				if (materialPlatform == Material.OAK_WOOD) mat = Material.OAK_STAIRS;
				else if (materialPlatform == Material.COBBLESTONE) mat = Material.COBBLESTONE_STAIRS;
			}
			
			// Get platform blocks
			List<Block> blocks = new ArrayList<>();
			blocks.add(origin); // Add standing block
			for (int i = 1; i < sizePlatform; i++) { // Add blocks ahead
				Block b = origin.getRelative(dirX * i, dirY * i, dirZ * i);
				if (b == null) continue;
				blocks.add(b);
			}
			
			// Remove old blocks
			Iterator<Block> iter = platform.iterator();
			while (iter.hasNext()) {
				Block b = iter.next();
				if (!blocks.contains(b)) {
					b.setType(Material.AIR);
					iter.remove();
				}
			}
			
			// Set new blocks
			for (Block b : blocks) {
				if (platform.contains(b) || BlockUtils.isAir(b.getType())) {
					BlockUtils.setTypeAndData(b, mat, mat.createBlockData(), false);
					platform.add(b);
				}
			}
		}
		
	}

}
