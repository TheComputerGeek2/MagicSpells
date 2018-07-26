package com.nisovin.magicspells.spells.buff;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.material.MaterialData;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.materials.MagicBlockMaterial;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.Util;

public class StonevisionSpell extends BuffSpell {
	
	private int range;
	private Set<Material> transparentTypes;
	boolean unobfuscate;
	
	MagicMaterial glass = new MagicBlockMaterial(Material.GLASS);
	
	private HashMap<String, TransparentBlockSet> seers;

	public StonevisionSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		this.range = getConfigInt("range", 4);
		this.unobfuscate = getConfigBoolean("unobfuscate", false);
		
		this.transparentTypes = EnumSet.noneOf(Material.class);
		MagicMaterial type = MagicSpells.getItemNameResolver().resolveBlock(getConfigString("transparent-type", "stone"));
		if (type != null) {
			this.transparentTypes.add(type.getMaterial());
		}
		List<String> types = getConfigStringList("transparent-types", null);
		if (types != null) {
			for (String typeString : types) {
				type = MagicSpells.getItemNameResolver().resolveBlock(typeString);
				if (type != null) this.transparentTypes.add(type.getMaterial());
			}
		}
		if (this.transparentTypes.isEmpty()) {
			MagicSpells.error("Spell '" + this.internalName + "' does not define any transparent types");
		}
		
		String s = getConfigString("glass", "");
		if (!s.isEmpty()) this.glass = MagicSpells.getItemNameResolver().resolveBlock(s);
		if (this.glass == null) this.glass = new MagicBlockMaterial(Material.GLASS);
		
		this.seers = new HashMap<>();
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		this.seers.put(player.getName(), new TransparentBlockSet(player, this.range, this.transparentTypes));
		return true;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player p = event.getPlayer();
		String playerName = p.getName();
		if (!this.seers.containsKey(playerName)) return;
		if (isExpired(p)) {
			turnOff(p);
		} else {
			boolean moved = this.seers.get(playerName).moveTransparency();
			if (moved) {
				addUse(p);
				chargeUseCost(p);
			}
		}
	}
	
	@Override
	public void turnOffBuff(Player player) {
		TransparentBlockSet t = this.seers.remove(player.getName());
		if (t != null) t.removeTransparency();
	}

	@Override
	protected void turnOff() {
		for (TransparentBlockSet tbs : this.seers.values()) {
			tbs.removeTransparency();
		}
		this.seers.clear();
	}
	
	private class TransparentBlockSet {
		
		Player player;
		Block center;
		int range;
		Set<Material> types;
		List<Block> blocks;
		Set<Chunk> chunks;
		
		public TransparentBlockSet(Player player, int range, Set<Material> types) {
			this.player = player;
			this.center = player.getLocation().getBlock();
			this.range = range;
			this.types = types;
			
			this.blocks = new ArrayList<>();
			if (unobfuscate) this.chunks = new HashSet<>();
			
			setTransparency();
		}
		
		public void setTransparency() {
			List<Block> newBlocks = new ArrayList<>();
			
			// Get blocks to set to transparent
			int px = this.center.getX();
			int py = this.center.getY();
			int pz = this.center.getZ();
			Block block;
			if (!unobfuscate) {
				// Handle normally
				for (int x = px - this.range; x <= px + this.range; x++) {
					for (int y = py - this.range; y <= py + this.range; y++) {
						for (int z = pz - this.range; z <= pz + this.range; z++) {
							block = this.center.getWorld().getBlockAt(x,y,z);
							if (this.types.contains(block.getType())) {
								Util.sendFakeBlockChange(this.player, block, glass);
								newBlocks.add(block);
							}
						}
					}
				}
			} else {
				// Unobfuscate everything
				int dx;
				int dy;
				int dz;
				for (int x = px - this.range - 1; x <= px + this.range + 1; x++) {
					for (int y = py - this.range - 1; y <= py + this.range + 1; y++) {
						for (int z = pz - this.range - 1; z <= pz + this.range + 1; z++) {
							dx = Math.abs(x - px);
							dy = Math.abs(y - py);
							dz = Math.abs(z - pz);
							block = this.center.getWorld().getBlockAt(x, y, z);
							if (this.types.contains(block.getType()) && dx <= this.range && dy <= this.range && dz <= this.range) {
								Util.sendFakeBlockChange(this.player, block, glass);
								newBlocks.add(block);
							} else if (block.getType() != Material.AIR) {
								Util.restoreFakeBlockChange(this.player, block);
							}
							
							// Save chunk for resending after spell ends
							Chunk c = block.getChunk();
							this.chunks.add(c);
						}
					}
				}
			}
			
			// Remove old transparent blocks
			this.blocks.stream().filter(b -> !newBlocks.contains(b)).forEachOrdered(b -> Util.restoreFakeBlockChange(this.player, b));
			
			// Update block set
			this.blocks = newBlocks;
		}
		
		public boolean moveTransparency() {
			if (this.player.isDead()) {
				this.player = PlayerNameUtils.getPlayer(this.player.getName());
			}
			Location loc = this.player.getLocation();
			if (!this.center.getWorld().equals(loc.getWorld()) || this.center.getX() != loc.getBlockX() || this.center.getY() != loc.getBlockY() || this.center.getZ() != loc.getBlockZ()) {
				// Moved
				this.center = loc.getBlock();
				setTransparency();
				return true;
			}
			return false;
		}
		
		public void removeTransparency() {
			Util.forEachOrdered(this.blocks, b -> Util.restoreFakeBlockChange(player, b));
			this.blocks = null;
		}
		
	}

	@Override
	public boolean isActive(Player player) {
		return this.seers.containsKey(player.getName());
	}

}
