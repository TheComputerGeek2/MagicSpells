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
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class NovaSpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntitySpell {
	
	MagicMaterial material;
	Vector relativeOffset;
	Subspell spellOnEnd;
	Subspell locationSpell;
	Subspell spellOnWaveRemove;
	String spellOnEndName;
	String locationSpellName;
	String spellOnWaveRemoveName;
	
	int radius;
	int startRadius;
	int heightPerTick;
	int novaTickInterval;
	int expandingRadiusChange;
	
	double visibleRange;
	
	boolean pointBlank;
	boolean circleShape;
	boolean removePreviousBlocks;
	
	public NovaSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		material = MagicSpells.getItemNameResolver().resolveBlock(getConfigString("type", "water"));
		relativeOffset = getConfigVector("relative-offset", "0,0,0");
		spellOnEndName = getConfigString("spell-on-end", "");
		locationSpellName = getConfigString("spell", "");
		spellOnWaveRemoveName = getConfigString("spell-on-wave-remove", "");
		
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
		
		if (material == null) {
			MagicSpells.error("NovaSpell " + internalName + " has an invalid block type defined!");
		}
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState spellCastState, float power, String[] strings) {
		if (spellCastState == SpellCastState.NORMAL) {
			
			Location loc;
			if (pointBlank) loc = player.getLocation();
			else loc = getTargetedBlock(player, power).getLocation();
			
			createNova(player, loc, power);
		}
		
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntity(Player player, LivingEntity livingEntity, float v) {
		createNova(player, livingEntity.getLocation(), v);
		return false;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity livingEntity, float v) {
		return false;
	}
	
	@Override
	public boolean castAtLocation(Player player, Location location, float v) {
		createNova(player, location, v);
		return false;
	}
	
	@Override
	public boolean castAtLocation(Location location, float v) {
		return false;
	}
	
	void createNova(Player pl, Location loc, float power) {
		if (material == null) return;
		// Relative offset
		Location startLoc = loc.clone();
		Vector direction = pl.getLocation().getDirection().normalize();
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
		if (!circleShape) new NovaTrackerSquare(nearby, startLoc.getBlock(), material, pl, radius, novaTickInterval, expandingRadiusChange, power);
		else new NovaTrackerCircle(nearby, startLoc.getBlock(), material, pl, radius, novaTickInterval, expandingRadiusChange, power);
	}
	
	private class NovaTrackerSquare implements Runnable {
		
		MagicMaterial matNova;
		List<Player> nearby;
		Set<Block> blocks;
		Player caster;
		Block center;
		float power;
		int radiusNova;
		int radiusChange;
		int taskId;
		int count;
		int temp;
		
		public NovaTrackerSquare(List<Player> nearby, Block center, MagicMaterial mat, Player caster, int radius, int tickInterval, int activeRadiusChange, float power) {
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.caster = caster;
			this.power = power;
			this.radiusNova = radius;
			this.blocks = new HashSet<>();
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
			
			if (removePreviousBlocks) {
				for (Block b : blocks) {
					for (Player p : nearby) Util.restoreFakeBlockChange(p, b);
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
					if (b.getType() == Material.AIR || b.getType() == Material.LONG_GRASS) {
						Block under = b.getRelative(BlockFace.DOWN);
						if (under.getType() == Material.AIR || under.getType() == Material.LONG_GRASS) b = under;
					} else if (b.getRelative(BlockFace.UP).getType() == Material.AIR || b.getRelative(BlockFace.UP).getType() == Material.LONG_GRASS) {
						b = b.getRelative(BlockFace.UP);
					}
					
					if (b.getType() != Material.AIR && b.getType() != Material.LONG_GRASS) continue;
					
					if (blocks.contains(b)) continue;
					for (Player p : nearby) Util.sendFakeBlockChange(p, b, matNova);
					blocks.add(b);
					if (locationSpell != null) locationSpell.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
				}
			}
			
		}
		
		public void stop() {
			for (Block b : blocks) {
				for (Player p : nearby) Util.restoreFakeBlockChange(p, b);
				if (spellOnEnd != null) spellOnEnd.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
			}
			blocks.clear();
			MagicSpells.cancelTask(taskId);
		}
		
	}
	
	private class NovaTrackerCircle implements Runnable {
		
		MagicMaterial matNova;
		List<Player> nearby;
		Set<Block> blocks;
		Player caster;
		Block center;
		float power;
		int radiusNova;
		int radiusChange;
		int taskId;
		int count;
		int temp;
		
		public NovaTrackerCircle(List<Player> nearby, Block center, MagicMaterial mat, Player caster, int radius, int tickInterval, int activeRadiusChange, float power) {
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.caster = caster;
			this.power = power;
			this.radiusNova = radius;
			this.blocks = new HashSet<>();
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
					for (Player p : nearby) Util.restoreFakeBlockChange(p, b);
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
				
				if (b.getType() == Material.AIR || b.getType() == Material.LONG_GRASS) {
					Block under = b.getRelative(BlockFace.DOWN);
					if (under.getType() == Material.AIR || under.getType() == Material.LONG_GRASS) b = under;
				} else if (b.getRelative(BlockFace.UP).getType() == Material.AIR || b.getRelative(BlockFace.UP).getType() == Material.LONG_GRASS) {
					b = b.getRelative(BlockFace.UP);
				}
				
				if (b.getType() != Material.AIR && b.getType() != Material.LONG_GRASS) return;
				
				if (blocks.contains(b)) return;
				for (Player p : nearby) Util.sendFakeBlockChange(p, b, matNova);
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
				
				if (b.getType() == Material.AIR || b.getType() == Material.LONG_GRASS) {
					Block under = b.getRelative(BlockFace.DOWN);
					if (under.getType() == Material.AIR || under.getType() == Material.LONG_GRASS) b = under;
				} else if (b.getRelative(BlockFace.UP).getType() == Material.AIR || b.getRelative(BlockFace.UP).getType() == Material.LONG_GRASS) {
					b = b.getRelative(BlockFace.UP);
				}
				
				if (b.getType() != Material.AIR && b.getType() != Material.LONG_GRASS) continue;
				
				if (blocks.contains(b)) continue;
				for (Player p : nearby) Util.sendFakeBlockChange(p, b, matNova);
				blocks.add(b);
				if (locationSpell != null) locationSpell.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
			}
			
		}
		
		public void stop() {
			for (Block b : blocks) {
				for (Player p : nearby) Util.restoreFakeBlockChange(p, b);
				if (spellOnEnd != null) spellOnEnd.castAtLocation(caster, b.getLocation().add(0.5, 0, 0.5),  power);
			}
			blocks.clear();
			MagicSpells.cancelTask(taskId);
		}
		
	}
	
}
