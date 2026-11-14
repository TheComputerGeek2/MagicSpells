package com.nisovin.magicspells.util.recipes.types;

import org.jetbrains.annotations.Nullable;

import org.jspecify.annotations.NullMarked;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.configuration.ConfigurationSection;

@NullMarked
public class SmithingRecipeFactory extends RecipeFactory {

	@Override
	@Nullable
	protected Recipe createRecipe(ConfigurationSection config, NamespacedKey key, ItemStack result) {
		RecipeChoice template = resolveRecipeChoice(config, "template");
		RecipeChoice base = resolveRecipeChoice(config, "base");
		RecipeChoice addition = resolveRecipeChoice(config, "addition");
		if (template == null || base == null || addition == null) return null;

		boolean copyNbt = config.getBoolean("copyNbt", true);

		return new SmithingTransformRecipe(key, result, template, base, addition, copyNbt);
	}

}
