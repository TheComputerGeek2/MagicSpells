package com.nisovin.magicspells.util.projectile;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectileManagerThrownPotion extends ProjectileManager {

	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return ThrownPotion.class;
	}

	@NotNull
	protected ItemStack getItem() {
		return new ItemStack(Material.POTION);
	}

	@NotNull
	public final ItemStack getPotion(@Nullable Color color) {
		ItemStack potion = getItem();
		if (color != null) potion.editMeta(PotionMeta.class, p -> p.setColor(color));
		return potion;
	}

}
