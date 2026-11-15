package com.nisovin.magicspells.handlers;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectHandler {

	private static final Map<String, PotionEffectType> potionEffects = new HashMap<>();

	static {
		// Add bukkit and minecraft potion effect names

		// Add legacy values
		potionEffects.put("slow", PotionEffectType.SLOWNESS);
		potionEffects.put("fast_digging", PotionEffectType.HASTE);
		potionEffects.put("slow_digging", PotionEffectType.MINING_FATIGUE);
		potionEffects.put("increase_damage", PotionEffectType.STRENGTH);
		potionEffects.put("heal", PotionEffectType.INSTANT_HEALTH);
		potionEffects.put("harm", PotionEffectType.INSTANT_DAMAGE);
		potionEffects.put("jump", PotionEffectType.JUMP_BOOST);
		potionEffects.put("confusion", PotionEffectType.NAUSEA);
		potionEffects.put("damage_resistance", PotionEffectType.RESISTANCE);

		for (PotionEffectType potionEffect : Registry.MOB_EFFECT) {
			potionEffects.put(potionEffect.key().value(), potionEffect);
		}
	}

	public static PotionEffectType getPotionEffectType(String identification) {
		return potionEffects.get(identification.trim().toLowerCase());
	}

}
