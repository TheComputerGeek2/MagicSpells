package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

@Name("chestcontains")
public class ChestContainsCondition extends Condition {

	private static final Pattern FORMAT = Pattern.compile("(?<loc>[^,]+,-?\\d+,-?\\d+,-?\\d+),(?<item>.*)");

	//world,x,y,z,item

	private Location location;

	private MagicItemData itemData;

	@Override
	public boolean initialize(@NotNull String var) {
		Matcher matcher = FORMAT.matcher(var);
		if (!matcher.find()) return false;

		location = LocationUtil.fromString(matcher.group("loc"));
		itemData = MagicItems.getMagicItemDataFromString(matcher.group("item").trim());

		return location != null && itemData != null;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkChest();
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkChest();
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return checkChest();
	}

	private boolean checkChest() {
		Block block = location.getBlock();
		if (!(block.getState() instanceof Chest chest)) return false;

		for (ItemStack item : chest.getInventory().getContents()) {
			MagicItemData data = MagicItems.getMagicItemDataFromItemStack(item);
			if (data == null) continue;
			if (itemData.matches(data)) return true;
		}

		return false;
	}

}
