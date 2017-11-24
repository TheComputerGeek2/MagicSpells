package com.nisovin.magicspells.castmodifiers.conditions;

import com.nisovin.magicspells.util.Util;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

import java.util.EnumSet;

public class BiomeCondition extends Condition {
	
	private EnumSet<Biome> biomes = EnumSet.noneOf(Biome.class);

	@Override
	public boolean setVar(String var) {
		String[] s = var.split(",");
		for (int i = 0; i < s.length; i++) {
			
			// Get the biome
			Biome biome = Util.enumValueSafe(Biome.class, s[i].toUpperCase());
			
			// Is it null?
			if (biome == null) {
				// Rip...
				DebugHandler.debugBadEnumValue(Biome.class, s[i].toUpperCase());
				
				// NEXT!
				continue;
			}
			
			// Add to the collection
			biomes.add(biome);
		}
		return true;
	}

	@Override
	public boolean check(Player player) {
		return check(player, player);
	}
	
	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(player, target.getLocation());
	}
	
	@Override
	public boolean check(Player player, Location location) {
		return biomes.contains(location.getBlock().getBiome());
	}
	
}
