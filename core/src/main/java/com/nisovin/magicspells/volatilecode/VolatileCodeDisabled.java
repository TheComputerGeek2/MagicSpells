package com.nisovin.magicspells.volatilecode;

import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

import io.papermc.paper.advancement.AdvancementDisplay.Frame;

public class VolatileCodeDisabled extends VolatileCodeHandle {

	public VolatileCodeDisabled() {
		super(null);
	}

	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, long duration) {

	}

	@Override
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {

	}

	@Override
	public boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire) {
		return false;
	}

	@Override
	public void playDragonDeathEffect(Location location) {

	}

	@Override
	public void setClientVelocity(Player player, Vector velocity) {

	}

	@Override
	public void playHurtSound(LivingEntity entity) {
		Sound sound = entity.getHurtSound();
		if (sound == null) return;
		entity.getWorld().playSound(entity.getLocation(), sound, 1, 1);
	}

	@Override
	public void sendToastEffect(Player receiver, ItemStack icon, Frame frameType, Component text) {

	}

	@Override
	public void addGameTestMarker(Player player, Location location, int color, String name, int lifetime) {

	}

	@Override
	public void clearGameTestMarkers(Player player) {

	}

}
