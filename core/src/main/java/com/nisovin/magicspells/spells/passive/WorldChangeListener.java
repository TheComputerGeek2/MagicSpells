package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class WorldChangeListener extends PassiveListener {

	private final Set<String> worldNames = new HashSet<>();

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		for (String worldName : var.split(",")) {
			World world = Bukkit.getWorld(worldName);
			if (world == null) continue;

			worldNames.add(worldName);
		}
	}

	@OverridePriority
	@EventHandler
	public void onWorldChange(PlayerTeleportEvent event) {
		World worldFrom = event.getFrom().getWorld();
		if (worldFrom == null) return;

		Location locTo = event.getTo();
		if (locTo == null) return;

		World worldTo = locTo.getWorld();
		if (worldTo == null) return;
		if (worldFrom.equals(worldTo)) return;

		if (!worldNames.isEmpty() && !worldNames.contains(worldTo.getName())) return;
		if (!hasSpell(event.getPlayer())) return;
		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(event.getPlayer());
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
