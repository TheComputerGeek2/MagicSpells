package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.BlockPlatform;

public class FrostwalkJutsu extends BuffJutsu {

	private Map<UUID, BlockPlatform> frostwalkers;

	private int size;
	private boolean leaveFrozen;

	public FrostwalkJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		size = getConfigInt("size", 2);
		leaveFrozen = getConfigBoolean("leave-frozen", false);
		
		frostwalkers = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		frostwalkers.put(entity.getUniqueId(), new BlockPlatform(Material.ICE, Material.WATER, entity.getLocation().getBlock().getRelative(0, -1, 0), size, !leaveFrozen, "square"));
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return frostwalkers.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		BlockPlatform platform = frostwalkers.get(entity.getUniqueId());
		if (platform == null) return;

		platform.destroyPlatform();
		frostwalkers.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		Util.forEachValueOrdered(frostwalkers, BlockPlatform::destroyPlatform);
		frostwalkers.clear();
	}


	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (!isActive(player)) return;
		if (isExpired(player)) {
			turnOff(player);
			return;
		}
		
		Block block;
		boolean teleportUp = false;
		Location locationTo = event.getTo();
		Location locationFrom = event.getFrom();
		double locationToY = locationTo.getY();
		double locationFromY = locationFrom.getY();
		Block locationToBlock = locationTo.getBlock();

		if (locationToY > locationFromY && locationToY % 1 > .62 && locationToBlock.getType() == Material.WATER && BlockUtils.isAir(locationToBlock.getRelative(0, 1, 0).getType())) {
			block = locationToBlock;
			teleportUp = true;
		} else {
			block = locationToBlock.getRelative(0, -1, 0);
		}
		boolean moved = frostwalkers.get(player.getUniqueId()).movePlatform(block);
		if (!moved) return;

		addUseAndChargeCost(player);

		if (teleportUp) {
			Location loc = player.getLocation().clone();
			loc.setY(locationTo.getBlockY() + 1);
			player.teleport(loc);
		}

	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (frostwalkers.isEmpty()) return;
		Block block = event.getBlock();
		if (block.getType() != Material.ICE) return;
		if (Util.containsValueParallel(frostwalkers, platform -> platform.blockInPlatform(block))) event.setCancelled(true);
	}

}
