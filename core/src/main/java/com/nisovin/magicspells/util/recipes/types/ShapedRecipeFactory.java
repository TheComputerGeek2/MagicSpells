package com.nisovin.magicspells.util.recipes.types;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.nisovin.magicspells.MagicSpells;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.recipe.CraftingBookCategory;

public class ShapedRecipeFactory extends CraftingRecipeFactory {

	@Override
	@Nullable
	protected Recipe createCrafting(ConfigurationSection config, NamespacedKey key, ItemStack result) {
		ShapedRecipe recipe = new ShapedRecipe(key, result);

		List<String> shape = config.getStringList("shape");
		recipe.shape(shape.toArray(new String[0]));

		ConfigurationSection ingredients = config.getConfigurationSection("ingredients");
		if (ingredients == null) {
			MagicSpells.error("No ingredients defined for custom shaped recipe '%s'.".formatted(config.getName()));
			return null;
		}

		for (String ingStr : ingredients.getKeys(false)) {
			if (ingStr.length() != 1) {
				MagicSpells.error("Invalid ingredient key '%s' on custom shaped recipe '%s' - keys should a single character.".formatted(key, config.getName()));
				return null;
			}
			char ingredient = ingStr.charAt(0);

			RecipeChoice choice = resolveRecipeChoice(config, "ingredients." + ingredient);
			if (choice == null) return null;
			recipe.setIngredient(ingredient, choice);
		}

		String group = config.getString("group", "");
		if (!group.isBlank()) recipe.setGroup(group);

		CraftingBookCategory category = resolveEnum(config, "category", CraftingBookCategory.class);
		if (category != null) recipe.setCategory(category);

		return recipe;
	}

}
