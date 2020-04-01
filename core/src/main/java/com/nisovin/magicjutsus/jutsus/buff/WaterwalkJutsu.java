package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;

public class WaterwalkJutsu extends BuffJutsu {

	private Set<UUID> waterwalking;

	private float speed;
	
	private Ticker ticker;
	
	public WaterwalkJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		speed = getConfigFloat("speed", 0.05F);
		
		waterwalking = new HashSet<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		if (!(entity instanceof Player)) return true;
		waterwalking.add(entity.getUniqueId());
		startTicker();
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return waterwalking.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		waterwalking.remove(entity.getUniqueId());
		((Player) entity).setFlying(false);
		if (((Player) entity).getGameMode() != GameMode.CREATIVE) ((Player) entity).setAllowFlight(false);

		if (waterwalking.isEmpty()) stopTicker();
	}
	
	@Override
	protected void turnOff() {
		for (UUID id : waterwalking) {
			Player pl = Bukkit.getPlayer(id);
			if (pl == null) continue;
			if (!pl.isValid()) continue;

			pl.setFlying(false);
			if (pl.getGameMode() != GameMode.CREATIVE) pl.setAllowFlight(false);
		}

		waterwalking.clear();
		stopTicker();
	}
	
	private void startTicker() {
		if (ticker != null) return;
		ticker = new Ticker();
	}
	
	private void stopTicker() {
		if (ticker == null) return;
		ticker.stop();
		ticker = null;
	}
	
	private class Ticker implements Runnable {
		
		private int taskId;
		
		private int count = 0;
		
		private Ticker() {
			taskId = MagicJutsus.scheduleRepeatingTask(this, 5, 5);
		}
		
		@Override
		public void run() {
			count++;
			if (count >= 4) count = 0;

			Block feet;
			Block underfeet;
			Location loc;

			for (UUID id : waterwalking) {
				Player pl = Bukkit.getPlayer(id);
				if (pl == null) continue;
				if (!pl.isValid()) continue;
				if (!pl.isOnline()) continue;

				loc = pl.getLocation();
				feet = loc.getBlock();
				underfeet = feet.getRelative(BlockFace.DOWN);

				if (feet.getType() == Material.WATER) {
					loc.setY(Math.floor(loc.getY() + 1) + 0.1);
					pl.teleport(loc);
				} else if (pl.isFlying() && BlockUtils.isAir(underfeet.getType())) {
					loc.setY(Math.floor(loc.getY() - 1) + 0.1);
					pl.teleport(loc);
				}

				feet = pl.getLocation().getBlock();
				underfeet = feet.getRelative(BlockFace.DOWN);

				if (BlockUtils.isAir(feet.getType()) && underfeet.getType() == Material.WATER) {
					if (!pl.isFlying()) {
						pl.setAllowFlight(true);
						pl.setFlying(true);
						pl.setFlySpeed(speed);
					}
					if (count == 0) addUseAndChargeCost(pl);
				} else if (pl.isFlying()) {
					pl.setFlying(false);
					if (pl.getGameMode() != GameMode.CREATIVE) pl.setAllowFlight(false);
					pl.setFlySpeed(0.1F);
				}
			}
		}
		
		public void stop() {
			MagicJutsus.cancelTask(taskId);
		}
		
	}

}
