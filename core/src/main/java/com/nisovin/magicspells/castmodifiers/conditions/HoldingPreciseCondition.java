package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EntityEquipment;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

// TODO this should be refactored along with the other 'has item' related conditions to reduce redundant code
public class HoldingPreciseCondition extends Condition {

	private MagicItemData itemData = null;
	
	@Override
	public boolean initialize(String var) {
		MagicItem magicItem = MagicItems.getMagicItemFromString(var.trim());
		if (magicItem == null) return false;

		itemData = magicItem.getMagicItemData();
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		ItemStack item = livingEntity.getEquipment().getItemInMainHand();
		return check(item);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		EntityEquipment equip = target.getEquipment();
		return equip != null && check(equip.getItemInMainHand());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}
	
	private boolean check(ItemStack item) {
		MagicItemData data = MagicItems.getMagicItemDataFromItemStack(item);
		if (data == null) return false;

		return data.equals(itemData);
	}

}
