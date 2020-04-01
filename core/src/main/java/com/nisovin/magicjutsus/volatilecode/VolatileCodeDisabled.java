package com.nisovin.magicjutsus.volatilecode;

import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
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
		// Due to the way MagicJutsus is set up, the new method introduced for this in 1.14 can't be used properly
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
	public void setTexture(SkullMeta meta, String texture, String signature) {
		// Need volatile code for this
	}

	@Override
	public void setSkin(Player player, String skin, String signature) {
		// Need volatile code for this
	}

	@Override
	public void setTexture(SkullMeta meta, String texture, String signature, String uuid, String name) {
		// Need volatile code for this
	}

	@Override
	public int getCustomModelData(ItemMeta meta) {
		return 0;
	}

	@Override
	public void setCustomModelData(ItemMeta meta, int data) {

	}

}
