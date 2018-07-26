package com.nisovin.magicspells.spells.buff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.nisovin.magicspells.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.util.MagicConfig;

public class WalkwaySpell extends BuffSpell {

	private Material material;
	private int size;
	private boolean cancelOnTeleport;
	
	HashMap<String, Platform> platforms;
	private WalkwayListener listener;
	
	public WalkwaySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		this.material = MagicSpells.getItemNameResolver().resolveBlock(getConfigString("platform-type", "oak_wood")).getMaterial();
		this.size = getConfigInt("size", 6);
		this.cancelOnTeleport = getConfigBoolean("cancel-on-teleport", true);
		
		this.platforms = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();
		if (this.cancelOnTeleport) registerEvents(new TeleportListener());
	}
	
	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		this.platforms.put(player.getName(), new Platform(player, this.material, this.size));
		registerListener();
		return true;
	}
	
	private void registerListener() {
		if (this.listener != null) return;
		this.listener = new WalkwayListener();
		registerEvents(this.listener);
	}
	
	private void unregisterListener() {
		if (this.listener != null && this.platforms.isEmpty()) {
			unregisterEvents(this.listener);
			this.listener = null;
		}
	}
	
	public class WalkwayListener implements Listener {
	
		@EventHandler(priority=EventPriority.MONITOR)
		public void onPlayerMove(PlayerMoveEvent event) {
			Player player = event.getPlayer();
			Platform carpet = platforms.get(player.getName());
			if (carpet != null) {
				boolean moved = carpet.move();
				if (moved) {
					addUseAndChargeCost(player);
				}
			}
		}
	
		@EventHandler(ignoreCancelled=true)
		public void onBlockBreak(BlockBreakEvent event) {
			Block block = event.getBlock();
			for (Platform platform : platforms.values()) {
				if (platform.blockInPlatform(block)) {
					event.setCancelled(true);
					return;
				}
			}
		}
		
	}

	public class TeleportListener implements Listener {
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerTeleport(PlayerTeleportEvent event) {
			Player player = event.getPlayer();
			if (!platforms.containsKey(player.getName())) return;
			Location locationFrom = event.getFrom();
			Location locationTo = event.getTo();
			if (LocationUtil.differentWorldDistanceGreaterThan(locationFrom, locationTo, 50)) {
				turnOff(player);
			}
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerPortal(PlayerPortalEvent event) {
			Player player = event.getPlayer();
			if (!platforms.containsKey(player.getName())) return;
			turnOff(player);
		}
		
	}
	
	@Override
	public void turnOffBuff(Player player) {
		Platform platform = this.platforms.remove(player.getName());
		if (platform == null) return;
		platform.remove();
		unregisterListener();
	}

	@Override
	protected void turnOff() {
		Util.forEachValueOrdered(platforms, Platform::remove);
		this.platforms.clear();
		unregisterListener();
	}
	
	private class Platform {
		
		private Player player;
		private Material materialPlatform;
		private int sizePlatform;
		private List<Block> platform;
		
		private int prevX;
		private int prevZ;
		private int prevDirX;
		private int prevDirY;
		private int prevDirZ;
		
		public Platform(Player player, Material material, int size) {
			this.player = player;
			this.materialPlatform = material;
			this.sizePlatform = size;
			this.platform = new ArrayList<>();
			
			move();
		}
		
		public boolean move() {
			Block origin = this.player.getLocation().subtract(0, 1, 0).getBlock();
			int x = origin.getX();
			int z = origin.getZ();
			int dirX = 0;
			int dirY = 0;
			int dirZ = 0;
			
			Vector dir = this.player.getLocation().getDirection().setY(0).normalize();
			if (dir.getX() > .7) {
				dirX = 1;
			} else if (dir.getX() < -.7) {
				dirX = -1;
			} else {
				dirX = 0;
			}
			if (dir.getZ() > .7) {
				dirZ = 1;
			} else if (dir.getZ() < -.7) {
				dirZ = -1;
			} else {
				dirZ = 0;
			}
			double pitch = this.player.getLocation().getPitch();
			if (this.prevDirY == 0) {
				if (pitch < -40) {
					dirY = 1;
				} else if (pitch > 40) {
					dirY = -1;
				} else {
					dirY = this.prevDirY;
				}
			} else if (this.prevDirY == 1 && pitch > -10) {
				dirY = 0;
			} else if (this.prevDirY == -1 && pitch < 10) {
				dirY = 0;
			} else {
				dirY = this.prevDirY;
			}
			
			if (x != this.prevX || z != this.prevZ || dirX != this.prevDirX || dirY != this.prevDirY || dirZ != this.prevDirZ) {
				
				if (origin.getType() == Material.AIR) {
					// Check for weird stair positioning
					Block up = origin.getRelative(0, 1, 0);
					if (up != null && ((this.materialPlatform.name().endsWith("WOOD")&& up.getType().name().endsWith("STAIRS")) || (this.materialPlatform == Material.COBBLESTONE && up.getType() == Material.COBBLESTONE_STAIRS))) {
						origin = up;
					} else {					
						// Allow down movement when stepping out over an edge
						Block down = origin.getRelative(0, -1, 0);
						if (down != null && down.getType() != Material.AIR) {
							origin = down;
						}
					}
				}
				
				drawCarpet(origin, dirX, dirY, dirZ);
				
				this.prevX = x;
				this.prevZ = z;
				this.prevDirX = dirX;
				this.prevDirY = dirY;
				this.prevDirZ = dirZ;
				
				return true;
			}
			return false;
		}
		
		public boolean blockInPlatform(Block block) {
			return this.platform.contains(block);
		}
		
		public void remove() {
			this.platform.stream().forEachOrdered(b -> b.setType(Material.AIR));
		}
		
		public void drawCarpet(Block origin, int dirX, int dirY, int dirZ) {
			// Determine block type and maybe stair direction
			Material mat = this.materialPlatform;
			byte data = 0;
			if ((this.materialPlatform.name().endsWith("WOOD") || this.materialPlatform == Material.COBBLESTONE) && dirY != 0) {
				boolean changed = false;
				if (dirY == -1) {
					if (dirX == -1 && dirZ == 0) {
						data = 0;
						changed = true;
					} else if (dirX == 1 && dirZ == 0) {
						data = 1;
						changed = true;
					} else if (dirZ == -1 && dirX == 0) {
						data = 2;
						changed = true;
					} else if (dirZ == 1 && dirX == 0) {
						data = 3;
						changed = true;
					}
				} else if (dirY == 1) {
					if (dirX == -1 && dirZ == 0) {
						data = 1;
						changed = true;
					} else if (dirX == 1 && dirZ == 0) {
						data = 0;
						changed = true;
					} else if (dirZ == -1 && dirX == 0) {
						data = 3;
						changed = true;
					} else if (dirZ == 1 && dirX == 0) {
						data = 2;
						changed = true;
					}
				}
				if (changed) {
					if (this.materialPlatform == Material.OAK_WOOD) {
						mat = Material.OAK_STAIRS;
					} else if (this.materialPlatform == Material.COBBLESTONE) {
						mat = Material.COBBLESTONE_STAIRS;
					}
				}
			}
			
			// Get platform blocks
			List<Block> blocks = new ArrayList<>();
			blocks.add(origin); // Add standing block
			for (int i = 1; i < this.sizePlatform; i++) { // Add blocks ahead
				Block b = origin.getRelative(dirX * i, dirY * i, dirZ * i);
				if (b == null) continue;
				blocks.add(b);
			}
			
			// Remove old blocks
			Iterator<Block> iter = this.platform.iterator();
			while (iter.hasNext()) {
				Block b = iter.next();
				if (!blocks.contains(b)) {
					b.setType(Material.AIR);
					iter.remove();
				}
			}
			
			// Set new blocks
			for (Block b : blocks) {
				if (this.platform.contains(b) || b.getType() == Material.AIR) {
					BlockUtils.setTypeAndData(b, mat, false);
					this.platform.add(b);
				}
			}
		}
		
	}

	@Override
	public boolean isActive(Player player) {
		return this.platforms.containsKey(player.getName());
	}

}
