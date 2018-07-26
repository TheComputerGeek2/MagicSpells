package com.nisovin.magicspells.spells.buff;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.PlayerNameUtils;

/**
 * WaterwalkSpell<br>
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
 *     </tr>
 *     <tr>
 *         <td>
 *             speed
 *         </td>
 *         <td>
 *             Float
 *         </td>
 *         <td>
 *             The speed for moving when walking on water.
 *         </td>
 *     </tr>
 * </table>
 */
public class WaterwalkSpell extends BuffSpell {

	float speed;
	
	HashSet<String> waterwalking;
	private Ticker ticker = null;
	
	public WaterwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		this.speed = getConfigFloat("speed", 0.05F);
		
		this.waterwalking = new HashSet<>();
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		this.waterwalking.add(player.getName());
		startTicker();
		return true;
	}

	@Override
	public boolean isActive(Player player) {
		return this.waterwalking.contains(player.getName());
	}

	@Override
	public void turnOffBuff(Player player) {
		if (this.waterwalking.remove(player.getName())) {
			player.setFlying(false);
			if (player.getGameMode() != GameMode.CREATIVE) player.setAllowFlight(false);
		}
		if (this.waterwalking.isEmpty()) stopTicker();
	}
	
	@Override
	protected void turnOff() {
		for (String playerName : this.waterwalking) {
			Player player = PlayerNameUtils.getPlayerExact(playerName);
			if (player == null) continue;
			if (!player.isValid()) continue;
			
			player.setFlying(false);
			if (player.getGameMode() != GameMode.CREATIVE) player.setAllowFlight(false);
		}
		this.waterwalking.clear();
		stopTicker();
	}
	
	private void startTicker() {
		if (this.ticker != null) return;
		this.ticker = new Ticker();
	}
	
	private void stopTicker() {
		if (this.ticker == null) return;
		this.ticker.stop();
		this.ticker = null;
	}
	
	private class Ticker implements Runnable {
		
		private int taskId = 0;
		
		private int count = 0;
		
		public Ticker() {
			this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 5, 5);
		}
		
		@Override
		public void run() {
			this.count += 1;
			if (this.count >= 4) this.count = 0;
			Location loc;
			Block feet;
			Block underfeet;
			for (String n : waterwalking) {
				Player p = PlayerNameUtils.getPlayerExact(n);
				if (p == null) continue;
				if (!p.isOnline()) continue;
				if (!p.isValid()) continue;
				loc = p.getLocation();
				feet = loc.getBlock();
				underfeet = feet.getRelative(BlockFace.DOWN);
				if (feet.getType() == Material.WATER) {
					loc.setY(Math.floor(loc.getY() + 1) + 0.1);
					p.teleport(loc);
				} else if (p.isFlying() && underfeet.getType() == Material.AIR) {
					loc.setY(Math.floor(loc.getY() - 1) + 0.1);
					p.teleport(loc);
				}
				feet = p.getLocation().getBlock();
				underfeet = feet.getRelative(BlockFace.DOWN);
				if (feet.getType() == Material.AIR && underfeet.getType() == Material.WATER) {
					if (!p.isFlying()) {
						p.setAllowFlight(true);
						p.setFlying(true);
						p.setFlySpeed(speed);
					}
					if (this.count == 0) addUseAndChargeCost(p);
				} else if (p.isFlying()) {
					p.setFlying(false);
					if (p.getGameMode() != GameMode.CREATIVE) p.setAllowFlight(false);
					p.setFlySpeed(0.1F);
				}
			}
		}
		
		public void stop() {
			Bukkit.getScheduler().cancelTask(this.taskId);
		}
		
	}

}
