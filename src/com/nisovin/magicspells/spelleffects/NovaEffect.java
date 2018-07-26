package com.nisovin.magicspells.spelleffects;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;

import com.nisovin.magicspells.materials.MagicBlockMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.util.SpellAnimation;
import com.nisovin.magicspells.materials.MagicMaterial;
public class NovaEffect extends SpellEffect {
	
	MagicMaterial mat;
	
	int radius = 3;
	int novaTickInterval = 5;
	int expandingRadiusChange = 1;
	int startRadius = 0;
	int heightPerTick = 0;
	
	double range = 20;
	
	boolean circleShape = false;
	boolean removePreviousBlocks = true;
	
	@Override
	public void loadFromString(String string) {
		if (string != null && !string.isEmpty()) {
			
			String[] params = string.split(" ");
			Material type = Material.FIRE;
			
			if (params.length >= 1) {
				try {
					type = Material.valueOf(params[0]);
				} catch (Exception e) {
					DebugHandler.debugNumberFormat(e);
				}
			}

			mat = new MagicBlockMaterial(type);
			
			if (params.length >= 2) {
				try {
					radius = Integer.parseInt(params[2]);
				} catch (NumberFormatException e) {
					DebugHandler.debugNumberFormat(e);
				}
			}
			
			if (params.length >= 3) {
				try {
					novaTickInterval = Integer.parseInt(params[3]);
				} catch (NumberFormatException e) {
					DebugHandler.debugNumberFormat(e);
				}
			}
		}
	}
	
	@Override
	public void loadFromConfig(ConfigurationSection config) {
		mat = MagicSpells.getItemNameResolver().resolveBlock(config.getString("type", "fire"));
		radius = config.getInt("radius", radius);
		startRadius = config.getInt("start-radius", startRadius);
		novaTickInterval = config.getInt("expand-interval", novaTickInterval);
		expandingRadiusChange = config.getInt("expanding-radius-change", expandingRadiusChange);
		if (expandingRadiusChange < 1) expandingRadiusChange = 1;
		
		range = Math.max(config.getDouble("range", range), 1);
		removePreviousBlocks = config.getBoolean("remove-previous-blocks", removePreviousBlocks);
		heightPerTick = config.getInt("height-per-tick", heightPerTick);
		circleShape = config.getBoolean("circle-shape", circleShape);
	}
	
	@Override
	public Runnable playEffectLocation(Location location) {
		if (mat == null) return null;
		
		// Get nearby players
		Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, range, range, range);
		List<Player> nearby = new ArrayList<>();
		for (Entity e : nearbyEntities) {
			if (!(e instanceof Player)) continue;
			nearby.add((Player)e);
		}
		
