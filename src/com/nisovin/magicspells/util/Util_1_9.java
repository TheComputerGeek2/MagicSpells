package com.nisovin.magicspells.util;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Particle;

public class Util_1_9 {

	static Map<String, Particle> particleMap = new HashMap<>();
	
	public static Particle getParticleFromName(String name) {
		for (Particle p : Particle.values()) {
			if (p.name().equalsIgnoreCase(name)) return p;
		}
		return particleMap.get(name.toLowerCase());
	}
	
	static {
		particleMap.put("explode", Particle.EXPLOSION_NORMAL);
		particleMap.put("largeexplode", Particle.EXPLOSION_LARGE);
		particleMap.put("hugeexplosion", Particle.EXPLOSION_HUGE);
		particleMap.put("fireworksspark", Particle.FIREWORKS_SPARK);
		particleMap.put("bubble", Particle.WATER_BUBBLE);
		particleMap.put("splash", Particle.WATER_SPLASH);
		particleMap.put("wake", Particle.WATER_WAKE);
		particleMap.put("suspended", Particle.SUSPENDED);
		particleMap.put("depthsuspend", Particle.SUSPENDED_DEPTH);
		particleMap.put("crit", Particle.CRIT);
		particleMap.put("magiccrit", Particle.CRIT_MAGIC);
		particleMap.put("smoke", Particle.SMOKE_NORMAL);
		particleMap.put("largesmoke", Particle.SMOKE_LARGE);
		particleMap.put("spell", Particle.SPELL);
		particleMap.put("instantspell", Particle.SPELL_INSTANT);
		particleMap.put("mobspell", Particle.SPELL_MOB);
		particleMap.put("mobspellambient", Particle.SPELL_MOB_AMBIENT);
		particleMap.put("witchmagic", Particle.SPELL_WITCH);
		particleMap.put("dripwater", Particle.DRIP_WATER);
		particleMap.put("driplava", Particle.DRIP_LAVA);
		particleMap.put("angryvillager", Particle.VILLAGER_ANGRY);
		particleMap.put("happyvillager", Particle.VILLAGER_HAPPY);
		particleMap.put("townaura", Particle.TOWN_AURA);
		particleMap.put("note", Particle.NOTE);
		particleMap.put("portal", Particle.PORTAL);
		particleMap.put("enchantmenttable", Particle.ENCHANTMENT_TABLE);
		particleMap.put("flame", Particle.FLAME);
		particleMap.put("lava", Particle.LAVA);
		particleMap.put("reddust", Particle.REDSTONE);
		particleMap.put("snowballpoof", Particle.SNOWBALL);
		particleMap.put("slime", Particle.SLIME);
		particleMap.put("heart", Particle.HEART);
		particleMap.put("barrier", Particle.BARRIER);
		particleMap.put("cloud", Particle.CLOUD);
		particleMap.put("snowshovel", Particle.SNOW_SHOVEL);
		particleMap.put("droplet", Particle.WATER_DROP);
		particleMap.put("mobappearance", Particle.MOB_APPEARANCE);
		particleMap.put("endrod", Particle.END_ROD);
		particleMap.put("dragonbreath", Particle.DRAGON_BREATH);
		particleMap.put("damageindicator", Particle.DAMAGE_INDICATOR);
		particleMap.put("sweepattack", Particle.SWEEP_ATTACK);
	}
	
}
