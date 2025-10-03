package com.nisovin.magicspells.util.recipes.wrapper;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

import org.jetbrains.annotations.NotNull;

public class CraftingRecipe extends CustomRecipe {

	@NotNull
	private final Recipe recipe;
	private final boolean autoDiscover;

	public CraftingRecipe(@NotNull Recipe recipe, NamespacedKey key, boolean autoDiscover) {
		super(key);
		this.recipe = recipe;
		this.autoDiscover = autoDiscover;
	}

	public boolean isAutoDiscover() {
		return autoDiscover;
	}

	@Override
	public void add() {
		Bukkit.addRecipe(recipe);
	}

	@Override
	public void remove() {
		Bukkit.removeRecipe(getKey());
	}

}
