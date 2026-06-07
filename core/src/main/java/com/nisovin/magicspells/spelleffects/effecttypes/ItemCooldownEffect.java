package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.magicitems.MagicItems;

@Name("itemcooldown")
public class ItemCooldownEffect extends SpellEffect {

	private ConfigData<String> item;
	private ConfigData<Integer> duration;
	private ConfigData<NamespacedKey> key;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		item = ConfigDataUtil.getString(config, "item", "stone");
		key = ConfigDataUtil.getNamespacedKey(config, "key", null);
		duration = ConfigDataUtil.getInteger(config, "duration", TimeUtil.TICKS_PER_SECOND);
	}

	@Override
	protected Runnable playEffectEntity(Entity entity, SpellData data) {
		if (!(entity instanceof Player player)) return null;

		int duration = this.duration.get(data);

		NamespacedKey key = this.key.get(data);
		if (key == null) {
			MagicItem item = MagicItems.getMagicItemFromString(this.item.get(data));
			if (item != null) player.setCooldown(item.getItemStack(), duration);
		} else player.setCooldown(key, duration);

		return null;
	}

}
