package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;

public class WaterwalkSpell extends BuffSpell {

	private final Map<UUID, SpellData> entities;

	private ConfigData<Float> speed;

	private Ticker ticker;

	public WaterwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		speed = getConfigDataFloat("speed", 0.05F);

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		if (!(entity instanceof Player)) return true;
		entities.put(entity.getUniqueId(), new SpellData(power, args));
		startTicker();
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
		Player player = (Player) entity;

		player.setFlying(false);
		if (player.getGameMode() != GameMode.CREATIVE) player.setAllowFlight(false);
		player.setFlySpeed(0.1F);

		if (entities.isEmpty()) stopTicker();
	}

	@Override
	protected void turnOff() {
		for (UUID id : entities.keySet()) {
			Player pl = Bukkit.getPlayer(id);
			if (pl == null) continue;
			if (!pl.isValid()) continue;

			pl.setFlying(false);
			if (pl.getGameMode() != GameMode.CREATIVE) pl.setAllowFlight(false);
		}

		entities.clear();
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

	public Map<UUID, SpellData> getEntities() {
		return entities;
	}

	private class Ticker implements Runnable {

		private final int taskId;

		private int count = 0;

		private Ticker() {
			taskId = MagicSpells.scheduleRepeatingTask(this, 5, 5);
		}

		@Override
		public void run() {
			count++;
			if (count >= 4) count = 0;

			Block feet;
			Block underfeet;
			Location loc;

			for (UUID id : entities.keySet()) {
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

				SpellData data = entities.get(id);
				if (BlockUtils.isAir(feet.getType()) && underfeet.getType() == Material.WATER) {
					if (!pl.isFlying()) {
						pl.setAllowFlight(true);
						pl.setFlying(true);
						pl.setFlySpeed(speed.get(pl, null, data.power(), data.args()));
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
			MagicSpells.cancelTask(taskId);
		}

	}

}
