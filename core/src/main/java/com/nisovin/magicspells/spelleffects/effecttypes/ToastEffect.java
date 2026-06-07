package com.nisovin.magicspells.spelleffects.effecttypes;

import net.kyori.adventure.text.Component;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.magicitems.MagicItems;

import io.papermc.paper.advancement.AdvancementDisplay.Frame;

@Name("toast")
public class ToastEffect extends SpellEffect {

	private String text;
	private ItemStack icon;
	private ConfigData<Frame> frame;
	private ConfigData<Boolean> broadcast;
	private ConfigData<Boolean> useViewerAsTarget;
	private ConfigData<Boolean> useViewerAsDefault;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		text = config.getString("text", "");
		frame = ConfigDataUtil.getEnum(config, "frame", Frame.class, Frame.TASK);

		String magicItemString = config.getString("icon", "paper");
		MagicItem magicItem = MagicItems.getMagicItemFromString(magicItemString);
		if (magicItem != null) {
			ItemStack item = magicItem.getItemStack();
			icon = item.isEmpty() ? null : item;
		}
		if (icon == null) MagicSpells.error("Invalid toast effect icon specified: '" + magicItemString + "'");

		broadcast = ConfigDataUtil.getBoolean(config, "broadcast", false);
		useViewerAsTarget = ConfigDataUtil.getBoolean(config, "use-viewer-as-target", false);
		useViewerAsDefault = ConfigDataUtil.getBoolean(config, "use-viewer-as-default", true);
	}

	@Override
	protected Runnable playEffectEntity(Entity entity, SpellData data) {
		if (icon == null) return null;

		if (broadcast.get(data)) Util.forEachPlayerOnline(player -> send(player, data));
		else if (entity instanceof Player player) send(player, data);

		return null;
	}

	private void send(Player player, SpellData data) {
		if (useViewerAsTarget.get(data)) data = data.target(player);
		if (useViewerAsDefault.get(data)) data = data.recipient(player);

		Component textComponent = Util.getMiniMessage(text, data.recipient(), data);
		MagicSpells.getVolatileCodeHandler().sendToastEffect(player, icon, frame.get(data), textComponent);
	}

}
