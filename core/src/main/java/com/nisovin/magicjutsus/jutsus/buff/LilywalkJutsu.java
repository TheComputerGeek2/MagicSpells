package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;

public class LilywalkJutsu extends BuffJutsu {

	private Map<UUID, Lilies> lilywalkers;

	public LilywalkJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		lilywalkers = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		Lilies lilies = new Lilies();
		lilies.move(entity.getLocation().getBlock());

		lilywalkers.put(entity.getUniqueId(), lilies);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return lilywalkers.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		Lilies lilies = lilywalkers.remove(entity.getUniqueId());
		if (lilies == null) return;
		lilies.remove();
	}

	@Override
	protected void turnOff() {
		Util.forEachValueOrdered(lilywalkers, Lilies::remove);
		lilywalkers.clear();
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Lilies lilies = lilywalkers.get(player.getUniqueId());

		if (lilies == null) return;
		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		Block block = event.getTo().getBlock();
		boolean moved = lilies.isMoved(block);
		if (!moved) return;

		lilies.move(block);
		addUse(player);
		chargeUseCost(player);
	}
	
	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (lilywalkers.isEmpty()) return;
		final Block block = event.getBlock();
		if (block.getType() != Material.LILY_PAD) return;
		if (Util.containsValueParallel(lilywalkers, lilies -> lilies.contains(block))) event.setCancelled(true);
	}

	private static class Lilies {
		
		private Block center = null;
		private Set<Block> blocks = new HashSet<>();
		
		private void move(Block center) {
			this.center = center;
			
			Iterator<Block> iter = blocks.iterator();
			while (iter.hasNext()) {
				Block b = iter.next();
				if (b.equals(center)) continue;
				b.setType(Material.AIR);
				iter.remove();
			}
			
			setToLily(center);
			setToLily(center.getRelative(BlockFace.NORTH));
			setToLily(center.getRelative(BlockFace.SOUTH));
			setToLily(center.getRelative(BlockFace.EAST));
			setToLily(center.getRelative(BlockFace.WEST));
			setToLily(center.getRelative(BlockFace.NORTH_WEST));
			setToLily(center.getRelative(BlockFace.NORTH_EAST));
			setToLily(center.getRelative(BlockFace.SOUTH_WEST));
			setToLily(center.getRelative(BlockFace.SOUTH_EAST));
		}
		
		private void setToLily(Block block) {
			if (!BlockUtils.isAir(block.getType())) return;
			
			BlockState state = block.getRelative(BlockFace.DOWN).getState();
			if ((state.getType() == Material.WATER) && BlockUtils.getWaterLevel(state) == 0) {
				block.setType(Material.LILY_PAD);
				blocks.add(block);
			}
		}
		
		private boolean isMoved(Block center) {
			return !Objects.equals(this.center, center);
		}
		
		private boolean contains(Block block) {
			return blocks.contains(block);
		}
		
		private void remove() {
			Util.forEachOrdered(blocks, block -> block.setType(Material.AIR));
			blocks.clear();
		}
		
	}

}
