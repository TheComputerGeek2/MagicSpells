package com.nisovin.magicjutsus;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicjutsus.util.CastItem;

public class RightClickListener implements Listener {

	private MagicJutsus plugin;
	
	private Map<CastItem, Jutsu> rightClickCastItems = new HashMap<>();
	private Map<String, Long> lastCast = new HashMap<>();
	
	RightClickListener(MagicJutsus plugin) {
		this.plugin = plugin;
		for (Jutsu jutsu : MagicJutsus.getJutsus().values()) {
			CastItem[] items = jutsu.getRightClickCastItems();
			if (items.length <= 0) continue;
			for (CastItem item : items) {
				Jutsu old = rightClickCastItems.put(item, jutsu);
				if (old == null) continue;
				MagicJutsus.error("The jutsu '" + jutsu.getInternalName() + "' has same right-click-cast-item as '" + old.getInternalName() + "'!");
			}
		}
	}
	
	boolean hasRightClickCastItems() {
		return !rightClickCastItems.isEmpty();
	}
	
	@EventHandler
	public void onRightClick(final PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (!event.hasItem()) return;

		CastItem castItem = new CastItem(event.getItem());
		final Jutsu jutsu = rightClickCastItems.get(castItem);
		if (jutsu == null) return;

		Player player = event.getPlayer();
		Jutsubook jutsubook = MagicJutsus.getJutsubook(player);

		if (!jutsubook.hasJutsu(jutsu) || !jutsubook.canCast(jutsu)) return;

		if (!jutsu.ignoreGlobalCooldown) {
			Long lastCastTime = lastCast.get(player.getName());
			if (lastCastTime != null && lastCastTime + plugin.globalCooldown > System.currentTimeMillis()) return;
			lastCast.put(player.getName(), System.currentTimeMillis());
		}
			
		MagicJutsus.scheduleDelayedTask(() -> jutsu.cast(player), 0);
		event.setCancelled(true);
	}
	
}
