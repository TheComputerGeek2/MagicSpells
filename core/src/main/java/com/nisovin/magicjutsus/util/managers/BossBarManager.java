package com.nisovin.magicjutsus.util.managers;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;

public class BossBarManager {

	private Map<UUID, BossBar> bars = new HashMap<>();

	public void setPlayerBar(Player player, String title, double progress, BarStyle style, BarColor color) {
		createBar(player, title, progress, style, color);
	}

	public void setPlayerBar(Player player, String title, double progress, BarStyle style) {
		createBar(player, title, progress, style, BarColor.PURPLE);
	}

	public void setPlayerBar(Player player, String title, double progress) {
		createBar(player, title, progress, BarStyle.SOLID, BarColor.PURPLE);
	}

	public void addPlayerBarFlag(Player player, BarFlag flag) {
		BossBar bar = bars.get(player.getUniqueId());
		if (bar == null) return;
		bar.addFlag(flag);
	}

	public void removePlayerBar(Player player) {
		BossBar bar = bars.remove(player.getUniqueId());
		if (bar != null) bar.removeAll();
	}

	public void turnOff() {
		bars.values().forEach(BossBar::removeAll);
		bars.clear();
	}

	private void createBar(Player player, String title, double progress, BarStyle style, BarColor color) {
		BossBar bar = bars.get(player.getUniqueId());
		if (bar == null) {
			bar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&', title), color, style);
			bars.put(player.getUniqueId(), bar);
		}
		bar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
		bar.setStyle(style);
		bar.setColor(color);
		bar.setProgress(progress);
		bar.addPlayer(player);
	}

}
