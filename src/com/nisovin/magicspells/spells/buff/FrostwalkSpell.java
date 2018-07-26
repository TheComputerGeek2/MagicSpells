package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;

import com.nisovin.magicspells.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.BlockPlatform;
import com.nisovin.magicspells.util.ConfigData;
import com.nisovin.magicspells.util.MagicConfig;

public class FrostwalkSpell extends BuffSpell {
	
	@ConfigData(field="size", dataType="int", defaultValue="2")
	private int size;
	
	@ConfigData(field="leave-frozen", dataType="boolean", defaultValue="false")
	private boolean leaveFrozen;
	
	private HashMap<String,BlockPlatform> frostwalkers;

	public FrostwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		size = getConfigInt("size", 2);
		leaveFrozen = getConfigBoolean("leave-frozen", false);
		
		frostwalkers = new HashMap<>();
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		frostwalkers.put(player.getName(), new BlockPlatform(Material.ICE, Material.WATER, player.getLocation().getBlock().getRelative(0, -1, 0), size, !leaveFrozen, "square"));
		return true;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		
		if (!frostwalkers.containsKey(playerName)) return;
		
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
		
		if (locationToY > locationFromY && locationToY % 1 > .62 && locationToBlock.getType() == Material.WATER && locationToBlock.getRelative(0, 1, 0).getType() == Material.AIR) {
			block = locationToBlock;
			teleportUp = true;
		} else {
			block = locationToBlock.getRelative(0, -1, 0);
		}
		boolean moved = frostwalkers.get(playerName).movePlatform(block);
		if (moved) {
			addUse(player);
			chargeUseCost(player);
			if (teleportUp) {
				Location loc = player.getLocation().clone();
				loc.setY(locationTo.getBlockY() + 1);
				player.teleport(loc);
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		if (frostwalkers.isEmpty()) return;
		
		Block block = event.getBlock();
		if (block.getType() != Material.ICE) return;
		
		if (blockInPlatform(block)) event.setCancelled(true);
	}
	
	@Override
	public void turnOffBuff(Player player) {
		String playerName = player.getName();
		BlockPlatform platform = frostwalkers.get(playerName);
		if (platform == null) return;
		
		platform.destroyPlatform();
		frostwalkers.remove(playerName);
	}
	
	@Override
	protected void turnOff() {
		Util.forEachValueOrdered(frostwalkers, BlockPlatform::destroyPlatform);
		frostwalkers.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return frostwalkers.containsKey(player.getName());
	}
	
	private boolean blockInPlatform(final Block block) {
		return Util.containsValueParallel(frostwalkers, platform -> platform.blockInPlatform(block));
	}

}
