package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.spelleffects.SpellEffect;

public class TitleEffect extends SpellEffect {

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
		String titleMsg = title.isEmpty() ? "" : Util.doVarReplacementAndColorize(player, title);
		String subtitleMsg = subtitle.isEmpty() ? "" : Util.doVarReplacementAndColorize(player, subtitle);
		player.sendTitle(titleMsg, subtitleMsg, fadeIn, stay, fadeOut);
	}

}
