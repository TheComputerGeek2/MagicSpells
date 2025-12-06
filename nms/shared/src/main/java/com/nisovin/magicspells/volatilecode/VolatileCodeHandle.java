package com.nisovin.magicspells.volatilecode;

import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

import io.papermc.paper.advancement.AdvancementDisplay.Frame;

import com.nisovin.magicspells.util.glow.GlowManager;

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

}
