package com.nisovin.magicspells.util.recipes.types;

import org.jetbrains.annotations.Nullable;

import org.jspecify.annotations.NullMarked;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.configuration.ConfigurationSection;

@NullMarked
public class StonecuttingRecipeFactory extends RecipeFactory {

	@Override
	@Nullable
	protected Recipe createRecipe(ConfigurationSection config, NamespacedKey key, ItemStack result) {
		RecipeChoice ingredient = resolveRecipeChoice(config,"ingredient");
		if (ingredient == null) return null;

		return new StonecuttingRecipe(key, result, ingredient);
	}

}
