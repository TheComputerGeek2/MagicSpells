package com.nisovin.magicspells.spells.buff;

import java.util.*;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.config.ConfigData;

public class SeeHealthSpell extends BuffSpell {

	private final Map<UUID, SeeHealthData> players;

	private int interval;
	private final ConfigData<Integer> barSize;

	private final ConfigData<String> symbol;

	private final ConfigData<Boolean> constantSymbol;
	private final ConfigData<Boolean> constantBarSize;

	private Updater updater;

	public SeeHealthSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		barSize = getConfigDataInt("bar-size", 20);
		interval = getConfigInt("update-interval", 5);

		symbol = getConfigDataString("symbol", "=");

		constantSymbol = getConfigDataBoolean("constant-symbol", true);
		constantBarSize = getConfigDataBoolean("constant-bar-size", true);

		players = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		if (!(data.target() instanceof Player target)) return false;
		if (updater == null) updater = new Updater();

		boolean constantSymbol = this.constantSymbol.get(data);
		boolean constantBarSize = this.constantBarSize.get(data);

		String symbol = constantSymbol ? this.symbol.get(data) : null;
		int barSize = constantBarSize ? this.barSize.get(data) : 0;

		players.put(target.getUniqueId(), new SeeHealthData(data, barSize, symbol, constantBarSize, constantSymbol));

		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return players.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		players.remove(entity.getUniqueId());

		if (updater != null && players.isEmpty()) {
			updater.stop();
			updater = null;
		}
	}

	@Override
	protected void turnOff() {
		players.clear();

		if (updater != null) {
			updater.stop();
			updater = null;
		}
	}

	private void showHealthBar(Player player, SeeHealthData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data.spellData);
		if (info.noTarget()) return;

		SpellData subData = info.spellData();
		LivingEntity target = subData.target();

		double percent = target.getHealth() / Util.getMaxHealth(target);

		NamedTextColor color = NamedTextColor.GREEN;
		if (percent <= 0.2) color = NamedTextColor.DARK_RED;
		else if (percent <= 0.4) color = NamedTextColor.RED;
		else if (percent <= 0.6) color = NamedTextColor.GOLD;
		else if (percent <= 0.8) color = NamedTextColor.YELLOW;

		String symbol = data.constantSymbol ? data.symbol : this.symbol.get(subData);
		int barSize = data.constantBarSize ? data.barSize : this.barSize.get(subData);

		int remaining = (int) Math.round(percent * barSize);
		int lost = barSize - remaining;

		Component healthBar = Component.empty();
		if (remaining > 0) healthBar = healthBar.append(Component.text(symbol.repeat(remaining)).color(color));
		if (lost > 0) healthBar = healthBar.append(Component.text(symbol.repeat(lost)).color(NamedTextColor.GRAY));

		player.sendActionBar(healthBar);
	}

	public Map<UUID, SeeHealthData> getPlayers() {
		return players;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	private class Updater implements Runnable {

		private final ScheduledTask task;

		private Updater() {
			task = MagicSpells.scheduleRepeatingTask(this, 0, interval);
		}

		@Override
		public void run() {
			for (Map.Entry<UUID, SeeHealthData> entry : players.entrySet()) {
				UUID id = entry.getKey();
				Player player = Bukkit.getPlayer(id);
				if (player == null || !player.isValid()) continue;

				SeeHealthData data = entry.getValue();
				showHealthBar(player, data);
			}
		}

		public void stop() {
			MagicSpells.cancelTask(task);
		}

	}

	public record SeeHealthData(SpellData spellData, int barSize, String symbol, boolean constantBarSize, boolean constantSymbol) {
	}

}
