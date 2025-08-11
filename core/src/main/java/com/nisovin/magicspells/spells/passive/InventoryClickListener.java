package com.nisovin.magicspells.spells.passive;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("inventoryclick")
public class InventoryClickListener extends PassiveListener {

	private ConfigData<InventoryAction> action;

	private MagicItemData itemCursor;
	private MagicItemData itemCurrent;

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;
		String[] splits = var.split(" ");

		if (!splits[0].equals("null")) {
			try {
				InventoryAction invAction = InventoryAction.valueOf(splits[0].toUpperCase());
				action = data -> invAction;
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid inventory action '" + splits[0] + "' in 'inventoryclick' trigger on PassiveSpell '" + passiveSpell.getInternalName() + "'");
			}
		}

		if (splits.length > 1 && !splits[1].isEmpty() && !splits[1].equals("null")) {
			itemCurrent = MagicItems.getMagicItemDataFromString(splits[1]);

			if (itemCurrent == null) {
				MagicSpells.error("Invalid magic item '" + splits[1] + "' in 'inventoryclick' trigger on PassiveSpell '" + passiveSpell.getInternalName() + "'");
			}
		}

		if (splits.length > 2 && !splits[2].isEmpty() && !splits[2].equals("null")) {
			itemCursor = MagicItems.getMagicItemDataFromString(splits[2]);

			if (itemCursor == null) {
				MagicSpells.error("Invalid magic item '" + splits[2] + "' in 'inventoryclick' trigger on PassiveSpell '" + passiveSpell.getInternalName() + "'");
			}
		}
	}

	@Override
	public boolean initialize(@NotNull ConfigurationSection config) {
		action = ConfigDataUtil.getEnum(config, "action", InventoryAction.class, null);

		String currentString = config.getString("current-item");
		itemCurrent = MagicItems.getMagicItemDataFromString(currentString);
		if (currentString != null && itemCurrent == null) {
			MagicSpells.error("Invalid 'current-item' Magic Item specified in 'inventoryclick' trigger on PassiveSpell '" + passiveSpell.getInternalName() + "': " + currentString);
			return false;
		}

		String cursorString = config.getString("cursor-item");
		itemCursor = MagicItems.getMagicItemDataFromString(cursorString);
		if (cursorString != null && itemCursor == null) {
			MagicSpells.error("Invalid 'cursor-item' Magic Item specified in 'inventoryclick' trigger on PassiveSpell '" + passiveSpell.getInternalName() + "': " + cursorString);
			return false;
		}

		return true;
	}

	@OverridePriority
	@EventHandler
	public void onInvClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) return;
		if (!canTrigger(player)) return;
		SpellData data = new SpellData(player);

		InventoryAction action = this.action.get(data);
		if (action != null && !event.getAction().equals(action)) return;

		if (itemCurrent != null) {
			MagicItemData item = MagicItems.getMagicItemDataFromItemStack(event.getCurrentItem());
			if (item == null || !itemCurrent.matches(item)) return;
		}

		if (itemCursor != null) {
			MagicItemData item = MagicItems.getMagicItemDataFromItemStack(event.getCursor());
			if (item == null || !itemCursor.matches(item)) return;
		}

		boolean casted = passiveSpell.activate(data);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
