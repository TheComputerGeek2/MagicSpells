package com.nisovin.magicspells.util;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class MobUtil {

	private static final Map<EntityType, Material> entityToEggMaterial = new HashMap<>();
	private static final Map<String, EntityType> entityTypeMap = new HashMap<>();

	static {
		for (EntityType type : EntityType.values()) {
			if (type == EntityType.UNKNOWN) continue;

			entityTypeMap.put(type.name().toLowerCase(), type);
			entityTypeMap.put(type.name().toLowerCase().replace("_", ""), type);

			entityTypeMap.put(type.getKey().getKey(), type);
			entityTypeMap.put(type.getKey().getKey().replace("_", ""), type);

			Material material = Util.getMaterial(type.getKey().getKey() + "_SPAWN_EGG");
			if (material != null) entityToEggMaterial.put(type, material);
		}

		Map<String, EntityType> types = new HashMap<>();
		for (Map.Entry<String, EntityType> entry : entityTypeMap.entrySet()) {
			types.put(entry.getKey() + 's', entry.getValue());
		}
		entityTypeMap.putAll(types);
	}

	public static EntityType getEntityType(String type) {
		if (type.equalsIgnoreCase("player")) return EntityType.PLAYER;
		return entityTypeMap.get(type.toLowerCase());
	}

	public static ItemStack getEggItemForEntityType(EntityType type) {
		Material eggMaterial = entityToEggMaterial.get(type);
		if (eggMaterial == null) return null;

		return new ItemStack(eggMaterial);
	}

	public static boolean hasEggMaterialForEntityType(EntityType type) {
		return entityToEggMaterial.containsKey(type);
	}

	public static void setTarget(LivingEntity mob, LivingEntity target, EntityTargetEvent.TargetReason reason) {
		EntityTargetEvent e = new EntityTargetEvent(mob, target, reason);
        Bukkit.getPluginManager().callEvent(e);
	}

	public static void setTarget(LivingEntity mob, LivingEntity target) {
		if (mob instanceof Mob m) m.setTarget(target);
	}

	public static EntityType getPigZombieEntityType() {
		try {
			return EntityType.valueOf("ZOMBIFIED_PIGLIN");
		} catch (IllegalArgumentException ex) {
			return EntityType.valueOf("PIG_ZOMBIE");
		}
	}

}
