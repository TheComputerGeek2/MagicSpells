package com.nisovin.magicspells.spells.targeted;

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

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class NovaSpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntitySpell {

	private Material material;
	private String materialName;

	private Vector relativeOffset;

	private Subspell spellOnEnd;
	private Subspell locationSpell;
	private Subspell spellOnWaveRemove;
	private String spellOnEndName;
	private String locationSpellName;
	private String spellOnWaveRemoveName;

	private ConfigData<Integer> radius;
	private ConfigData<Integer> startRadius;
	private ConfigData<Integer> heightPerTick;
	private ConfigData<Integer> novaTickInterval;
	private ConfigData<Integer> expandingRadiusChange;

	private ConfigData<Double> visibleRange;

	private boolean pointBlank;
	private boolean circleShape;
	private boolean removePreviousBlocks;
	
	public NovaSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		materialName = getConfigString("type", "water");
		material = Util.getMaterial(materialName);
		if (material == null || !material.isBlock()) {
			MagicSpells.error("NovaSpell '" + internalName + "' has an invalid block type defined!");
			material = null;
		}
		
		relativeOffset = getConfigVector("relative-offset", "0,0,0");

		spellOnEndName = getConfigString("spell-on-end", "");
		locationSpellName = getConfigString("spell", "");
		spellOnWaveRemoveName = getConfigString("spell-on-wave-remove", "");
		
		radius = getConfigDataInt("radius", 3);
		startRadius = getConfigDataInt("start-radius", 0);
		heightPerTick = getConfigDataInt("height-per-tick", 0);
		novaTickInterval = getConfigDataInt("expand-interval", 5);
		expandingRadiusChange = getConfigDataInt("expanding-radius-change", 1);

		visibleRange = getConfigDataDouble("visible-range", 20);

		pointBlank = getConfigBoolean("point-blank", true);
		circleShape = getConfigBoolean("circle-shape", false);
		removePreviousBlocks = getConfigBoolean("remove-previous-blocks", true);
		
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		locationSpell = new Subspell(locationSpellName);
		if (!locationSpell.process() || !locationSpell.isTargetedLocationSpell()) {
			if (!locationSpellName.isEmpty()) MagicSpells.error("NovaSpell " + internalName + " has an invalid spell defined!");
			locationSpell = null;
		}
		
		spellOnWaveRemove = new Subspell(spellOnWaveRemoveName);
		if (!spellOnWaveRemove.process() || !spellOnWaveRemove.isTargetedLocationSpell()) {
			if (!spellOnWaveRemoveName.isEmpty()) MagicSpells.error("NovaSpell " + internalName + " has an invalid spell-on-wave-remove defined!");
			spellOnWaveRemove = null;
		}
		
		spellOnEnd = new Subspell(spellOnEndName);
		if (!spellOnEnd.process() || !spellOnEnd.isTargetedLocationSpell()) {
			if (!spellOnEndName.isEmpty()) MagicSpells.error("NovaSpell " + internalName + " has an invalid spell-on-end defined!");
			spellOnEnd = null;
		}
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState spellCastState, float power, String[] strings) {
		if (spellCastState == SpellCastState.NORMAL) {
			Location loc;
			if (pointBlank) loc = caster.getLocation();
			else loc = getTargetedBlock(caster, power).getLocation();
			
			createNova(caster, loc, power, strings);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity livingEntity, float v, String[] args) {
		createNova(caster, livingEntity.getLocation(), v, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		createNova(caster, target.getLocation(), power, null);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity livingEntity, float v) {
		return false;
	}
	
	@Override
	public boolean castAtLocation(LivingEntity livingEntity, Location location, float v, String[] args) {
		createNova(livingEntity, location, v, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		createNova(caster, target, power, null);
		return true;
	}

	@Override
	public boolean castAtLocation(Location location, float v) {
		return false;
	}

	private void createNova(LivingEntity caster, Location loc, float power, String[] args) {
		if (material == null) return;
		// Relative offset
		Location startLoc = loc.clone();
		Vector direction = caster.getLocation().getDirection().normalize();
		Vector horizOffset = new Vector(-direction.getZ(), 0.0, direction.getX()).normalize();
		startLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
		startLoc.add(direction.setY(0).normalize().multiply(relativeOffset.getX()));
		startLoc.add(0, relativeOffset.getY(), 0);
		
		// Get nearby players
		double visibleRange = Math.min(Math.max(this.visibleRange.get(caster, null, power, args), 20), MagicSpells.getGlobalRadius());

		Collection<Entity> nearbyEntities = startLoc.getWorld().getNearbyEntities(startLoc, visibleRange, visibleRange, visibleRange);
		List<Player> nearby = new ArrayList<>();
		for (Entity e : nearbyEntities) {
			if (!(e instanceof Player)) continue;
			nearby.add((Player) e);
		}

		int radius = this.radius.get(caster, null, power, args);
		int startRadius = this.startRadius.get(caster, null, power, args);
		int heightPerTick = this.heightPerTick.get(caster, null, power, args);
		int novaTickInterval = this.novaTickInterval.get(caster, null, power, args);
		int expandingRadiusChange = this.expandingRadiusChange.get(caster, null, power, args);
		if (expandingRadiusChange < 1) expandingRadiusChange = 1;

		// Start tracker
		if (!circleShape) new NovaTrackerSquare(nearby, startLoc.getBlock(), material, caster, radius, startRadius, heightPerTick, novaTickInterval, expandingRadiusChange, power);
		else new NovaTrackerCircle(nearby, startLoc.getBlock(), material, caster, radius, startRadius, heightPerTick, novaTickInterval, expandingRadiusChange, power);
	}
	
	private class NovaTrackerSquare implements Runnable {
		
		private Material matNova;
		private List<Player> nearby;
		private Set<Block> blocks;
		private LivingEntity caster;
		private Block center;
		private float power;
		private int radiusNova;
		private int startRadius;
		private int heightPerTick;
		private int radiusChange;
		private int taskId;
		private int count;
		private int temp;

		private NovaTrackerSquare(List<Player> nearby, Block center, Material mat, LivingEntity caster, int radius, int startRadius, int heightPerTick, int tickInterval, int activeRadiusChange, float power) {
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.caster = caster;
			this.power = power;
			this.radiusNova = radius;
			this.blocks = new HashSet<>();
			this.radiusChange = activeRadiusChange;
			this.startRadius = startRadius;
			this.heightPerTick = heightPerTick;
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
			
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
					if (spellOnWaveRemove != null) spellOnWaveRemove.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
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
					if (locationSpell != null) locationSpell.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
				}
			}
			
		}

		private void stop() {
			for (Block b : blocks) {
				for (Player p : nearby) p.sendBlockChange(b.getLocation(), b.getBlockData());
				if (spellOnEnd != null) spellOnEnd.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
			}
			blocks.clear();
			MagicSpells.cancelTask(taskId);
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
		private int startRadius;
		private int heightPerTick;
		private int radiusChange;
		private int taskId;
		private int count;
		private int temp;

		private NovaTrackerCircle(List<Player> nearby, Block center, Material mat, LivingEntity caster, int radius, int startRadius, int heightPerTick, int tickInterval, int activeRadiusChange, float power) {
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.caster = caster;
			this.power = power;
			this.radiusNova = radius;
			this.blocks = new HashSet<>();
			this.startRadius = startRadius;
			this.heightPerTick = heightPerTick;
			this.radiusChange = activeRadiusChange;
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
			
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
					if (spellOnWaveRemove != null) spellOnWaveRemove.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
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
				if (locationSpell != null) locationSpell.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
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
				if (locationSpell != null) locationSpell.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
			}
			
		}

		private void stop() {
			for (Block b : blocks) {
				for (Player p : nearby) p.sendBlockChange(b.getLocation(), b.getBlockData());
				if (spellOnEnd != null) spellOnEnd.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
			}
			blocks.clear();
			MagicSpells.cancelTask(taskId);
		}
		
	}
	
}
