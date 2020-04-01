package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;

public class WindwalkJutsu extends BuffJutsu {

	private Set<UUID> flyers;

	private int maxY;
	private int maxAltitude;
	private float flySpeed;
	private float launchSpeed;
	private boolean cancelOnLand;

	private HeightMonitor heightMonitor;
	
	public WindwalkJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		maxY = getConfigInt("max-y", 260);
		maxAltitude = getConfigInt("max-altitude", 100);
		flySpeed = getConfigFloat("fly-speed", 0.1F);
		launchSpeed = getConfigFloat("launch-speed", 1F);
		cancelOnLand = getConfigBoolean("cancel-on-land", true);
		
		flyers = new HashSet<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (cancelOnLand) registerEvents(new SneakListener());
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		if (!(entity instanceof Player)) return true;

		if (launchSpeed > 0) {
			entity.teleport(entity.getLocation().add(0, 0.25, 0));
			entity.setVelocity(new Vector(0, launchSpeed, 0));
		}

		flyers.add(entity.getUniqueId());
		((Player) entity).setAllowFlight(true);
		((Player) entity).setFlying(true);
		((Player) entity).setFlySpeed(flySpeed);

		if (heightMonitor == null && (maxY > 0 || maxAltitude > 0)) heightMonitor = new HeightMonitor();

		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return flyers.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		flyers.remove(entity.getUniqueId());
		((Player) entity).setFlying(false);
		if (((Player) entity).getGameMode() != GameMode.CREATIVE) ((Player) entity).setAllowFlight(false);
		((Player) entity).setFlySpeed(0.1F);
		entity.setFallDistance(0);

		if (heightMonitor != null && flyers.isEmpty()) {
			heightMonitor.stop();
			heightMonitor = null;
		}
	}

	@Override
	protected void turnOff() {
		for (UUID id : flyers) {
			Player player = Bukkit.getPlayer(id);
			if (player == null) continue;
			if (!player.isValid()) continue;
			turnOff(player);
		}

		flyers.clear();

		if (heightMonitor == null) return;
		heightMonitor.stop();
		heightMonitor = null;
	}

	public class SneakListener implements Listener {
		
		@EventHandler(priority=EventPriority.MONITOR)
		public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
			Player player = event.getPlayer();
			if (!isActive(player)) return;
			if (BlockUtils.isAir(player.getLocation().subtract(0, 1, 0).getBlock().getType())) return;
			turnOff(player);
		}

	}
	
	private class HeightMonitor implements Runnable {

		private int taskId;

		private HeightMonitor() {
			taskId = MagicJutsus.scheduleRepeatingTask( this, TimeUtil.TICKS_PER_SECOND, TimeUtil.TICKS_PER_SECOND);
		}
		
		@Override
		public void run() {
			for (UUID id : flyers) {
				Player pl = Bukkit.getPlayer(id);
				if (pl == null) continue;
				if (!pl.isValid()) continue;
				addUseAndChargeCost(pl);
				if (maxY > 0) {
					int ydiff = pl.getLocation().getBlockY() - maxY;
					if (ydiff > 0) {
						pl.setVelocity(pl.getVelocity().setY(-ydiff * 1.5));
						continue;
					}
				}

				if (maxAltitude > 0) {
					int ydiff = pl.getLocation().getBlockY() - pl.getWorld().getHighestBlockYAt(pl.getLocation()) - maxAltitude;
					if (ydiff > 0) pl.setVelocity(pl.getVelocity().setY(-ydiff * 1.5));
				}
			}
		}
		
		public void stop() {
			MagicJutsus.cancelTask(taskId);
		}
		
	}

}
