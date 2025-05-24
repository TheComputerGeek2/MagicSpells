package com.nisovin.magicspells.castmodifiers.conditions;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.RegistryAccess;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.castmodifiers.Condition;

@Name("biome")
public class BiomeCondition extends Condition {

	private final Set<Biome> biomes = new HashSet<>();

	@Override
	public boolean initialize(@NotNull String var) {
		if (var.isEmpty()) return false;

		Registry<Biome> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);

		for (String value : var.split(",")) {
			if (value.startsWith("#")) {
				NamespacedKey key = NamespacedKey.fromString(value.substring(1));
				if (key == null) return false;

				TagKey<Biome> tagKey = TagKey.create(RegistryKey.BIOME, key);
				if (!registry.hasTag(tagKey)) return false;

				Tag<Biome> tag = registry.getTag(tagKey);
				biomes.addAll(tag.resolve(registry));
				continue;
			}

			NamespacedKey key = NamespacedKey.fromString(value);
			if (key == null) return false;

			Biome biome = registry.get(key);
			if (biome == null) return false;

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
