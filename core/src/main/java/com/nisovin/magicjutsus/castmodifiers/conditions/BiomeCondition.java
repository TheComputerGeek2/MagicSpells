package com.nisovin.magicjutsus.castmodifiers.conditions;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class BiomeCondition extends Condition {
	
	private EnumSet<Biome> biomes = EnumSet.noneOf(Biome.class);

	@Override
	public boolean setVar(String var) {
		String[] s = var.split(",");

		for (int i = 0; i < s.length; i++) {
			Biome biome = Util.enumValueSafe(Biome.class, s[i].toUpperCase());

			if (biome == null) {
				DebugHandler.debugBadEnumValue(Biome.class, s[i].toUpperCase());
				continue;
			}

			biomes.add(biome);
		}
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(livingEntity, target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return biomes.contains(location.getBlock().getBiome());
	}
	
}
