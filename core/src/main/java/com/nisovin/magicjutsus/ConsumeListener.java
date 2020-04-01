package com.nisovin.magicjutsus;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import com.nisovin.magicjutsus.util.CastItem;
import com.nisovin.magicjutsus.Jutsu.JutsuCastState;
import com.nisovin.magicjutsus.Jutsu.JutsuCastResult;

public class ConsumeListener implements Listener {

	private MagicJutsus plugin;

	private Map<CastItem, Jutsu> consumeCastItems = new HashMap<>();
	private HashMap<String, Long> lastCast = new HashMap<>();
	
	ConsumeListener(MagicJutsus plugin) {
		this.plugin = plugin;
		for (Jutsu jutsu : MagicJutsus.getJutsus().values()) {
			CastItem[] items = jutsu.getConsumeCastItems();
			if (items.length <= 0) continue;
			for (CastItem item : items) {
				Jutsu old = consumeCastItems.put(item, jutsu);
				if (old == null) continue;
				MagicJutsus.error("The jutsu '" + jutsu.getInternalName() + "' has same consume-cast-item as '" + old.getInternalName() + "'!");
			}
		}
	}
	
	boolean hasConsumeCastItems() {
		return !consumeCastItems.isEmpty();
	}
	
	@EventHandler
	public void onConsume(final PlayerItemConsumeEvent event) {
		CastItem castItem = new CastItem(event.getItem());
		final Jutsu jutsu = consumeCastItems.get(castItem);
		if (jutsu == null) return;

		Player player = event.getPlayer();
		Long lastCastTime = lastCast.get(player.getName());
		if (lastCastTime != null && lastCastTime + plugin.globalCooldown > System.currentTimeMillis()) return;
		lastCast.put(player.getName(), System.currentTimeMillis());

		if (MagicJutsus.getJutsubook(player).canCast(jutsu)) {
			JutsuCastResult result = jutsu.cast(event.getPlayer());
			if (result.state != JutsuCastState.NORMAL) event.setCancelled(true);
		}
	}

}
