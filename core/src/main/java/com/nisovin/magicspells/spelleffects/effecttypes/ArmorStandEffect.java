package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

@Name("armorstand")
public class ArmorStandEffect extends SpellEffect {

	public static final String ENTITY_TAG = "MS_ARMOR_STAND";

	private EntityData entityData;

	private boolean gravity;

	private String customName;
	private boolean customNameVisible;

	private ConfigData<Boolean> disableSlots;

	private ItemStack headItem;
	private ItemStack offHandItem;
	private ItemStack mainHandItem;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		ConfigurationSection section = config.getConfigurationSection("armorstand");
		if (section == null) return;

		entityData = new EntityData(section);

		gravity = section.getBoolean("gravity", false);

		customName = section.getString("custom-name", "");
		customNameVisible = section.getBoolean("custom-name-visible", false);

		disableSlots = ConfigDataUtil.getBoolean(section, "disable-slots", true);

		MagicItem item = MagicItems.getMagicItemFromString(section.getString("head"));
		if (item != null) headItem = item.getItemStack();

		item = MagicItems.getMagicItemFromString(section.getString("offhand"));
		if (item != null) offHandItem = item.getItemStack();

		item = MagicItems.getMagicItemFromString(section.getString("mainhand"));
		if (item != null) mainHandItem = item.getItemStack();
	}

	@Override
	protected ArmorStand playArmorStandEffectLocation(Location location, SpellData data) {
		return entityData.spawn(location, data, ArmorStand.class, stand -> {
			stand.setSilent(true);
			stand.addScoreboardTag(ENTITY_TAG);

			stand.setGravity(gravity);
			stand.setCustomNameVisible(customNameVisible);
			stand.customName(Util.getMiniMessage(customName, data));

			if (this.disableSlots.get(data)) stand.setDisabledSlots(EquipmentSlot.values());

			stand.setItem(EquipmentSlot.HEAD, headItem);
			stand.setItem(EquipmentSlot.HAND, mainHandItem);
			stand.setItem(EquipmentSlot.OFF_HAND, offHandItem);
		});
	}

}
