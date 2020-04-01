package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.PlayerNameUtils;

public class StonevisionJutsu extends BuffJutsu {

	private Map<UUID, TransparentBlockSet> seers;
	private Set<Material> transparentTypes;

	private int radius;
	private boolean unobfuscate;

	private Material material;

	public StonevisionJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		radius = getConfigInt("radius", 4);
		unobfuscate = getConfigBoolean("unobfuscate", false);
		
		transparentTypes = new HashSet<>();

		String replaceMaterialName = getConfigString("material", "BARRIER").toUpperCase();
		material = Material.getMaterial(replaceMaterialName);
		if (material == null || !material.isBlock()) {
			MagicJutsus.error("StonevisionJutsu '" + internalName + "' has an invalid material defined!");
			material = null;
		}

		List<String> types = getConfigStringList("transparent-types", null);
		if (types != null) {
			for (String str : types) {
				Material material = Material.getMaterial(str.toUpperCase());
				if (material != null && material.isBlock()) transparentTypes.add(material);
			}
		}

		if (transparentTypes.isEmpty()) MagicJutsus.error("StonevisionJutsu '" + internalName + "' does not define any transparent types");

		seers = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		if (!(entity instanceof Player)) return true;
		seers.put(entity.getUniqueId(), new TransparentBlockSet((Player) entity, radius, transparentTypes));
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return seers.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		TransparentBlockSet t = seers.remove(entity.getUniqueId());
		if (t != null) t.removeTransparency();
	}

	@Override
	protected void turnOff() {
		for (TransparentBlockSet tbs : seers.values()) {
			tbs.removeTransparency();
		}

		seers.clear();
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player pl = event.getPlayer();
		if (!isActive(pl)) return;
		if (isExpired(pl)) {
			turnOff(pl);
			return;
		}

		boolean moved = seers.get(pl.getUniqueId()).moveTransparency();
		if (!moved) return;
		addUseAndChargeCost(pl);
	}

	private class TransparentBlockSet {

		private Player player;
		private Block center;
		private int radius;
		private Set<Material> types;
		private List<Block> blocks;
		private Set<Chunk> chunks;

		private TransparentBlockSet(Player player, int radius, Set<Material> types) {
			this.player = player;
			this.center = player.getLocation().getBlock();
			this.radius = radius;
			this.types = types;

			this.blocks = new ArrayList<>();
			if (unobfuscate) this.chunks = new HashSet<>();

			setTransparency();
		}

		private void setTransparency() {
			List<Block> newBlocks = new ArrayList<>();

			// Get blocks to set to transparent
			int px = center.getX();
			int py = center.getY();
			int pz = center.getZ();
			Block block;
			if (!unobfuscate) {
				// Handle normally
				for (int x = px - radius; x <= px + radius; x++) {
					for (int y = py - radius; y <= py + radius; y++) {
						for (int z = pz - radius; z <= pz + radius; z++) {
							block = center.getWorld().getBlockAt(x,y,z);
							if (types.contains(block.getType())) {
								player.sendBlockChange(block.getLocation(), material.createBlockData());
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
				for (int x = px - radius - 1; x <= px + radius + 1; x++) {
					for (int y = py - radius - 1; y <= py + radius + 1; y++) {
						for (int z = pz - radius - 1; z <= pz + radius + 1; z++) {
							dx = Math.abs(x - px);
							dy = Math.abs(y - py);
							dz = Math.abs(z - pz);
							block = center.getWorld().getBlockAt(x, y, z);
							if (types.contains(block.getType()) && dx <= radius && dy <= radius && dz <= radius) {
								player.sendBlockChange(block.getLocation(), material.createBlockData());
								newBlocks.add(block);
							} else if (block.getType() != Material.AIR) {
								player.sendBlockChange(block.getLocation(), block.getType().createBlockData());
							}

							// Save chunk for resending after jutsu ends
							Chunk c = block.getChunk();
							chunks.add(c);
						}
					}
				}
			}

			// Remove old transparent blocks
			blocks.stream().filter(b -> !newBlocks.contains(b)).forEachOrdered(b -> player.sendBlockChange(b.getLocation(), b.getType().createBlockData()));
			// Update block set
			blocks = newBlocks;
		}

		private boolean moveTransparency() {
			if (player.isDead()) {
				player = PlayerNameUtils.getPlayer(player.getName());
			}
			Location loc = this.player.getLocation();
			if (!center.getWorld().equals(loc.getWorld()) || center.getX() != loc.getBlockX() || center.getY() != loc.getBlockY() || center.getZ() != loc.getBlockZ()) {
				// Moved
				center = loc.getBlock();
				setTransparency();
				return true;
			}
			return false;
		}

		private void removeTransparency() {
			Util.forEachOrdered(blocks, b -> player.sendBlockChange(b.getLocation(), b.getType().createBlockData()));
			blocks = null;
		}

	}

}
