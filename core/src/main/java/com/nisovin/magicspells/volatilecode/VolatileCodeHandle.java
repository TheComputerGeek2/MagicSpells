package com.nisovin.magicspells.volatilecode;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.inventory.Recipe;
import org.bukkit.util.Vector;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public interface VolatileCodeHandle {
	
	void addPotionGraphicalEffect(LivingEntity entity, int color, int duration);

	void creaturePathToLoc(Creature creature, Location loc, float speed);
	
	void sendFakeSlotUpdate(Player player, int slot, ItemStack item);

	boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire);
	
	boolean createExplosionByEntity(Entity entity, Location location, float size, boolean fire, boolean breakBlocks);

	void setExperienceBar(Player player, int level, float percent);

	void setTarget(LivingEntity entity, LivingEntity target);

	void setFallingBlockHurtEntities(FallingBlock block, float damage, int max);

	void setKiller(LivingEntity entity, Player killer);

	void playDragonDeathEffect(Location location);

	void addAILookAtPlayer(LivingEntity entity, int range);
	
	void saveSkinData(Player player, String name);

	void setClientVelocity(Player player, Vector velocity);
	
	double getAbsorptionHearts(LivingEntity entity);

	void setAbsorptionHearts(LivingEntity entity, double Double);

	void setTexture(SkullMeta meta, String texture, String signature);

	void setTexture(SkullMeta meta, String texture, String signature, String uuid, OfflinePlayer offlinePlayer);

	void setSkin(Player player, String skin, String signature);

	int getCustomModelData(ItemMeta meta);

	void setCustomModelData(ItemMeta meta, int data);

	ItemStack setNBTString(ItemStack item, String key, String value);

	String getNBTString(ItemStack item, String key);

	void setInventoryTitle(Player player, String title);

	Recipe createCookingRecipe(String type, NamespacedKey namespaceKey, String group, ItemStack result, Material ingredient, float experience, int cookingTime);

	Recipe createStonecutterRecipe(NamespacedKey namespaceKey, String group, ItemStack result, Material ingredient);

	Recipe createSmithingRecipe(NamespacedKey namespaceKey, ItemStack result, Material base, Material addition);

}
