package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.GrindstoneInventory;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

@Name("grindstone")
public class GrindstoneListener extends PassiveListener {

	private Set<MagicItemData> upperItem;
	private Set<MagicItemData> lowerItem;
	private Set<MagicItemData> resultItem;

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;
		String[] split = var.split(" ", 3);

		if (split.length > 0) {
			if (!split[0].equals("any")) {
				String[] items = split[0].split(MagicItemDataParser.DATA_REGEX);
				upperItem = new HashSet<>();

				for (String item : items) {
					MagicItemData itemData = MagicItems.getMagicItemDataFromString(item);
					if (itemData == null) {
						MagicSpells.error("Invalid magic item '" + item + "' in grindstone trigger on passive spell '" + passiveSpell.getInternalName() + "'.");
						continue;
					}

					upperItem.add(itemData);
				}
			}
		}

		if (split.length > 1) {
			if (!split[1].equals("any")) {
				String[] items = split[1].split(MagicItemDataParser.DATA_REGEX);
				lowerItem = new HashSet<>();

				for (String item : items) {
					MagicItemData itemData = MagicItems.getMagicItemDataFromString(item);
					if (itemData == null) {
						MagicSpells.error("Invalid magic item '" + item + "' in grindstone trigger on passive spell '" + passiveSpell.getInternalName() + "'.");
						continue;
					}

					lowerItem.add(itemData);
				}
			}
		}

		if (split.length > 2) {
			if (!split[2].equals("any")) {
				String[] items = split[2].split(MagicItemDataParser.DATA_REGEX);
				resultItem = new HashSet<>();

				for (String item : items) {
					MagicItemData itemData = MagicItems.getMagicItemDataFromString(item);
					if (itemData == null) {
						MagicSpells.error("Invalid magic item '" + item + "' in grindstone trigger on passive spell '" + passiveSpell.getInternalName() + "'.");
						continue;
					}

					resultItem.add(itemData);
				}
			}
		}

		if (upperItem != null && upperItem.isEmpty()) upperItem = null;
		if (lowerItem != null && lowerItem.isEmpty()) lowerItem = null;
		if (resultItem != null && resultItem.isEmpty()) resultItem = null;
	}

	@OverridePriority
	@EventHandler
	public void onGrindstone(PrepareResultEvent event) {
		Inventory inventory = event.getInventory();
		if (!(inventory instanceof GrindstoneInventory grindstone)) return;

		LivingEntity caster = event.getView().getPlayer();
		if (!canTrigger(caster)) return;

		if (upperItem != null && !contains(upperItem, grindstone.getUpperItem())) return;
		if (lowerItem != null && !contains(lowerItem, grindstone.getLowerItem())) return;
		if (resultItem != null && !contains(resultItem, grindstone.getResult())) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setResult(null);
	}

	private boolean contains(Set<MagicItemData> items, ItemStack item) {
		if (item == null) item = ItemStack.empty();

		MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
		if (itemData == null) return false;

		for (MagicItemData data : items)
			if (data.matches(itemData))
				return true;

		return false;
	}

}
