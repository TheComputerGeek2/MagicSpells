package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.TimeUtil;

public class ItemCooldownEffect extends JutsuEffect {

	private ItemStack item;

	private int duration;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		item = Util.getItemStackFromString(config.getString("item", "stone"));
		duration = config.getInt("duration", TimeUtil.TICKS_PER_SECOND);
	}
	
	@Override
	protected Runnable playEffectEntity(Entity entity) {
		if (!(entity instanceof Player)) return null;
		((Player) entity).setCooldown(item.getType(), duration);
		return null;
	}
	
}
