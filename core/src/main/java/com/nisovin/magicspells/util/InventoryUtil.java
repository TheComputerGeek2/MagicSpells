package com.nisovin.magicspells.util;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.inventory.InventoryType;

import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

public class InventoryUtil {

	private static final String SERIALIZATION_KEY_SIZE = "size";
	private static final String SERIALIZATION_KEY_TYPE = "type";
	private static final String SERIALIZATION_KEY_TITLE = "title";
	private static final String SERIALIZATION_KEY_CONTENTS = "contents";

	/*
	 * type: INVENTORY_TYPE/string
	 * size: integer
	 * title: string
	 * contents:
	 *     slot number: serialized itemstack
	 *     slot number: serialized itemstack
	 */
	public static Map<Object, Object> serializeInventoryContents(Inventory inv, InventoryView view) {
		Map<Object, Object> ret = new HashMap<>();
		ItemStack[] contents = inv.getContents();
		String inventoryType = inv.getType().name();
		int size = inv.getSize();
		String title = view.getTitle();
		
		// A map of slot to itemstack
		Map<Object, Object> serializedContents = createContentsMap(contents);
		
		ret.put(SERIALIZATION_KEY_SIZE, size);
		ret.put(SERIALIZATION_KEY_TYPE, inventoryType);
		ret.put(SERIALIZATION_KEY_TITLE, title);
		ret.put(SERIALIZATION_KEY_CONTENTS, serializedContents);

		return ret;
	}
	
	private static Map<Object, Object> createContentsMap(ItemStack[] items) {
		Map<Object, Object> serialized = new HashMap<>();
		int maxSlot = items.length - 1;
		for (int currentSlot = 0; currentSlot <= maxSlot; currentSlot++) {
			ItemStack currentItem = items[currentSlot];
			if (currentItem == null) continue;
			serialized.put(currentSlot, currentItem.serialize());
		}
		return serialized;
	}
	
	public static Inventory deserializeInventory(Map<Object, Object> serialized) {
		String strInventoryType = (String) serialized.get(SERIALIZATION_KEY_TYPE);
		int inventorySize = (Integer) serialized.get(SERIALIZATION_KEY_SIZE);
		String title = (String) serialized.get(SERIALIZATION_KEY_TITLE);
		Inventory ret;
		if (strInventoryType.equals(InventoryType.CHEST.name())) ret = Bukkit.createInventory(null, inventorySize, title);
		else ret = Bukkit.createInventory(null, InventoryType.valueOf(strInventoryType), title);

		// Handle the item contents
		Map<Object, Object> serializedItems = (Map<Object, Object>) serialized.get(SERIALIZATION_KEY_CONTENTS);
		ret.setContents(deserializeContentsMap(serializedItems, inventorySize));
		
		return ret;
	}
	
	private static ItemStack[] deserializeContentsMap(Map<Object, Object> contents, int size) {
		ItemStack[] ret = new ItemStack[size];
		
		// Can we exit early?
		if (contents == null) return ret;
		
		for (int i = 0; i < size; i++) {
			Map<String, Object> serializedStack = (Map<String, Object>) contents.get(i);
			if (serializedStack == null) continue;
			ret[i] = ItemStack.deserialize(serializedStack);
		}
		
		return ret;
	}
	
	public static boolean isNothing(ItemStack itemStack) {
		if (itemStack == null) return true;
		if (BlockUtils.isAir(itemStack.getType())) return true;
		return itemStack.getAmount() == 0;
	}

	public static boolean inventoryContains(EntityEquipment entityEquipment, SpellReagents.ReagentItem item) {
		if (entityEquipment == null) return false;
		MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item.getItemStack());
		if (itemData == null) return false;

		int count = 0;
		ItemStack[] armorContents = entityEquipment.getArmorContents();
		ItemStack mainHand = entityEquipment.getItemInMainHand();
		ItemStack offHand = entityEquipment.getItemInOffHand();
		ItemStack[] equipment = new ItemStack[6];

		// first 4 slots are filled with armor
		for (int j = 0; j < 4; j++) {
			equipment[j] = armorContents[j];
		}
		equipment[4] = mainHand;
		equipment[5] = offHand;

		for (ItemStack itemInside : equipment) {
			MagicItemData magicItemData = MagicItems.getMagicItemDataFromItemStack(itemInside);
			if (magicItemData == null) continue;
			if (itemInside == null) continue;
			if (magicItemData.equals(itemData)) count += itemInside.getAmount();
			if (count >= item.getAmount()) return true;
		}
		return false;
	}

	public static boolean inventoryContains(Inventory inventory, SpellReagents.ReagentItem item) {
		if (inventory == null) return false;
		MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item.getItemStack());
		if (itemData == null) return false;
		int count = 0;
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] == null) continue;
			MagicItemData magicItemData = MagicItems.getMagicItemDataFromItemStack(items[i]);
			if (magicItemData == null) continue;
			if (magicItemData.equals(itemData)) count += items[i].getAmount();
			if (count >= item.getAmount()) return true;
		}
		return false;
	}

	public static ItemStack[] getEquipmentItems(EntityEquipment entityEquipment) {
		ItemStack[] armorContents = entityEquipment.getArmorContents();
		ItemStack mainHand = entityEquipment.getItemInMainHand();
		ItemStack offHand = entityEquipment.getItemInOffHand();
		ItemStack[] equipment = new ItemStack[6];

		// first 4 slots are filled with armor
		for (int j = 0; j < 4; j++) {
			equipment[j] = armorContents[j];
		}
		equipment[4] = mainHand;
		equipment[5] = offHand;

		return equipment;
	}
	
}
