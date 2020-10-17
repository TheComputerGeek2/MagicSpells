package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.SpellEffect;

public class BroadcastEffect extends SpellEffect {

	private String message;

	private int range;
	private int rangeSq;

	private boolean targeted;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		message = config.getString("message", "");
		range = config.getInt("range", 0);
		rangeSq = range * range;
		targeted = config.getBoolean("targeted", false);
	}

	@Override
	public Runnable playEffectLocation(Location location) {
		broadcast(location, message);
		return null;
	}
	
	@Override
	public Runnable playEffectEntity(Entity entity) {
		if (targeted) {
			if (entity instanceof Player) MagicSpells.sendMessage(message, (Player) entity, null);
			return null;
		}
		String msg = message;
		if (entity instanceof Player) {
			msg = msg.replace("%a", ((Player) entity).getDisplayName())
					.replace("%t", ((Player) entity).getDisplayName())
					.replace("%n", entity.getName());
		}
		broadcast(entity == null ? null : entity.getLocation(), msg);

		return null;
	}
	
	private void broadcast(Location location, String message) {
		if (range <= 0) {
			Util.forEachPlayerOnline(player -> MagicSpells.sendMessage(message, player, null));
			return;
		}
		if (location == null) return;

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!player.getWorld().equals(location.getWorld())) continue;
			if (player.getLocation().distanceSquared(location) > rangeSq) continue;
			MagicSpells.sendMessage(message, player, null);
		}

	}

}
