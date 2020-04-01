package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;

public class BroadcastEffect extends JutsuEffect {

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
			if (entity instanceof Player) MagicJutsus.sendMessage(message, (Player) entity, null);
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
			Util.forEachPlayerOnline(player -> MagicJutsus.sendMessage(message, player, null));
			return;
		}
		if (location == null) return;

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!player.getWorld().equals(location.getWorld())) continue;
			if (player.getLocation().distanceSquared(location) > rangeSq) continue;
			MagicJutsus.sendMessage(message, player, null);
		}

	}

}
