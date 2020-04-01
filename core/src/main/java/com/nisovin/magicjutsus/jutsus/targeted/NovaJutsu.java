package com.nisovin.magicjutsus.jutsus.targeted;

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
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class NovaJutsu extends TargetedJutsu implements TargetedLocationJutsu, TargetedEntityJutsu {

	private Material material;
	private String materialName;

	private Vector relativeOffset;

	private Ninjutsu jutsuOnEnd;
	private Ninjutsu locationJutsu;
	private Ninjutsu jutsuOnWaveRemove;
	private String jutsuOnEndName;
	private String locationJutsuName;
	private String jutsuOnWaveRemoveName;

	private int radius;
	private int startRadius;
	private int heightPerTick;
	private int novaTickInterval;
	private int expandingRadiusChange;

	private double visibleRange;

	private boolean pointBlank;
	private boolean circleShape;
	private boolean removePreviousBlocks;
	
	public NovaJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		materialName = getConfigString("type", "water").toUpperCase();
		material = Material.getMaterial(materialName);
		if (material == null || !material.isBlock()) {
			MagicJutsus.error("NovaJutsu '" + internalName + "' has an invalid block type defined!");
			material = null;
		}
		
		relativeOffset = getConfigVector("relative-offset", "0,0,0");

		jutsuOnEndName = getConfigString("jutsu-on-end", "");
		locationJutsuName = getConfigString("jutsu", "");
		jutsuOnWaveRemoveName = getConfigString("jutsu-on-wave-remove", "");
		
		radius = getConfigInt("radius", 3);
		startRadius = getConfigInt("start-radius", 0);
		heightPerTick = getConfigInt("height-per-tick", 0);
		novaTickInterval = getConfigInt("expand-interval", 5);
		expandingRadiusChange = getConfigInt("expanding-radius-change", 1);
		if (expandingRadiusChange < 1) expandingRadiusChange = 1;
		
		visibleRange = Math.max(getConfigDouble("visible-range", 20), 20);

		pointBlank = getConfigBoolean("point-blank", true);
		circleShape = getConfigBoolean("circle-shape", false);
		removePreviousBlocks = getConfigBoolean("remove-previous-blocks", true);
		
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		locationJutsu = new Ninjutsu(locationJutsuName);
		if (!locationJutsu.process() || !locationJutsu.isTargetedLocationJutsu()) {
			if (!locationJutsuName.isEmpty()) MagicJutsus.error("NovaJutsu " + internalName + " has an invalid jutsu defined!");
			locationJutsu = null;
		}
		
		jutsuOnWaveRemove = new Ninjutsu(jutsuOnWaveRemoveName);
		if (!jutsuOnWaveRemove.process() || !jutsuOnWaveRemove.isTargetedLocationJutsu()) {
			if (!jutsuOnWaveRemoveName.isEmpty()) MagicJutsus.error("NovaJutsu " + internalName + " has an invalid jutsu-on-wave-remove defined!");
			jutsuOnWaveRemove = null;
		}
		
		jutsuOnEnd = new Ninjutsu(jutsuOnEndName);
		if (!jutsuOnEnd.process() || !jutsuOnEnd.isTargetedLocationJutsu()) {
			if (!jutsuOnEndName.isEmpty()) MagicJutsus.error("NovaJutsu " + internalName + " has an invalid jutsu-on-end defined!");
			jutsuOnEnd = null;
		}
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState jutsuCastState, float power, String[] strings) {
		if (jutsuCastState == JutsuCastState.NORMAL) {
			Location loc;
			if (pointBlank) loc = livingEntity.getLocation();
			else loc = getTargetedBlock(livingEntity, power).getLocation();
			
			createNova(livingEntity, loc, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity livingEntity, float v) {
		createNova(caster, livingEntity.getLocation(), v);
		return false;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity livingEntity, float v) {
		return false;
	}
	
	@Override
	public boolean castAtLocation(LivingEntity livingEntity, Location location, float v) {
		createNova(livingEntity, location, v);
		return false;
	}
	
	@Override
	public boolean castAtLocation(Location location, float v) {
		return false;
	}
	
	private void createNova(LivingEntity livingEntity, Location loc, float power) {
		if (material == null) return;
		// Relative offset
		Location startLoc = loc.clone();
		Vector direction = livingEntity.getLocation().getDirection().normalize();
		Vector horizOffset = new Vector(-direction.getZ(), 0.0, direction.getX()).normalize();
		startLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		startLoc.add(direction.setY(0).normalize().multiply(relativeOffset.getX()));
		startLoc.add(0, relativeOffset.getY(), 0);
		
		// Get nearby players
		Collection<Entity> nearbyEntities = startLoc.getWorld().getNearbyEntities(startLoc, visibleRange, visibleRange, visibleRange);
		List<Player> nearby = new ArrayList<>();
		for (Entity e : nearbyEntities) {
			if (!(e instanceof Player)) continue;
			nearby.add((Player) e);
		}
		
		// Start tracker
		if (!circleShape) new NovaTrackerSquare(nearby, startLoc.getBlock(), material, livingEntity, radius, novaTickInterval, expandingRadiusChange, power);
		else new NovaTrackerCircle(nearby, startLoc.getBlock(), material, livingEntity, radius, novaTickInterval, expandingRadiusChange, power);
	}
	
	private class NovaTrackerSquare implements Runnable {
		
		private Material matNova;
		private List<Player> nearby;
		private Set<Block> blocks;
		private LivingEntity caster;
		private Block center;
		private float power;
		private int radiusNova;
		private int radiusChange;
		private int taskId;
		private int count;
		private int temp;

		private NovaTrackerSquare(List<Player> nearby, Block center, Material mat, LivingEntity caster, int radius, int tickInterval, int activeRadiusChange, float power) {
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.caster = caster;
			this.power = power;
			this.radiusNova = radius;
			this.blocks = new HashSet<>();
			this.radiusChange = activeRadiusChange;
			this.taskId = MagicJutsus.scheduleRepeatingTask(this, 0, tickInterval);
			
			this.count = 0;
			this.temp = 0;
		}
		
		@Override
		public void run() {
			temp = count;
			temp += startRadius;
			temp *= radiusChange;
			count++;
			
			if (removePreviousBlocks) {
				for (Block b : blocks) {
					for (Player p : nearby) p.sendBlockChange(b.getLocation(), b.getBlockData());
					if (jutsuOnWaveRemove != null) jutsuOnWaveRemove.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
				}
				blocks.clear();
			}
			
			if (temp > radiusNova + 1) {
				stop();
				return;
			} else if (temp > radiusNova) {
				return;
			}
			
			int bx = center.getX();
			int y = center.getY();
			int bz = center.getZ();
			y += count * heightPerTick;
			
			for (int x = bx - temp; x <= bx + temp; x++) {
				for (int z = bz - temp; z <= bz + temp; z++) {
					if (Math.abs(x - bx) != temp && Math.abs(z - bz) != temp) continue;
					
					Block b = center.getWorld().getBlockAt(x, y, z);
					if (BlockUtils.isAir(b.getType()) || b.getType() == Material.TALL_GRASS) {
						Block under = b.getRelative(BlockFace.DOWN);
						if (BlockUtils.isAir(under.getType()) || under.getType() == Material.TALL_GRASS) b = under;
					} else if (BlockUtils.isAir(b.getRelative(BlockFace.UP).getType()) || b.getRelative(BlockFace.UP).getType() == Material.TALL_GRASS) {
						b = b.getRelative(BlockFace.UP);
					}
					
					if (!BlockUtils.isAir(b.getType()) && b.getType() != Material.TALL_GRASS) continue;
					
					if (blocks.contains(b)) continue;
					for (Player p : nearby) p.sendBlockChange(b.getLocation(), matNova.createBlockData());
					blocks.add(b);
					if (locationJutsu != null) locationJutsu.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
				}
			}
			
		}

		private void stop() {
			for (Block b : blocks) {
				for (Player p : nearby) p.sendBlockChange(b.getLocation(), b.getBlockData());
				if (jutsuOnEnd != null) jutsuOnEnd.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
			}
			blocks.clear();
			MagicJutsus.cancelTask(taskId);
		}
		
	}
	
	private class NovaTrackerCircle implements Runnable {

		private Material matNova;
		private List<Player> nearby;
		private Set<Block> blocks;
		private LivingEntity caster;
		private Block center;
		private float power;
		private int radiusNova;
		private int radiusChange;
		private int taskId;
		private int count;
		private int temp;

		private NovaTrackerCircle(List<Player> nearby, Block center, Material mat, LivingEntity caster, int radius, int tickInterval, int activeRadiusChange, float power) {
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.caster = caster;
			this.power = power;
			this.radiusNova = radius;
			this.blocks = new HashSet<>();
			this.radiusChange = activeRadiusChange;
			this.taskId = MagicJutsus.scheduleRepeatingTask(this, 0, tickInterval);
			
			this.count = 0;
			this.temp = 0;
		}
		
		@Override
		public void run() {
			temp = count;
			temp += startRadius;
			temp *= radiusChange;
			count++;
			
			// Remove old blocks
			if (removePreviousBlocks) {
				for (Block b : blocks) {
					for (Player p : nearby) p.sendBlockChange(b.getLocation(), b.getBlockData());
					if (jutsuOnWaveRemove != null) jutsuOnWaveRemove.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
				}
				blocks.clear();
			}
			
			if (temp > radiusNova + 1) {
				stop();
				return;
			} else if (temp > radiusNova) {
				return;
			}
			
			// Generate the bottom block
			Location centerLocation = center.getLocation().clone();
			centerLocation.add(0.5, count * heightPerTick, 0.5);
			Block b;
			
			if (startRadius == 0 && temp == 0) {
				b = centerLocation.getWorld().getBlockAt(centerLocation);
				
				if (BlockUtils.isAir(b.getType()) || b.getType() == Material.TALL_GRASS) {
					Block under = b.getRelative(BlockFace.DOWN);
					if (BlockUtils.isAir(under.getType()) || under.getType() == Material.TALL_GRASS) b = under;
				} else if (BlockUtils.isAir(b.getRelative(BlockFace.UP).getType()) || b.getRelative(BlockFace.UP).getType() == Material.TALL_GRASS) {
					b = b.getRelative(BlockFace.UP);
				}
				
				if (!BlockUtils.isAir(b.getType()) && b.getType() != Material.TALL_GRASS) return;
				
				if (blocks.contains(b)) return;
				for (Player p : nearby) p.sendBlockChange(b.getLocation(), matNova.createBlockData());
				blocks.add(b);
				if (locationJutsu != null) locationJutsu.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
			}
			
			// Generate the circle
			Vector v;
			double angle, x, z;
			double amount = temp * 64;
			double inc = (2 * Math.PI) / amount;
			for (int i = 0; i < amount; i++) {
				angle = i * inc;
				x = temp * Math.cos(angle);
				z = temp * Math.sin(angle);
				v = new Vector(x, 0, z);
				b = center.getWorld().getBlockAt(centerLocation.add(v));
				centerLocation.subtract(v);
				
				if (BlockUtils.isAir(b.getType()) || b.getType() == Material.TALL_GRASS) {
					Block under = b.getRelative(BlockFace.DOWN);
					if (BlockUtils.isAir(under.getType()) || under.getType() == Material.TALL_GRASS) b = under;
				} else if (BlockUtils.isAir(b.getRelative(BlockFace.UP).getType()) || b.getRelative(BlockFace.UP).getType() == Material.TALL_GRASS) {
					b = b.getRelative(BlockFace.UP);
				}
				
				if (!BlockUtils.isAir(b.getType()) && b.getType() != Material.TALL_GRASS) continue;
				
				if (blocks.contains(b)) continue;
				for (Player p : nearby) p.sendBlockChange(b.getLocation(), matNova.createBlockData());
				blocks.add(b);
				if (locationJutsu != null) locationJutsu.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
			}
			
		}

		private void stop() {
			for (Block b : blocks) {
				for (Player p : nearby) p.sendBlockChange(b.getLocation(), b.getBlockData());
				if (jutsuOnEnd != null) jutsuOnEnd.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
			}
			blocks.clear();
			MagicJutsus.cancelTask(taskId);
		}
		
	}
	
}
