package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.FluidCollisionMode;
import org.bukkit.event.EventPriority;
import org.bukkit.util.RayTraceResult;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.config.ConfigData;

public class WindwalkSpell extends BuffSpell {

	private final Map<UUID, FlyData> players;

	private boolean cancelOnLand;
	private final boolean alwaysFly;
	private final ConfigData<Boolean> enableMaxY;
	private final ConfigData<Boolean> constantMaxY;
	private final ConfigData<Boolean> constantFlySpeed;
	private final ConfigData<Boolean> constantMaxAltitude;

	private final ConfigData<Double> maxY;
	private final ConfigData<Double> maxAltitude;

	private final ConfigData<Float> flySpeed;
	private final ConfigData<Float> launchSpeed;

	private HeightMonitor heightMonitor;

	public WindwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		alwaysFly = getConfigBoolean("always-fly", false);
		enableMaxY = getConfigDataBoolean("enable-max-y", true);
		cancelOnLand = getConfigBoolean("cancel-on-land", true);
		constantMaxY = getConfigDataBoolean("constant-max-y", true);
		constantFlySpeed = getConfigDataBoolean("constant-fly-speed", true);
		constantMaxAltitude = getConfigDataBoolean("constant-max-altitude", true);

		flySpeed = getConfigDataFloat("fly-speed", 0.1F);
		launchSpeed = getConfigDataFloat("launch-speed", 1F);

		maxY = getConfigDataDouble("max-y", 260);
		maxAltitude = getConfigDataDouble("max-altitude", 100);

		players = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		if (alwaysFly) registerEvents(new FlyDisableListener());
		if (alwaysFly || cancelOnLand) registerEvents(new SneakListener());
	}

	@Override
	public boolean castBuff(SpellData data) {
		if (!(data.target() instanceof Player target)) return false;

		if (heightMonitor == null) heightMonitor = new HeightMonitor();

		boolean constantMaxY = this.constantMaxY.get(data);
		boolean constantMaxAltitude = this.constantMaxAltitude.get(data);

		FlyData flyData = new FlyData(
			data,
			constantMaxY ? maxY.get(data) : 0,
			constantMaxAltitude ? maxAltitude.get(data) : 0,
			flySpeed.get(data),
			enableMaxY.get(data),
			constantMaxY,
			constantMaxAltitude,
			constantFlySpeed.get(data),
			target.getAllowFlight(),
			target.getFlySpeed()
		);
		players.put(target.getUniqueId(), flyData);

		float launchSpeed = this.launchSpeed.get(data);
		if (launchSpeed > 0) {
			target.teleport(target.getLocation().add(0, 0.25, 0));
			target.setVelocity(new Vector(0, launchSpeed, 0));
		}
		target.setAllowFlight(true);
		target.setFlying(true);
		target.setFlySpeed(flyData.flySpeed);

		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		if (!(data.target() instanceof Player target)) return false;

		FlyData oldData = players.remove(target.getUniqueId());

		boolean constantMaxY = this.constantMaxY.get(data);
		boolean constantMaxAltitude = this.constantMaxAltitude.get(data);

		FlyData flyData = new FlyData(
			data,
			constantMaxY ? maxY.get(data) : 0,
			constantMaxAltitude ? maxAltitude.get(data) : 0,
			flySpeed.get(data),
			enableMaxY.get(data),
			constantMaxY,
			constantMaxAltitude,
			constantFlySpeed.get(data),
			oldData.wasFlyingAllowed,
			oldData.oldFlySpeed
		);
		players.put(target.getUniqueId(), flyData);

		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return players.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		Player pl = (Player) entity;

		FlyData flyData = players.remove(pl.getUniqueId());
		pl.setFlying(false);
		pl.setAllowFlight(flyData.wasFlyingAllowed());
		pl.setFlySpeed(flyData.oldFlySpeed());
		pl.setFallDistance(0);

		if (heightMonitor == null || !players.isEmpty()) return;
		heightMonitor.stop();
		heightMonitor = null;
	}

	@Override
	protected void turnOff() {
		super.turnOff();

		if (heightMonitor == null) return;
		heightMonitor.stop();
		heightMonitor = null;
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return players.keySet();
	}

	public boolean shouldCancelOnLand() {
		return cancelOnLand;
	}

	public void setCancelOnLand(boolean cancelOnLand) {
		this.cancelOnLand = cancelOnLand;
	}

	public class FlyDisableListener implements Listener {

		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
			if (isActive(event.getPlayer()) && !event.isFlying()) event.setCancelled(true);
		}

	}

	public class SneakListener implements Listener {

		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
			Player player = event.getPlayer();
			if (!isActive(player) || player.getLocation().subtract(0, 1, 0).getBlock().getType().isAir()) return;

			if (alwaysFly) {
				player.teleport(player.getLocation().add(0, 0.25, 0));
				player.setFlying(true);
			} else if (cancelOnLand) turnOff(player);
		}

	}

	private class HeightMonitor implements Runnable {

		private final int taskId;

		private HeightMonitor() {
			taskId = MagicSpells.scheduleRepeatingTask(this, TimeUtil.TICKS_PER_SECOND, TimeUtil.TICKS_PER_SECOND);
		}

		@Override
		public void run() {
			for (UUID id : players.keySet()) {
				Player player = Bukkit.getPlayer(id);
				if (player == null || !player.isValid()) continue;

				FlyData data = players.get(id);

				addUseAndChargeCost(player);

				float flySpeed = data.constantFlySpeed ? data.flySpeed : WindwalkSpell.this.flySpeed.get(data.spellData);
				player.setFlySpeed(flySpeed);

				Location location = player.getLocation();
				Vector velocity = player.getVelocity();

				if (data.enableMaxY) {
					double maxY = data.constantMaxY ? data.maxY : WindwalkSpell.this.maxY.get(data.spellData);

					double diff = maxY - location.getY();
					if (diff < 0) {
						player.setVelocity(velocity.setY(diff * 1.5));
						continue;
					}
				}

				double maxAltitude = data.constantMaxAltitude ? data.maxAltitude : WindwalkSpell.this.maxAltitude.get(data.spellData);
				if (maxAltitude <= 0) return;

				Vector down = new Vector(0, -1, 0);

				RayTraceResult result = player.getWorld().rayTraceBlocks(location, down, maxAltitude, FluidCollisionMode.ALWAYS, true);
				if (result != null) return;

				location.add(0, -maxAltitude, 0);
				double distance = location.getY() - player.getWorld().getMinHeight();
				if (distance <= 0) return;

				result = player.getWorld().rayTraceBlocks(location, down, distance, FluidCollisionMode.ALWAYS, true);
				if (result != null) {
					Vector position = result.getHitPosition();
					distance = location.getY() - position.getY();
				}

				player.setVelocity(velocity.setY(distance * -1.5));
			}
		}

		public void stop() {
			MagicSpells.cancelTask(taskId);
		}

	}

	private record FlyData(SpellData spellData, double maxY, double maxAltitude, float flySpeed, boolean enableMaxY,
						   boolean constantMaxY, boolean constantMaxAltitude, boolean constantFlySpeed,
						   boolean wasFlyingAllowed, float oldFlySpeed) {
	}

}
