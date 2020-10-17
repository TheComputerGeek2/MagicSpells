package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

public class ChestContainsCondition extends Condition {

	//world,x,y,z,item

	private MagicLocation location;

	private MagicItemData itemData;

	@Override
	public boolean initialize(String var) {
		try {
			String[] vars = var.split(",");
			location = new MagicLocation(vars[0], Integer.parseInt(vars[1]), Integer.parseInt(vars[2]), Integer.parseInt(vars[3]));

			MagicItem magicItem = MagicItems.getMagicItemFromString(vars[4]);
			if (magicItem == null) return false;

			itemData = magicItem.getMagicItemData();
			return itemData != null;
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return checkChest();
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return checkChest();
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return checkChest();
	}

	private boolean checkChest() {
		Block block = location.getLocation().getBlock();
		if (!BlockUtils.isChest(block)) return false;

		Chest chest = (Chest) block.getState();
		ItemStack[] items = chest.getInventory().getContents();
		if (items.length == 0) return false;

		for (ItemStack item : items) {
			MagicItemData data = MagicItems.getMagicItemDataFromItemStack(item);
			if (data == null) continue;
			if (data.equals(itemData)) return true;
		}

		return false;
	}

}
