package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

@Name("itemcooldown")
public class ItemCooldownEffect extends SpellEffect {

	private ConfigData<Material> item;
	private ConfigData<Integer> duration;
	private ConfigData<NamespacedKey> key;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		key = ConfigDataUtil.getNamespacedKey(config, "key", null);
		item = ConfigDataUtil.getMaterial(config, "item", Material.STONE);
		duration = ConfigDataUtil.getInteger(config, "duration", TimeUtil.TICKS_PER_SECOND);
	}

	@Override
	protected Runnable playEffectEntity(Entity entity, SpellData data) {
		if (!(entity instanceof Player player)) return null;

		int duration = this.duration.get(data);

		NamespacedKey key = this.key.get(data);
		if (key == null) player.setCooldown(item.get(data), duration);
		else player.setCooldown(key, duration);

		return null;
	}

}
