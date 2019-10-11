package com.nisovin.magicspells;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.util.CastItem;
import org.bukkit.inventory.ItemStack;

public class RightClickListener implements Listener {

	private MagicSpells plugin;
	
	private Map<CastItem, Spell> rightClickCastItems = new HashMap<>();
	private Map<String, Long> lastCast = new HashMap<>();
	
	RightClickListener(MagicSpells plugin) {
		this.plugin = plugin;
		for (Spell spell : MagicSpells.getSpells().values()) {
			CastItem[] items = spell.getRightClickCastItems();
			if (items.length <= 0) continue;
			for (CastItem item : items) {
				Spell old = rightClickCastItems.put(item, spell);
				if (old == null) continue;
				MagicSpells.error("The spell '" + spell.getInternalName() + "' has same right-click-cast-item as '" + old.getInternalName() + "'!");
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
		final Spell spell = rightClickCastItems.get(castItem);
		if (spell == null) return;

		Player player = event.getPlayer();
		Spellbook spellbook = MagicSpells.getSpellbook(player);

		if (!spellbook.hasSpell(spell) || !spellbook.canCast(spell)) return;

		if (!spell.ignoreGlobalCooldown) {
			Long lastCastTime = lastCast.get(player.getName());
			if (lastCastTime != null && lastCastTime + plugin.globalCooldown > System.currentTimeMillis()) return;
			lastCast.put(player.getName(), System.currentTimeMillis());
		}
			
		MagicSpells.scheduleDelayedTask(() -> spell.cast(event.getPlayer()), 0);

		if (plugin.getIgnoreEventCancelItems().contains(castItem.getItemType())) return;
		event.setCancelled(true);
	}
	
}
