package com.nisovin.magicspells.spells.buff;

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

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.config.ConfigData;

public class StonevisionSpell extends BuffSpell {

	private final Map<UUID, TransparentBlockSet> entities;

	private final Set<Material> transparentTypes;

	private ConfigData<Integer> radius;

	private boolean unobfuscate;

	private Material material;

	public StonevisionSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataInt("radius", 4);
		unobfuscate = getConfigBoolean("unobfuscate", false);

		transparentTypes = new HashSet<>();

		String replaceMaterialName = getConfigString("material", "BARRIER");
		material = Util.getMaterial(replaceMaterialName);
		if (material == null || !material.isBlock()) {
			MagicSpells.error("StonevisionSpell '" + internalName + "' has an invalid material '" + replaceMaterialName + "' defined!");
			material = null;
		}

		List<String> types = getConfigStringList("transparent-types", null);
		if (types != null) {
			for (String str : types) {
				Material material = Util.getMaterial(str);
				if (material != null && material.isBlock()) transparentTypes.add(material);
			}
		}

		if (transparentTypes.isEmpty())
			MagicSpells.error("StonevisionSpell '" + internalName + "' does not define any transparent types");

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		if (!(entity instanceof Player player)) return true;

		int radius = this.radius.get(entity, null, power, args);
		radius = Math.min(radius, MagicSpells.getGlobalRadius());

		entities.put(entity.getUniqueId(), new TransparentBlockSet(player, radius, transparentTypes));
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		TransparentBlockSet t = entities.remove(entity.getUniqueId());
		if (t != null) t.removeTransparency();
	}

	@Override
	protected void turnOff() {
		for (TransparentBlockSet tbs : entities.values()) {
			tbs.removeTransparency();
		}

		entities.clear();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player pl = event.getPlayer();
		if (!isActive(pl)) return;
		if (isExpired(pl)) {
			turnOff(pl);
			return;
		}

		boolean moved = entities.get(pl.getUniqueId()).moveTransparency();
		if (!moved) return;
		addUseAndChargeCost(pl);
	}

	public Map<UUID, TransparentBlockSet> getEntities() {
		return entities;
	}

	public Set<Material> getTransparentTypes() {
		return transparentTypes;
	}

	public boolean shouldUnobfuscate() {
		return unobfuscate;
	}

	public void setUnobfuscate(boolean unobfuscate) {
		this.unobfuscate = unobfuscate;
	}

	public Material getMaterial() {
		return material;
	}

	public void setMaterial(Material material) {
		this.material = material;
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
			this.radius = radius;
			this.types = types;

			center = player.getLocation().getBlock();
			blocks = new ArrayList<>();
			if (unobfuscate) chunks = new HashSet<>();

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
							block = center.getWorld().getBlockAt(x, y, z);
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
							} else if (!BlockUtils.isAir(block.getType())) {
								player.sendBlockChange(block.getLocation(), block.getType().createBlockData());
							}

							// Save chunk for resending after spell ends
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
			if (player.isDead()) player = PlayerNameUtils.getPlayer(player.getName());

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
