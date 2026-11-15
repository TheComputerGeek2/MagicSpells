package com.nisovin.magicspells.util.itemreader;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.potion.PotionEffect;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.potion.SuspiciousEffectEntry;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.POTION_EFFECTS;

public class SuspiciousStewHandler {

	private static final String CONFIG_NAME = POTION_EFFECTS.toString();

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof SuspiciousStewMeta stewMeta)) return;
		if (!config.isList(CONFIG_NAME)) return;

		stewMeta.clearCustomEffects();

		List<String> effects = config.getStringList(CONFIG_NAME);
		List<PotionEffect> potionEffects = new ArrayList<>();
		for (String str : effects) {
			SuspiciousEffectEntry stewEffect = Util.buildSuspiciousStewPotionEffect(str);
			if (stewEffect == null) continue;

			stewMeta.addCustomEffect(stewEffect, true);
			potionEffects.add(new PotionEffect(stewEffect.effect(), stewEffect.duration(), 0));
		}
		if (!potionEffects.isEmpty()) data.setAttribute(POTION_EFFECTS, potionEffects);
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof SuspiciousStewMeta stewMeta)) return;
		if (!data.hasAttribute(POTION_EFFECTS)) return;
		stewMeta.clearCustomEffects();
		((List<PotionEffect>) data.getAttribute(POTION_EFFECTS)).forEach(effect ->
			stewMeta.addCustomEffect(SuspiciousEffectEntry.create(effect.getType(), effect.getDuration()), true)
		);
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof SuspiciousStewMeta stewMeta)) return;
		if (!stewMeta.hasCustomEffects()) return;
		List<PotionEffect> effects = stewMeta.getCustomEffects();
		if (!effects.isEmpty()) data.setAttribute(POTION_EFFECTS, effects);
	}

}