		// Start animation
		if (!circleShape) new NovaAnimationSquare(nearby, location.getBlock(), mat, radius, novaTickInterval, expandingRadiusChange);
		else new NovaAnimationCircle(nearby, location.getBlock(), mat, radius, novaTickInterval, expandingRadiusChange);
		return null;
	}
	
	private class NovaAnimationSquare extends SpellAnimation {
		
		List<Player> nearby;
		Set<Block> blocks;
		Block center;
		MagicMaterial matNova;
		int radiusNova;
		int radiusChange;
		
		public NovaAnimationSquare(List<Player> nearby, Block center, MagicMaterial mat, int radius, int tickInterval, int activeRadiusChange) {
			super(tickInterval, true);
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.radiusNova = radius;
			this.blocks = new HashSet<>();
			this.radiusChange = activeRadiusChange;
		}
		
		@Override
		protected void onTick(int tick) {
			tick += startRadius;
			tick *= radiusChange;
			
			// Remove old blocks
			if (removePreviousBlocks) {
				for (Block b : blocks) {
					for (Player p : nearby) Util.restoreFakeBlockChange(p, b);
				}
				blocks.clear();
			}
			
			if (tick > radiusNova + 1) {
				stop();
				return;
			} else if (tick > radiusNova) {
				return;
			}
			
			// Set next ring
			int bx = center.getX();
			int y = center.getY();
			int bz = center.getZ();
			y += tick * heightPerTick;
			
			for (int x = bx - tick; x <= bx + tick; x++) {
				for (int z = bz - tick; z <= bz + tick; z++) {
					if (Math.abs(x - bx) != tick && Math.abs(z - bz) != tick) continue;
					
					Block b = center.getWorld().getBlockAt(x, y, z);
					if (b.getType() == Material.AIR || b.getType() == Material.TALL_GRASS) {
						Block under = b.getRelative(BlockFace.DOWN);
						if (under.getType() == Material.AIR || under.getType() == Material.TALL_GRASS) b = under;
					} else if (b.getRelative(BlockFace.UP).getType() == Material.AIR || b.getRelative(BlockFace.UP).getType() == Material.TALL_GRASS) {
						b = b.getRelative(BlockFace.UP);
					}
					
					if (b.getType() != Material.AIR && b.getType() != Material.TALL_GRASS) continue;
					
					if (blocks.contains(b)) continue;
					for (Player p : nearby) Util.sendFakeBlockChange(p, b, matNova);
					blocks.add(b);
				}
			}
		}
		
		@Override
		protected void stop() {
			super.stop();
			for (Block b : blocks) {
				for (Player p : nearby) Util.restoreFakeBlockChange(p, b);
			}
			blocks.clear();
		}
		
	}
	
	private class NovaAnimationCircle extends SpellAnimation {
		
		List<Player> nearby;
		Set<Block> blocks;
		Block center;
		MagicMaterial matNova;
		int radiusNova;
		int radiusChange;
		
		public NovaAnimationCircle(List<Player> nearby, Block center, MagicMaterial mat, int radius, int tickInterval, int activeRadiusChange) {
			super(tickInterval, true);
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.radiusNova = radius;
			this.blocks = new HashSet<>();
			this.radiusChange = activeRadiusChange;
		}
		
		@Override
		protected void onTick(int tick) {
			tick += startRadius;
			tick *= radiusChange;
			
			// Remove old blocks
			if (removePreviousBlocks) {
				for (Block b : blocks) {
					for (Player p : nearby) {
						Util.restoreFakeBlockChange(p, b);
					}
				}
				blocks.clear();
			}
			
			if (tick > radiusNova + 1) {
				stop();
				return;
			} else if (tick > radiusNova) {
				return;
			}
			
			// Generate the bottom block
			Location centerLocation = center.getLocation().clone();
			centerLocation.add(0.5, tick * heightPerTick, 0.5);
			Block b;
			
			if (startRadius == 0 && tick == 0) {
				b = centerLocation.getWorld().getBlockAt(centerLocation);
				
				if (b.getType() == Material.AIR || b.getType() == Material.TALL_GRASS) {
					Block under = b.getRelative(BlockFace.DOWN);
					if (under.getType() == Material.AIR || under.getType() == Material.TALL_GRASS) b = under;
				} else if (b.getRelative(BlockFace.UP).getType() == Material.AIR || b.getRelative(BlockFace.UP).getType() == Material.TALL_GRASS) {
					b = b.getRelative(BlockFace.UP);
				}
				
				if (b.getType() != Material.AIR && b.getType() != Material.TALL_GRASS) return;
				
				if (blocks.contains(b)) return;
				for (Player p : nearby) Util.sendFakeBlockChange(p, b, matNova);
				blocks.add(b);
			}
			
			// Generate the circle
			Vector v;
			double angle, x, z;
			double amount = tick * 64;
			double inc = (2 * Math.PI) / amount;
			for (int i = 0; i < amount; i++) {
				angle = i * inc;
				x = tick * Math.cos(angle);
				z = tick * Math.sin(angle);
				v = new Vector(x, 0, z);
				b = center.getWorld().getBlockAt(centerLocation.add(v));
				centerLocation.subtract(v);
				
				if (b.getType() == Material.AIR || b.getType() == Material.TALL_GRASS) {
					Block under = b.getRelative(BlockFace.DOWN);
					if (under.getType() == Material.AIR || under.getType() == Material.TALL_GRASS) b = under;
				} else if (b.getRelative(BlockFace.UP).getType() == Material.AIR || b.getRelative(BlockFace.UP).getType() == Material.TALL_GRASS) {
					b = b.getRelative(BlockFace.UP);
				}
				
				if (b.getType() != Material.AIR && b.getType() != Material.TALL_GRASS) continue;
				
				if (blocks.contains(b)) continue;
				for (Player p : nearby) Util.sendFakeBlockChange(p, b, matNova);
				blocks.add(b);
			}
			
		}
		
		@Override
		protected void stop() {
			super.stop();
			for (Block b : blocks) {
				for (Player p : nearby) Util.restoreFakeBlockChange(p, b);
			}
			blocks.clear();
		}
		
	}
	
}
