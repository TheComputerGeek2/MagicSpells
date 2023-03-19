package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EntityEquipment;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

// TODO this should be refactored along with the other 'has item' related conditions to reduce redundant code
public class HoldingPreciseCondition extends Condition {

	private MagicItemData itemData = null;
	
	@Override
	public boolean initialize(String var) {
		itemData = MagicItems.getMagicItemDataFromString(var);
		return itemData != null;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkHolding(caster);
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkHolding(target);
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}
	
	private boolean checkHolding(LivingEntity target) {
		EntityEquipment equipment = target.getEquipment();
		if (equipment == null) return false;

		ItemStack item = equipment.getItemInMainHand();
		MagicItemData data = MagicItems.getMagicItemDataFromItemStack(item);
		if (data == null) return false;

		return itemData.matches(data);
	}

}
