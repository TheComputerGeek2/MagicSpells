package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

public class HoveringWithCondition extends Condition {

	private MagicItemData itemData;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;

		itemData = MagicItems.getMagicItemDataFromString(var);
		return itemData != null;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkHovering(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkHovering(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean checkHovering(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;

		ItemStack itemCursor = pl.getOpenInventory().getCursor();
		if (itemCursor == null) return false;

		MagicItemData cursorData = MagicItems.getMagicItemDataFromItemStack(itemCursor);
		if (cursorData == null) return false;

		return itemData.matches(cursorData);
	}

}
