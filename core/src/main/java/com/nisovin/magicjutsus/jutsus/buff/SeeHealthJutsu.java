package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.UUID;
import java.util.Random;
import java.util.HashSet;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;

public class SeeHealthJutsu extends BuffJutsu {

	private Set<UUID> bars;

	private String colors = "01234567890abcdef";
	private Random random = new Random();

	private int barSize;
	private int interval;
	private String symbol;

	private Updater updater;

	public SeeHealthJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		barSize = getConfigInt("bar-size", 20);
		interval = getConfigInt("update-interval", 5);
		symbol = getConfigString("symbol", "=");

		bars = new HashSet<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		if (!(entity instanceof Player)) return true;
		bars.add(entity.getUniqueId());
		updater = new Updater();
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return bars.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		bars.remove(entity.getUniqueId());

		if (updater != null && bars.isEmpty()) {
			updater.stop();
			updater = null;
		}
	}

	@Override
	protected void turnOff() {
		for (UUID id : bars) {
			Player player = Bukkit.getPlayer(id);
			if (player == null) continue;
			if (!player.isValid()) continue;
			player.updateInventory();
		}

		bars.clear();
		if (updater != null) {
			updater.stop();
			updater = null;
		}
	}

	private ChatColor getRandomColor() {
		return ChatColor.getByChar(colors.charAt(random.nextInt(colors.length())));
	}
	
	private void showHealthBar(Player player, LivingEntity entity) {
		double pct = entity.getHealth() / Util.getMaxHealth(entity);

		ChatColor color = ChatColor.GREEN;
		if (pct <= 0.2) color = ChatColor.DARK_RED;
		else if (pct <= 0.4) color = ChatColor.RED;
		else if (pct <= 0.6) color = ChatColor.GOLD;
		else if (pct <= 0.8) color = ChatColor.YELLOW;

		StringBuilder sb = new StringBuilder(barSize);
		sb.append(getRandomColor().toString());
		int remain = (int) Math.round(barSize * pct);
		sb.append(color.toString());

		for (int i = 0; i < remain; i++) sb.append(symbol);

		if (remain < barSize) {
			sb.append(ChatColor.DARK_GRAY.toString());
			for (int i = 0; i < barSize - remain; i++) sb.append(symbol);
		}

		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(sb.toString()));
	}
	
	private class Updater implements Runnable {

		private int taskId;

		private Updater() {
			taskId = MagicJutsus.scheduleRepeatingTask(this, 0, interval);
		}
		
		@Override
		public void run() {
			for (UUID id : bars) {
				Player player = Bukkit.getPlayer(id);
				if (player == null) continue;
				if (!player.isValid()) continue;
				TargetInfo<LivingEntity> target = getTargetedEntity(player, 1F);
				if (target != null) showHealthBar(player, target.getTarget());
			}
		}
		
		public void stop() {
			MagicJutsus.cancelTask(taskId);
		}
		
	}

}
