package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;

import com.nisovin.magicspells.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.util.MagicConfig;

/**
 * LilywalkSpell<br>
 * <table border=1>
 *     <tr>
 *         <th>
 *             Config Field
 *         </th>
 *         <th>
 *             Data Type
 *         </th>
 *         <th>
 *             Description
 *         </th>
 *         <ht>
 *             Default
 *         </ht>
 *     </tr>
 *     <tr>
 *         <td>
 *             cancel-on-teleport
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             true
 *         </td>
 *     </tr>
 * </table>
 */
public class LilywalkSpell extends BuffSpell {
	
	private boolean cancelOnTeleport;
	
	HashMap<String,Lilies> lilywalkers;
	private Listener listener;

	public LilywalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		this.cancelOnTeleport = getConfigBoolean("cancel-on-teleport", true);
		
		this.lilywalkers = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();
		if (this.cancelOnTeleport) registerEvents(new TeleportListener());
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		Lilies lilies = new Lilies();
		lilies.move(player.getLocation().getBlock());
		this.lilywalkers.put(player.getName(), lilies);
		registerListener();
		return true;
	}
	
	private void registerListener() {
		if (this.listener != null) return;
		this.listener = new LilyListener();
		registerEvents(this.listener);
	}
	
	private void unregisterListener() {
		if (this.listener == null) return;
		if (!this.lilywalkers.isEmpty()) return;
		
		unregisterEvents(this.listener);
		this.listener = null;
	}

	public class LilyListener implements Listener {
	
		@EventHandler(priority=EventPriority.MONITOR)
		public void onPlayerMove(PlayerMoveEvent event) {
			Player player = event.getPlayer();
			Lilies lilies = lilywalkers.get(player.getName());
			if (lilies == null) return;
			
			if (isExpired(player)) {
				turnOff(player);
				return;
			}
			Block block = event.getTo().getBlock();
			boolean moved = lilies.isMoved(block);
			if (moved) {
				lilies.move(block);
				addUse(player);
				chargeUseCost(player);
			}
		}
	
		@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
		public void onBlockBreak(BlockBreakEvent event) {
			if (lilywalkers.isEmpty()) return;
			final Block block = event.getBlock();
			if (block.getType() != Material.LILY_PAD) return;
			if (Util.containsValueParallel(lilywalkers, lilies -> lilies.contains(block))) event.setCancelled(true);
		}
		
	}
	
	public class Lilies {
		
		private Block center = null;
		private HashSet<Block> blocks = new HashSet<>();
		
		public void move(Block center) {
			this.center = center;
			
			Iterator<Block> iter = this.blocks.iterator();
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
			if (block.getType() != Material.AIR) return;
			
			BlockState state = block.getRelative(BlockFace.DOWN).getState();
			if ((state.getType() == Material.WATER) && BlockUtils.getWaterLevel(state) == 0) {
				block.setType(Material.LILY_PAD);
				this.blocks.add(block);
			}
		}
		
		public boolean isMoved(Block center) {
			return !Objects.equals(this.center, center);
		}
		
		public boolean contains(Block block) {
			return this.blocks.contains(block);
		}
		
		public void remove() {
			Util.forEachOrdered(this.blocks, block -> block.setType(Material.AIR));
			this.blocks.clear();
		}
		
	}

	public class TeleportListener implements Listener {
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerTeleport(PlayerTeleportEvent event) {
			Player player = event.getPlayer();
			if (!lilywalkers.containsKey(player.getName())) return;
			
			Location locationFrom = event.getFrom();
			Location locationTo = event.getTo();
			
			if (LocationUtil.differentWorldDistanceGreaterThan(locationFrom, locationTo, 50)) {
				turnOff(player);
			}
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerPortal(PlayerPortalEvent event) {
			Player player = event.getPlayer();
			if (!lilywalkers.containsKey(player.getName())) return;
			turnOff(player);
		}
		
	}
	
	@Override
	public void turnOffBuff(Player player) {
		Lilies lilies = this.lilywalkers.remove(player.getName());
		if (lilies == null) return;
		lilies.remove();
		unregisterListener();
	}
	
	@Override
	protected void turnOff() {
		Util.forEachValueOrdered(this.lilywalkers, Lilies::remove);
		this.lilywalkers.clear();
		unregisterListener();
	}

	@Override
	public boolean isActive(Player player) {
		return this.lilywalkers.containsKey(player.getName());
	}

}
