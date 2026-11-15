package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

@Name("startusingitem")
public class StartUsingItemEffect extends SpellEffect {

	private ConfigData<HandType> hand;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		hand = ConfigDataUtil.getEnum(config, "hand", HandType.class, HandType.MAINHAND);
	}

	@Override
	protected Runnable playEffectEntity(Entity entity, SpellData data) {
		if (entity instanceof LivingEntity livingEntity)
			livingEntity.startUsingItem(hand.get(data).slot);

		return null;
	}

	private enum HandType {

		MAINHAND(EquipmentSlot.HAND),
		OFFHAND(EquipmentSlot.OFF_HAND);

		private final EquipmentSlot slot;

		HandType(EquipmentSlot slot) {
			this.slot = slot;
		}
	}

}
