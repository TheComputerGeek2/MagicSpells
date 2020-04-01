package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;

public class TitleEffect extends JutsuEffect {

	private String title;
	private String subtitle;

	private int stay;
	private int fadeIn;
	private int fadeOut;

	private boolean broadcast;
	
	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		title = config.getString("title", "");
		subtitle = config.getString("subtitle", "");
		if (!title.isEmpty()) title = ChatColor.translateAlternateColorCodes('&', title);
		if (!subtitle.isEmpty()) subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);

		stay = config.getInt("stay", 40);
		fadeIn = config.getInt("fade-in", 10);
		fadeOut = config.getInt("fade-out", 10);
		broadcast = config.getBoolean("broadcast", false);
	}
	
	@Override
	protected Runnable playEffectEntity(Entity entity) {
		if (broadcast) Util.forEachPlayerOnline(this::send);
		else if (entity instanceof Player) send((Player) entity);
		return null;
	}
	
	private void send(Player player) {
		player.sendTitle(MagicJutsus.doVariableReplacements(player, title), MagicJutsus.doVariableReplacements(player, subtitle), fadeIn, stay, fadeOut);
	}

}
