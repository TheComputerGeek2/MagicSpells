package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;

public class ArmorStandEffect extends SpellEffect {

	public static final String ENTITY_TAG = "MS_ARMOR_STAND";

	private EntityData entityData;

	private String strMagicItem;

	private ItemStack headItem;

	private boolean gravity;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		ConfigurationSection section = config.getConfigurationSection("armorstand");
		if (section == null) return;

		entityData = new EntityData(section);
		entityData.setEntityType(EntityType.ARMOR_STAND);

		gravity = section.getBoolean("gravity", false);

		strMagicItem = section.getString("head", "");
		MagicItem magicItem = MagicItems.getMagicItemFromString(strMagicItem);
		if (magicItem != null) headItem = magicItem.getItemStack();
	}

	@Override
	protected ArmorStand playArmorStandEffectLocation(Location location) {
		ArmorStand armorStand = (ArmorStand) entityData.spawn(location);
		armorStand.addScoreboardTag(ENTITY_TAG);
		armorStand.setGravity(gravity);
		if (headItem != null) armorStand.setItem(EquipmentSlot.HEAD, headItem);
		return armorStand;
	}

}
