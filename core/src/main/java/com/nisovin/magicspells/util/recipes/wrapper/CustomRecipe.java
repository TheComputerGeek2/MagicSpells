package com.nisovin.magicspells.util.recipes.wrapper;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

import org.jetbrains.annotations.NotNull;

public class CustomRecipe {

	@NotNull
	private final NamespacedKey key;
	@NotNull
	private final Recipe recipe;

	public CustomRecipe(@NotNull NamespacedKey key, @NotNull Recipe recipe) {
		this.key = key;
		this.recipe = recipe;
	}

	@NotNull
	public NamespacedKey getKey() {
		return key;
	}

	public void add() {
		Bukkit.addRecipe(recipe);
	}

	public void remove() {
		Bukkit.removeRecipe(getKey());
	}

}
