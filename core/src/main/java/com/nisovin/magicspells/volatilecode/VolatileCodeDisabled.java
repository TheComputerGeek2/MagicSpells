package com.nisovin.magicspells.volatilecode;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.inventory.Recipe;
import org.bukkit.util.Vector;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class VolatileCodeDisabled implements VolatileCodeHandle {

	public VolatileCodeDisabled() {
		
	}

	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration) {
		// Need the volatile code for this
	}

	@Override
	public void creaturePathToLoc(Creature creature, Location loc, float speed) {
		// Need the volatile code for this

	}

	@Override
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {
		// Need the volatile code for this
	}

	@Override
	public boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire) {
		return false;
	}

	@Override
	public boolean createExplosionByEntity(Entity entity, Location location, float size, boolean fire, boolean breakBlocks) {
		// Due to the way MagicSpells is set up, the new method introduced for this in 1.14 can't be used properly
		return location.getWorld().createExplosion(location, size, fire/*, entity*/);
	}

	@Override
	public void setExperienceBar(Player player, int level, float percent) {
		// Need the volatile code for this
	}

	@Override
	public void setTarget(LivingEntity entity, LivingEntity target) {
		if (entity instanceof Creature) ((Creature) entity).setTarget(target);
	}

	@Override
	public void setFallingBlockHurtEntities(FallingBlock block, float damage, int max) {
		block.setHurtEntities(true);
		// Need the (rest of) volatile code for this
	}

	@Override
	public void playDragonDeathEffect(Location location) {
		// Need the volatile code for this
	}

	@Override
	public void setKiller(LivingEntity entity, Player killer) {
		// Need the volatile code for this
	}

	@Override
	public void addAILookAtPlayer(LivingEntity entity, int range) {
		// Need the volatile code for this
	}

	@Override
	public void saveSkinData(Player player, String name) {
		// Need the volatile code for this
	}

	@Override
	public void setClientVelocity(Player player, Vector velocity) {
		// Need the volatile code for this
	}

	@Override
	public double getAbsorptionHearts(LivingEntity entity) {
		// Need the volatile code for this
		return 0;
	}

	@Override
	public void setAbsorptionHearts(LivingEntity entity, double amount) {
		// Need the volatile code for this
	}

	@Override
	public void setTexture(SkullMeta meta, String texture, String signature) {
		// Need volatile code for this
	}

	@Override
	public void setSkin(Player player, String skin, String signature) {
		// Need volatile code for this
	}

	@Override
	public void setTexture(SkullMeta meta, String texture, String signature, String uuid, OfflinePlayer offlinePlayer) {
		// Need volatile code for this
	}

	@Override
	public int getCustomModelData(ItemMeta meta) {
		return 0;
	}

	@Override
	public void setCustomModelData(ItemMeta meta, int data) {

	}

	@Override
	public ItemStack setNBTString(ItemStack item, String key, String value) {
		// Need volatile code for this
		return null;
	}

	@Override
	public String getNBTString(ItemStack item, String key) {
		// Need volatile code for this
		return null;
	}

	@Override
	public void setInventoryTitle(Player player, String title) {
		// Need volatile code for this
	}

	@Override
	public Recipe createCookingRecipe(String type, NamespacedKey namespaceKey, String group, ItemStack result, Material ingredient, float experience, int cookingTime) {
		// Need volatile code for this
		return null;
	}

	@Override
	public Recipe createStonecutterRecipe(NamespacedKey namespaceKey, String group, ItemStack result, Material ingredient) {
		// Need volatile code for this
		return null;
	}

	@Override
	public Recipe createSmithingRecipe(NamespacedKey namespaceKey, ItemStack result, Material base, Material addition) {
		// Need volatile code for this
		return null;
	}

}
