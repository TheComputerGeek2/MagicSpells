package com.nisovin.magicspells.spells.passive;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
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

	private ConfigData<ClickType> click;
	private ConfigData<InventoryAction> action;
	private ConfigData<InventoryType.SlotType> slotType;

	private MagicItemData itemHotbar;
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
		click = ConfigDataUtil.getEnum(config, "click", ClickType.class, null);
		action = ConfigDataUtil.getEnum(config, "action", InventoryAction.class, null);
		slotType = ConfigDataUtil.getEnum(config, "slot-type", InventoryType.SlotType.class, null);

		String hotbarString = config.getString("hotbar-item");
		itemHotbar = MagicItems.getMagicItemDataFromString(hotbarString);
		if (hotbarString != null && itemHotbar == null) {
			MagicSpells.error("Invalid 'hotbar-item' Magic Item specified in 'inventoryclick' trigger on PassiveSpell '" + passiveSpell.getInternalName() + "': " + hotbarString);
			return false;
		}

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

		ClickType click = this.click.get(data);
		if (click != null) {
			if (!event.getClick().equals(click)) return;

			if (click == ClickType.NUMBER_KEY && itemHotbar != null) {
				ItemStack itemStack = event.getView().getBottomInventory().getItem(event.getHotbarButton());

				MagicItemData item = MagicItems.getMagicItemDataFromItemStack(itemStack);
				if (item == null || !itemHotbar.matches(item)) return;
			}
		}

		InventoryAction action = this.action.get(data);
		if (action != null && !event.getAction().equals(action)) return;

		InventoryType.SlotType slotType = this.slotType.get(data);
		if (slotType != null && !event.getSlotType().equals(slotType)) return;

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
