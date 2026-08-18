package com.nisovin.magicspells.volatilecode;

import java.util.function.Supplier;

import org.bukkit.World;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.glow.GlowManager;

import io.papermc.paper.advancement.AdvancementDisplay.Frame;


public abstract class VolatileCodeHandle {

	protected final VolatileCodeHelper helper;

	public VolatileCodeHandle(VolatileCodeHelper helper) {
		this.helper = helper;
	}

	public abstract void addPotionGraphicalEffect(LivingEntity entity, int color, long duration);

	public abstract void sendFakeSlotUpdate(Player player, int slot, ItemStack item);

	public abstract boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire);

	public abstract void playDragonDeathEffect(Location location);

	public abstract void setClientVelocity(Player player, Vector velocity);

	public abstract void playHurtSound(LivingEntity entity);

	public abstract void sendToastEffect(Player receiver, ItemStack icon, Frame frameType, Component text);

	public abstract byte getEntityMetadata(Entity entity);

	public abstract Entity getEntityFromId(World world, int id);

	public abstract GlowManager getGlowManager();

	public abstract long countGlobalRegionSchedulerTasks();

	public abstract long countEntitySchedulerTasks();

	@Nullable
	public Object getVolatileParticleData(
		Particle particle,
		Supplier<Integer> geyserWaterBlocks,
		Supplier<Float> geyserBurstImpulse
	) {
		return null;
	}

	@Nullable
	public abstract String getCommandStorageString(Key containerId, String tagKey);

	@Nullable
	public abstract Double getCommandStorageDouble(Key containerId, String tagKey);

}
