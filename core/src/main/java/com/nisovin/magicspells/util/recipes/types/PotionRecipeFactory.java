package com.nisovin.magicspells.util.recipes.types;

import org.jetbrains.annotations.Nullable;

import io.papermc.paper.potion.PotionMix;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.recipes.wrapper.PotionRecipe;

public class PotionRecipeFactory extends RecipeFactory<PotionRecipe> {

	@Override
	@Nullable
	protected PotionRecipe createRecipe(ConfigurationSection config, NamespacedKey key, ItemStack result) {
		RecipeChoice input = resolveRecipeChoice(config, "input");
		RecipeChoice ingredient = resolveRecipeChoice(config, "ingredient");
		if (input == null || ingredient == null) return null;

		return new PotionRecipe(new PotionMix(key, result, input, ingredient));
	}

}
