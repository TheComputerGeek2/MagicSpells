package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.CraftItemEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class CraftListener extends PassiveListener {

	private final Set<ItemStack> items = new HashSet<>();

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		for (String itemString : var.split(",")) {
			MagicItem magicItem = MagicItems.getMagicItemFromString(itemString.trim());
			if (magicItem == null) continue;

			ItemStack item = magicItem.getItemStack();
			if (item == null) continue;

			items.add(item);
		}
	}

	@OverridePriority
	@EventHandler
	public void onCraft(CraftItemEvent event) {
		ItemStack item = event.getCurrentItem();
		if (item == null || item.getType().isAir()) return;

		Player player = (Player) event.getWhoClicked();
		if (!hasSpell(player)) return;

		// all items
		if (items.isEmpty()) {
			if (!isCancelStateOk(event.isCancelled())) return;
			boolean casted = passiveSpell.activate(player);
			if (cancelDefaultAction(casted)) event.setCancelled(true);

			return;
		}

		// doesn't contain the item
		if (!items.contains(item)) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(player);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
