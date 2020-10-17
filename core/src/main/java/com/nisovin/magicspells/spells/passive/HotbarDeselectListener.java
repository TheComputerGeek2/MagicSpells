package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerItemHeldEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class HotbarDeselectListener extends PassiveListener {

	private final Set<MagicItemData> items = new HashSet<>();

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;
		MagicItemData itemData = null;
		MagicItem magicItem = MagicItems.getMagicItemFromString(var.trim());
		if (magicItem != null) itemData = magicItem.getMagicItemData();
		if (itemData == null) return;

		items.add(itemData);
	}

	@OverridePriority
	@EventHandler
	public void onPlayerScroll(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItem(event.getPreviousSlot());
		if (item == null || item.getType().isAir()) return;

		MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
		if (itemData == null) return;
		if (!items.isEmpty() && !contains(itemData)) return;

		if (!hasSpell(player)) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(player);
		if (!cancelDefaultAction(casted)) return;
		event.setCancelled(true);
	}

	private boolean contains(MagicItemData itemData) {
		for (MagicItemData data : items) {
			if (data.equals(itemData)) return true;
		}
		return false;
	}

}
