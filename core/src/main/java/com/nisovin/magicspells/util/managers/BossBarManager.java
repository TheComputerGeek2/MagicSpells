package com.nisovin.magicspells.util.managers;

import java.util.*;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;

public class BossBarManager {

	private final Pattern VALID_NAMESPACE_KEY = Pattern.compile("[a-z0-9._-]+");

	private final String NAMESPACE_KEY_DEFAULT = "ms_default";
	private final String NAMESPACE_KEY_VARIABLE = "ms_variable";

	private final Set<Bar> bars = new HashSet<>();

	public String getNamespaceKeyVariable() {
		return NAMESPACE_KEY_VARIABLE;
	}

	public boolean isNameSpaceKey(String namespaceKey) {
		return VALID_NAMESPACE_KEY.matcher(namespaceKey).matches();
	}

	private NamespacedKey createNamespaceKey(String namespaceKey) {
		return new NamespacedKey(MagicSpells.plugin, namespaceKey);
	}

	public Bar getBar(Player player, String namespaceKey) {
		if (namespaceKey == null || namespaceKey.isEmpty()) namespaceKey = NAMESPACE_KEY_DEFAULT;
		// Check if the bar exists.
		Bar finalBar = null;
		for (Bar bar : bars) {
			if (bar.player.equals(player) && bar.namespaceKey.equals(namespaceKey)) {
				finalBar = bar;
				break;
			}
		}
		// If it doesn't, create it.
		if (finalBar == null) {
			BossBar bossBar = Bukkit.createBossBar(createNamespaceKey(namespaceKey), "", BarColor.WHITE, BarStyle.SOLID);
			finalBar = new Bar(bossBar, player, namespaceKey);
			bars.add(finalBar);
		}
		return finalBar;
	}

	public void turnOff() {
		bars.forEach(Bar::deleteBossbar);
		bars.clear();
	}

	public class Bar {

		final BossBar bossbar;
		final Player player;
		final String namespaceKey;

		public Bar(BossBar bossbar, Player player, String namespaceKey) {
			this.bossbar = bossbar;
			this.player = player;
			this.namespaceKey = namespaceKey;
		}

		public void set(String title, double progress, BarStyle style, BarColor color) {
			bossbar.setTitle(Util.colorize(title));
			bossbar.setStyle(style);
			bossbar.setColor(color);
			bossbar.setProgress(progress);
			bossbar.addPlayer(player);
		}

		public void addFlag(BarFlag flag) {
			bossbar.addFlag(flag);
		}

		public void deleteBossbar() {
			bossbar.removeAll();
			Bukkit.removeBossBar(createNamespaceKey(namespaceKey));
		}

		public void remove() {
			deleteBossbar();
			bars.remove(this);
		}

	}

}
