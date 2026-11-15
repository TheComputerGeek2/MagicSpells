package com.nisovin.magicspells.util.projectile;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

public class ProjectileManagerSplashPotion extends ProjectileManagerThrownPotion {

	@NotNull
	@Override
	protected ItemStack getItem() {
		return ItemStack.of(Material.SPLASH_POTION);
	}

}
