package com.nisovin.magicspells.util.recipes.types;

import org.jetbrains.annotations.Nullable;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.recipes.wrapper.CraftingRecipe;

public abstract class CraftingRecipeFactory extends RecipeFactory<CraftingRecipe> {

	@Override
	@Nullable
	protected final CraftingRecipe createRecipe(ConfigurationSection config, NamespacedKey key, ItemStack result) {
		boolean autoDiscover = config.getBoolean("auto-discover", true);

		Recipe recipe = createCrafting(config, key, result);
		if (recipe == null) return null;

		return new CraftingRecipe(recipe, key, autoDiscover);
	}

	@Nullable
	protected abstract Recipe createCrafting(ConfigurationSection config, NamespacedKey key, ItemStack result);

}
