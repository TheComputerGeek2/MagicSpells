package com.nisovin.magicspells.util.recipes;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.recipes.wrapper.*;

public class CustomRecipes {

	private static final List<CustomRecipe> RECIPES = new ArrayList<>();
	private static final List<NamespacedKey> AUTO_DISCOVER = new ArrayList<>();

	public static void load(ConfigurationSection recipesSection) {
		for (String recipeKey : recipesSection.getKeys(false)) {
			ConfigurationSection config = recipesSection.getConfigurationSection(recipeKey);
			if (config == null) {
				MagicSpells.error("Recipe '" + recipeKey + "' is not a configuration section.");
				continue;
			}

			String typeName = config.getString("type", "none");
			RecipeType type = Util.enumValueSafe(RecipeType.class, typeName.toUpperCase());
			if (type == null) {
				MagicSpells.error("Recipe '%s' has an invalid 'type' defined: %s".formatted(recipeKey, typeName));
				continue;
			}

			CustomRecipe recipe = null;
			try {
				recipe = type.create(config);
			} catch (Exception e) {
				MagicSpells.error("Encountered error while loading recipe '%s'.".formatted(recipeKey));
				e.printStackTrace();
			}
			if (recipe == null) continue;

			RECIPES.add(recipe);
			recipe.add();

			if (!(recipe instanceof CraftingRecipe crafting) || !crafting.isAutoDiscover()) continue;
			AUTO_DISCOVER.add(crafting.getKey());
		}

		if (RECIPES.isEmpty()) return;
		Bukkit.updateRecipes();

		if (!AUTO_DISCOVER.isEmpty())
			for (Player player : Bukkit.getOnlinePlayers())
				player.discoverRecipes(AUTO_DISCOVER);
	}

	public static void clearRecipes() {
		RECIPES.forEach(CustomRecipe::remove);

		if (!AUTO_DISCOVER.isEmpty())
			for (Player player : Bukkit.getOnlinePlayers())
				player.undiscoverRecipes(AUTO_DISCOVER);

		RECIPES.clear();
		AUTO_DISCOVER.clear();
		Bukkit.updateRecipes();
	}

	public static List<CustomRecipe> getRecipes() {
		return RECIPES;
	}

	public static List<NamespacedKey> getAutoDiscover() {
		return AUTO_DISCOVER;
	}

}
