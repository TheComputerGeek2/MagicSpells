package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

@Name("prepareenchant")
public class PrepareEnchantListener extends PassiveListener {

	private Set<MagicItemData> items;

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;
		items = new HashSet<>();

		for (String item : var.split(MagicItemDataParser.DATA_REGEX)) {
			MagicItemData itemData = MagicItems.getMagicItemDataFromString(item);
			if (itemData == null) {
				MagicSpells.error("Invalid magic item '" + item + "' in enchant trigger on passive spell '" + passiveSpell.getInternalName() + "'.");
				continue;
			}

			items.add(itemData);
		}

		if (items.isEmpty()) items = null;
	}

	@OverridePriority
	@EventHandler
	public void onEnchant(PrepareItemEnchantEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		LivingEntity caster = event.getEnchanter();
		if (!canTrigger(caster)) return;

		if (items != null && !contains(event.getItem())) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

	private boolean contains(ItemStack item) {
		if (item == null) item = ItemStack.empty();

		MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(item);
		if (itemData == null) return false;

		for (MagicItemData data : items)
			if (data.matches(itemData))
				return true;

		return false;
	}

}
