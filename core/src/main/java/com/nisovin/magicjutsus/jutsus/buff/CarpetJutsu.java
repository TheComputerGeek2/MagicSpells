package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.BlockPlatform;

public class CarpetJutsu extends BuffJutsu {

	private Map<UUID, BlockPlatform> carpets;
	private Set<UUID> falling;

	private Material platformMaterial;
	private String materialName;
	private int platformSize;

	public CarpetJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		materialName = getConfigString("platform-block", "GLASS").toUpperCase();
		platformMaterial = Material.getMaterial(materialName);

		if (platformMaterial == null || !platformMaterial.isBlock()) {
			platformMaterial = null;
			MagicJutsus.error("CarpetJutsu " + internalName + " has an invalid platform-block defined! '" + materialName + "'");
		}

		platformSize = getConfigInt("size", 2);

		carpets = new HashMap<>();
		falling = new HashSet<>();
	}
	
	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		carpets.put(entity.getUniqueId(), new BlockPlatform(platformMaterial, Material.AIR, entity.getLocation().getBlock().getRelative(0, -1, 0), platformSize, true, "square"));
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return carpets.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		BlockPlatform platform = carpets.get(entity.getUniqueId());
		if (platform == null) return;

		platform.destroyPlatform();
		carpets.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		Util.forEachValueOrdered(carpets, BlockPlatform::destroyPlatform);
		carpets.clear();

	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		BlockPlatform platform = carpets.get(player.getUniqueId());
		if (platform == null) return;

		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		Block block = event.getTo().getBlock().getRelative(BlockFace.DOWN);
		if (falling.contains(player.getUniqueId())) block = event.getTo().getBlock().getRelative(BlockFace.DOWN, 2);

		boolean moved = platform.isMoved(block, true);
		if (moved) {
			platform.movePlatform(block, true);
			addUseAndChargeCost(player);
		}
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		if (!isActive(player)) return;

		if (player.isSneaking()) {
			falling.remove(player.getUniqueId());
			return;
		}

		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN, 2);
		boolean moved = carpets.get(player.getUniqueId()).movePlatform(block);

		if (moved) {
			falling.add(player.getUniqueId());
			addUseAndChargeCost(player);
		}
	}
	
	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (carpets.isEmpty()) return;
		final Block block = event.getBlock();
		if (block.getType() != platformMaterial) return;
		if (Util.containsValueParallel(carpets, platform -> platform.blockInPlatform(block))) event.setCancelled(true);
	}

}
