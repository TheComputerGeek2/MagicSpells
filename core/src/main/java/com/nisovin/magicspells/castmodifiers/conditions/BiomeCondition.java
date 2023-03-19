package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class BiomeCondition extends Condition {
	
	private final EnumSet<Biome> biomes = EnumSet.noneOf(Biome.class);

	@Override
	public boolean initialize(String var) {
		String[] s = var.split(",");

		for (String value : s) {
			Biome biome = Util.enumValueSafe(Biome.class, value.toUpperCase());
			if (biome == null) {
				DebugHandler.debugBadEnumValue(Biome.class, value.toUpperCase());
				continue;
			}
			biomes.add(biome);
		}
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return biome(caster.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return biome(target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return biome(location);
	}

	private boolean biome(Location location) {
		return biomes.contains(location.getBlock().getBiome());
	}
	
}
