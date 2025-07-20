package com.nisovin.magicspells.util.recipes.types;

import java.util.List;
import java.util.ArrayList;

import org.jetbrains.annotations.Nullable;

import com.nisovin.magicspells.MagicSpells;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.recipe.CraftingBookCategory;

public class ShapelessRecipeFactory extends CraftingRecipeFactory {

	@Override
	@Nullable
	protected Recipe createCrafting(ConfigurationSection config, NamespacedKey key, ItemStack result) {
		ShapelessRecipe recipe = new ShapelessRecipe(key, result);

		String path = "ingredients";
		ConfigurationSection ingredients;
		if (config.isList(path)) {
			// Backward compatibility:
			List<?> ingredientList = config.getList(path, new ArrayList<>());
			ingredients = config.createSection(path);
			for (int i = 0; i < ingredientList.size(); i++) {
				ingredients.set(i + "", ingredientList.get(i));
			}
		}
		else ingredients = config.getConfigurationSection(path);
		if (ingredients == null) {
			MagicSpells.error("No ingredients defined for custom shapeless recipe '%s'.".formatted(config.getName()));
			return null;
		}

		for (String ingredient : ingredients.getKeys(false)) {
			RecipeChoice choice = resolveRecipeChoice(config, path + "." + ingredient);
			if (choice == null) return null;
			recipe.addIngredient(choice);
		}

		String group = config.getString("group", "");
		if (!group.isBlank()) recipe.setGroup(group);

		CraftingBookCategory category = resolveEnum(config, "category", CraftingBookCategory.class);
		if (category != null) recipe.setCategory(category);

		return recipe;
	}

}
