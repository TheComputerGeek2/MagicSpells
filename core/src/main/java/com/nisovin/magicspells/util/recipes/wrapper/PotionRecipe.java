package com.nisovin.magicspells.util.recipes.wrapper;

import org.bukkit.Bukkit;

import io.papermc.paper.potion.PotionMix;

import org.jetbrains.annotations.NotNull;

public class PotionRecipe extends CustomRecipe {

	@NotNull
	private final PotionMix mix;

	public PotionRecipe(@NotNull PotionMix mix) {
		super(mix.getKey());
		this.mix = mix;
	}

	@Override
	public void add() {
		Bukkit.getPotionBrewer().addPotionMix(mix);
	}

	@Override
	public void remove() {
		Bukkit.getPotionBrewer().removePotionMix(getKey());
	}

}
