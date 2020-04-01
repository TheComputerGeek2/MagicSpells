package com.nisovin.magicjutsus.util;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Particle;

public class ParticleUtil {

	public enum ParticleEffect {

		EXPLOSION_NORMAL( "explosion_normal", "poof"),
		EXPLOSION_LARGE( "explosion_large", "explosion"),
		EXPLOSION_HUGE( "explosion_huge", "explosion_emitter"),
		FIREWORKS_SPARK( "fireworks_spark", "firework"),
		WATER_BUBBLE( "water_bubble", "bubble"),
		WATER_SPLASH( "water_splash", "splash"),
		WATER_WAKE( "water_wake", "fishing"),
		SUSPENDED( "suspended", "underwater"),
		SUSPENDED_DEPTH( "suspended_depth"),
		CRIT( "crit"),
		CRIT_MAGIC( "crit_magic", "enchanted_hit"),
		SMOKE_NORMAL( "smoke_normal", "smoke"),
		SMOKE_LARGE( "smoke_large", "large_smoke"),
		SPELL( "spell", "effect"),
		SPELL_INSTANT( "spell_instant", "instant_effect"),
		SPELL_MOB( "spell_mob", "entity_effect"),
		SPELL_MOB_AMBIENT( "spell_mob_ambient", "ambient_entity_effect"),
		SPELL_WITCH( "spell_witch", "witch"),
		DRIP_WATER( "drip_water", "dripping_water"),
		DRIP_LAVA( "drip_lava", "dripping_lava"),
		VILLAGER_ANGRY( "villager_angry", "angry_villager"),
		VILLAGER_HAPPY( "villager_happy", "happy_villager"),
		TOWN_AURA( "town_aura", "mycelium"),
		NOTE( "note"),
		PORTAL( "portal"),
		ENCHANTMENT_TABLE( "enchantment_table", "enchant"),
		FLAME( "flame"),
		LAVA( "lava"),
		CLOUD( "cloud"),
		REDSTONE( "redstone", "dust"),
		SNOWBALL( "snowball", "item_snowball"),
		SNOW_SHOVEL( "snow_shovel"),
		SLIME( "slime", "item_slime"),
		HEART( "heart"),
		BARRIER( "barrier"),
		ITEM_CRACK( "item_crack", "item"),
		BLOCK_CRACK( "block_crack", "blockcrack"),
		BLOCK_DUST( "block_dust", "blockdust", "block"),
		WATER_DROP( "water_drop", "rain"),
		MOB_APPEARANCE( "mob_appearance", "elder_guardian"),
		DRAGON_BREATH( "dragon_breath"),
		END_ROD( "end_rod"),
		DAMAGE_INDICATOR( "damage_indicator"),
		SWEEP_ATTACK( "sweep_attack"),
		FALLING_DUST( "falling_dust"),
		TOTEM( "totem", "totem_of_undying"),
		SPIT( "spit"),
		SQUID_INK( "squid_ink"),
		BUBBLE_POP( "bubble_pop"),
		CURRENT_DOWN( "current_down"),
		BUBBLE_COLUMN_UP( "bubble_column_up"),
		NAUTILUS( "nautilus"),
		DOLPHIN( "dolphin"),
		/*SNEEZE( "sneeze"),
		CAMPFIRE_COSY_SMOKE( "campfire_cosy_smoke"),
		CAMPFIRE_SIGNAL_SMOKE( "campfire_signal_smoke"),
		COMPOSTER( "composter"),
		FLASH( "flash"),
		FALLING_LAVA( "falling_lava"),
		LANDING_LAVA( "landing_lava"),
		FALLING_WATER( "falling_water"),
		DRIPPING_HONEY( "dripping_honey"),
		FALLING_HONEY( "falling_honey"),
		LANDING_HONEY( "landing_honey"),
		FALLING_NECTAR( "falling_nectar"), */

		;

		private final String[] names;

		ParticleEffect(String... names) {
			this.names = names;
		}

		private static Map<String, Particle> namesToType = new HashMap<>();
		private static boolean initialized = false;

		private static void initialize() {
			if (initialized) return;

			for (ParticleEffect pe : ParticleEffect.values()) {
				Particle particle = Particle.valueOf(pe.name());
				if (particle == null) continue;

				// handle the names
				namesToType.put(pe.name().toLowerCase(), particle);
				for (String s : pe.names) {
					namesToType.put(s.toLowerCase(), particle);
				}

			}

			initialized = true;
		}

		public static Particle getParticle(String particleName) {
			initialize();

			Particle particle = namesToType.get(particleName.toLowerCase());
			if (particle != null) return particle;

			try {
				particle = Particle.valueOf(particleName.toUpperCase());
			} catch (IllegalArgumentException ex) {
				particle = null;
			}

			return particle;
		}

	}

}
