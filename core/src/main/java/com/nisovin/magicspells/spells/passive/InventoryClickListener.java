package com.nisovin.magicspells.spells.passive;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class InventoryClickListener extends PassiveListener {

	private MagicClick click;

	@Override
	public void initialize(String var) {
		InventoryAction action = null;
		ItemStack itemCurrent = null;
		ItemStack itemCursor = null;
		if (var != null && !var.isEmpty()) {
			String[] splits = var.split(" ");
			if (!splits[0].equals("null")) action = InventoryAction.valueOf(splits[0].toUpperCase());
			if (splits.length > 1 && !splits[1].equals("null")) {
				MagicItem magicItem = MagicItems.getMagicItemFromString(splits[1]);
				if (magicItem != null) itemCurrent = magicItem.getItemStack();
			}
			if (splits.length > 2) {
				MagicItem magicItem = MagicItems.getMagicItemFromString(splits[2]);
				if (magicItem != null) itemCursor = magicItem.getItemStack();
			}
		}
		click = new MagicClick(action, itemCurrent, itemCursor);
	}

	@OverridePriority
	@EventHandler
	public void onInvClick(InventoryClickEvent event) {
		Player player = Bukkit.getPlayer(event.getWhoClicked().getUniqueId());
		if (player == null) return;
		if (!hasSpell(player)) return;

		// Valid action, but not used.
		if (click.action != null && !event.getAction().equals(click.action)) return;

		// Valid clicked item, but not used.
		if (click.itemCurrent != null) {
			ItemStack item = event.getCurrentItem();
			if (item == null) return;

			MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
			if (itemData == null) return;

			MagicItemData currentItemData = MagicItems.getMagicItemDataFromItemStack(click.itemCurrent);
			if (currentItemData == null) return;
			if (!currentItemData.equals(itemData)) return;
		}
		// Valid cursor item, but not used.
		if (click.itemCursor != null) {
			ItemStack item = event.getCursor();
			if (item == null) return;

			MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
			if (itemData == null) return;

			MagicItemData cursorItemData = MagicItems.getMagicItemDataFromItemStack(click.itemCursor);
			if (cursorItemData == null) return;
			if (!itemData.equals(cursorItemData)) return;
		}

		boolean casted = passiveSpell.activate(player);
		if (!cancelDefaultAction(casted)) return;
		event.setCancelled(true);
	}

	private static class MagicClick {

		private InventoryAction action;
		private ItemStack itemCurrent;
		private ItemStack itemCursor;

		private MagicClick(InventoryAction action, ItemStack itemCurrent, ItemStack itemCursor) {
			this.action = action;
			this.itemCurrent = itemCurrent;
			this.itemCursor = itemCursor;
		}

	}

}
