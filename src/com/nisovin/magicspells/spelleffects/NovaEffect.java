package com.nisovin.magicspells.spelleffects;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;

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
import com.nisovin.magicspells.materials.MagicUnknownMaterial;

public class NovaEffect extends SpellEffect {

	private List<MagicMaterial> mats = new ArrayList<>();
	private MagicMaterial mat;
	
	private int radius = 3;
	private int novaTickInterval = 5;
	private int expandingRadiusChange = 1;
	private int startRadius = 0;
	private int heightPerTick = 0;
	
	private double range = 20;
	
	private boolean circleShape = false;
	private boolean removePreviousBlocks = true;
	
	@Override
	public void loadFromString(String string) {
		if (string != null && !string.isEmpty()) {
			
			String[] params = string.split(" ");
			int type = 51;
			byte data = 0;
			
			if (params.length >= 1) {
				try {
					type = Integer.parseInt(params[0]);
				} catch (NumberFormatException e) {
					DebugHandler.debugNumberFormat(e);
				}
			}
			
			if (params.length >= 2) {
				try {
					data = Byte.parseByte(params[1]);
				} catch (NumberFormatException e) {
					DebugHandler.debugNumberFormat(e);
				}
			}
			
			mat = new MagicUnknownMaterial(type, data);
			
			if (params.length >= 3) {
				try {
					radius = Integer.parseInt(params[2]);
				} catch (NumberFormatException e) {
					DebugHandler.debugNumberFormat(e);
				}
			}
			
			if (params.length >= 4) {
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
		List<String> types = config.getStringList("type");
		if (types.size() == 0) {
			String type = config.getString("type", "fire");
			mat = MagicSpells.getItemNameResolver().resolveBlock(type);
		} else {
			types.forEach(type -> {
				MagicMaterial material = MagicSpells.getItemNameResolver().resolveBlock(type);
				if (material != null) mats.add(material);
			});
			if (mats.size() == 0) mat = MagicSpells.getItemNameResolver().resolveBlock("fire");
		}
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
		if (mat == null && mats.size() == 0) return null;
		
		// Get nearby players
		Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, range, range, range);
		List<Player> nearby = new ArrayList<>();
		for (Entity e : nearbyEntities) {
			if (!(e instanceof Player)) continue;
			nearby.add((Player)e);
		}
		
		// Start animation
		if (!circleShape) {
			if (mats.size() == 0) new NovaAnimationSquare(nearby, location.getBlock(), mat, radius, novaTickInterval, expandingRadiusChange);
			else new NovaAnimationSquare(nearby, location.getBlock(), mats, radius, novaTickInterval, expandingRadiusChange);
		} else {
			if (mats.size() == 0) new NovaAnimationCircle(nearby, location.getBlock(), mat, radius, novaTickInterval, expandingRadiusChange);
			else new NovaAnimationCircle(nearby, location.getBlock(), mats, radius, novaTickInterval, expandingRadiusChange);
		}
		return null;
	}

	private class NovaAnimationSquare extends SpellAnimation {
		
		List<Player> nearby;
		Set<Block> blocks = new HashSet<>();
		Block center;
		List<MagicMaterial> matNovas = new ArrayList<>();
		MagicMaterial matNova;
		int radiusNova;
		int radiusChange;

		NovaAnimationSquare(List<Player> nearby, Block center, List<MagicMaterial> matNovas, int radiusNova, int tickInterval, int radiusChange) {
			super(tickInterval, true);
			this.nearby = nearby;
			this.center = center;
			this.matNovas = matNovas;
			this.radiusNova = radiusNova;
			this.radiusChange = radiusChange;
		}

		NovaAnimationSquare(List<Player> nearby, Block center, MagicMaterial mat, int radius, int tickInterval, int activeRadiusChange) {
			super(tickInterval, true);
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.radiusNova = radius;
			this.radiusChange = activeRadiusChange;
		}

		MagicMaterial getRandomMaterial() {
			if (matNovas.size() == 0) return matNova;
			else return matNovas.get(random.nextInt(matNovas.size()));
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
					if (b.getType() == Material.AIR || b.getType() == Material.LONG_GRASS) {
						Block under = b.getRelative(BlockFace.DOWN);
						if (under.getType() == Material.AIR || under.getType() == Material.LONG_GRASS) b = under;
					} else if (b.getRelative(BlockFace.UP).getType() == Material.AIR || b.getRelative(BlockFace.UP).getType() == Material.LONG_GRASS) {
						b = b.getRelative(BlockFace.UP);
					}
					
					if (b.getType() != Material.AIR && b.getType() != Material.LONG_GRASS) continue;
					
					if (blocks.contains(b)) continue;
					for (Player p : nearby) Util.sendFakeBlockChange(p, b, getRandomMaterial());
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
		Set<Block> blocks = new HashSet<>();
		Block center;
		List<MagicMaterial> matNovas;
		MagicMaterial matNova;
		int radiusNova;
		int radiusChange;
		
		NovaAnimationCircle(List<Player> nearby, Block center, List<MagicMaterial> mats, int radius, int tickInterval, int activeRadiusChange) {
			super(tickInterval, true);
			this.nearby = nearby;
			this.center = center;
			this.matNovas = mats;
			this.radiusNova = radius;
			this.radiusChange = activeRadiusChange;
		}

		NovaAnimationCircle(List<Player> nearby, Block center, MagicMaterial mat, int radiusNova, int tickInterval, int radiusChange) {
			super(tickInterval, true);
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.radiusNova = radiusNova;
			this.radiusChange = radiusChange;
		}

		MagicMaterial getRandomMaterial() {
			if (matNovas == null) return matNova;
			else return matNovas.get(random.nextInt(matNovas.size()));
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
				
				if (b.getType() == Material.AIR || b.getType() == Material.LONG_GRASS) {
					Block under = b.getRelative(BlockFace.DOWN);
					if (under.getType() == Material.AIR || under.getType() == Material.LONG_GRASS) b = under;
				} else if (b.getRelative(BlockFace.UP).getType() == Material.AIR || b.getRelative(BlockFace.UP).getType() == Material.LONG_GRASS) {
					b = b.getRelative(BlockFace.UP);
				}
				
				if (b.getType() != Material.AIR && b.getType() != Material.LONG_GRASS) return;
				
				if (blocks.contains(b)) return;
				for (Player p : nearby) Util.sendFakeBlockChange(p, b, getRandomMaterial());
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
				
				if (b.getType() == Material.AIR || b.getType() == Material.LONG_GRASS) {
					Block under = b.getRelative(BlockFace.DOWN);
					if (under.getType() == Material.AIR || under.getType() == Material.LONG_GRASS) b = under;
				} else if (b.getRelative(BlockFace.UP).getType() == Material.AIR || b.getRelative(BlockFace.UP).getType() == Material.LONG_GRASS) {
					b = b.getRelative(BlockFace.UP);
				}
				
				if (b.getType() != Material.AIR && b.getType() != Material.LONG_GRASS) continue;
				
				if (blocks.contains(b)) continue;
				for (Player p : nearby) Util.sendFakeBlockChange(p, b, getRandomMaterial());
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
