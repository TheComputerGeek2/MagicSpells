package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;

public class ItemCooldownEffect extends SpellEffect {

	private ItemStack item;

	private int duration;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		MagicItem magicItem = MagicItems.getMagicItemFromString(config.getString("item", "stone"));
		if (magicItem != null) item = magicItem.getItemStack();
		duration = config.getInt("duration", TimeUtil.TICKS_PER_SECOND);
	}
	
	@Override
	protected Runnable playEffectEntity(Entity entity) {
		if (!(entity instanceof Player)) return null;
		if (item == null) return null;
		((Player) entity).setCooldown(item.getType(), duration);
		return null;
	}
	
}
