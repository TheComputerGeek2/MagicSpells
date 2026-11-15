package com.nisovin.magicspells.util.recipes;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.recipes.wrapper.*;

public class CustomRecipes {

	private static final Map<NamespacedKey, Recipe> RECIPES = new HashMap<>();
	private static final List<NamespacedKey> AUTO_DISCOVER = new ArrayList<>();

	public static void load(ConfigurationSection recipesSection) {
		for (String recipeKey : recipesSection.getKeys(false)) {
			ConfigurationSection config = recipesSection.getConfigurationSection(recipeKey);
			if (config == null) {
				MagicSpells.error("Recipe '" + recipeKey + "' is not a configuration section.");
				continue;
			}

			String typeName = config.getString("type", "none");
			RecipeType type = Util.enumValueSafe(RecipeType.class, typeName);
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

			RECIPES.put(recipe.key(), recipe.recipe());

			if (!recipe.autoDiscover()) continue;
			AUTO_DISCOVER.add(recipe.key());
		}

		if (RECIPES.isEmpty()) return;
		Bukkit.addRecipes(RECIPES.values());

		if (AUTO_DISCOVER.isEmpty()) return;
		RecipeDiscoverListener.suppressToastWhile(() -> {
			for (Player player : Bukkit.getOnlinePlayers())
				player.discoverRecipes(AUTO_DISCOVER);
		});
	}

	public static void clearRecipes() {
		if (!AUTO_DISCOVER.isEmpty())
			for (Player player : Bukkit.getOnlinePlayers())
				player.undiscoverRecipes(AUTO_DISCOVER);

		Bukkit.removeRecipes(RECIPES.keySet());

		RECIPES.clear();
		AUTO_DISCOVER.clear();
	}

	public static Map<NamespacedKey, Recipe> getRecipes() {
		return RECIPES;
	}

	public static List<NamespacedKey> getAutoDiscover() {
		return AUTO_DISCOVER;
	}

}
