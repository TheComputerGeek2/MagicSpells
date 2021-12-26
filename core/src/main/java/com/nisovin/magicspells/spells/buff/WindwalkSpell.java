package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;

public class WindwalkSpell extends BuffSpell {

	private final Map<UUID, SpellData> entities;

	private ConfigData<Integer> maxY;
	private ConfigData<Integer> maxAltitude;

	private ConfigData<Float> flySpeed;
	private ConfigData<Float> launchSpeed;

	private boolean cancelOnLand;

	private HeightMonitor heightMonitor;

	public WindwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		maxY = getConfigDataInt("max-y", 260);
		maxAltitude = getConfigDataInt("max-altitude", 100);

		flySpeed = getConfigDataFloat("fly-speed", 0.1F);
		launchSpeed = getConfigDataFloat("launch-speed", 1F);

		cancelOnLand = getConfigBoolean("cancel-on-land", true);

		entities = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		if (cancelOnLand) registerEvents(new SneakListener());
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		if (!(entity instanceof Player player)) return true;

		float launchSpeed = this.launchSpeed.get(entity, null, power, args);
		if (launchSpeed > 0) {
			entity.teleport(entity.getLocation().add(0, 0.25, 0));
			entity.setVelocity(new Vector(0, launchSpeed, 0));
		}

		entities.put(entity.getUniqueId(), new SpellData(power, args));

		player.setAllowFlight(true);
		player.setFlying(true);
		player.setFlySpeed(flySpeed.get(entity, null, power, args));

		if (heightMonitor == null) heightMonitor = new HeightMonitor();

		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
		((Player) entity).setFlying(false);
		if (((Player) entity).getGameMode() != GameMode.CREATIVE) ((Player) entity).setAllowFlight(false);
		((Player) entity).setFlySpeed(0.1F);
		entity.setFallDistance(0);

		if (heightMonitor != null && entities.isEmpty()) {
			heightMonitor.stop();
			heightMonitor = null;
		}
	}

	@Override
	protected void turnOff() {
		for (UUID id : entities.keySet()) {
			Player player = Bukkit.getPlayer(id);
			if (player == null) continue;
			if (!player.isValid()) continue;
			turnOff(player);
		}

		entities.clear();

		if (heightMonitor == null) return;
		heightMonitor.stop();
		heightMonitor = null;
	}

	public Map<UUID, SpellData> getEntities() {
		return entities;
	}

	public boolean shouldCancelOnLand() {
		return cancelOnLand;
	}

	public void setCancelOnLand(boolean cancelOnLand) {
		this.cancelOnLand = cancelOnLand;
	}

	public class SneakListener implements Listener {

		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
			Player player = event.getPlayer();
			if (!isActive(player)) return;
			if (BlockUtils.isAir(player.getLocation().subtract(0, 1, 0).getBlock().getType())) return;
			turnOff(player);
		}

	}

	private class HeightMonitor implements Runnable {

		private final int taskId;

		private HeightMonitor() {
			taskId = MagicSpells.scheduleRepeatingTask(this, TimeUtil.TICKS_PER_SECOND, TimeUtil.TICKS_PER_SECOND);
		}

		@Override
		public void run() {
			for (UUID id : entities.keySet()) {
				Player pl = Bukkit.getPlayer(id);
				if (pl == null) continue;
				if (!pl.isValid()) continue;
				addUseAndChargeCost(pl);

				SpellData data = entities.get(id);

				int maxY = WindwalkSpell.this.maxY.get(pl, null, data.power(), data.args());
				if (maxY > 0) {
					int ydiff = pl.getLocation().getBlockY() - maxY;
					if (ydiff > 0) {
						pl.setVelocity(pl.getVelocity().setY(-ydiff * 1.5));
						continue;
					}
				}

				int maxAltitude = WindwalkSpell.this.maxAltitude.get(pl, null, data.power(), data.args());
				if (maxAltitude > 0) {
					int ydiff = pl.getLocation().getBlockY() - pl.getWorld().getHighestBlockYAt(pl.getLocation()) - maxAltitude;
					if (ydiff > 0) pl.setVelocity(pl.getVelocity().setY(-ydiff * 1.5));
				}

				pl.setFlySpeed(flySpeed.get(pl, null, data.power(), data.args()));
			}
		}

		public void stop() {
			MagicSpells.cancelTask(taskId);
		}

	}

}
