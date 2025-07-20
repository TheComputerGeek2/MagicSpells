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

	public static void load(ConfigurationSection recipesSection) {
		List<NamespacedKey> discover = new ArrayList<>();

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

			CustomRecipe customRecipe = null;
			try {
				customRecipe = type.create(config);
			} catch (Exception e) {
				MagicSpells.error("Encountered error while loading recipe '%s'.".formatted(recipeKey));
				e.printStackTrace();
			}
			if (customRecipe == null) continue;

			RECIPES.add(customRecipe);
			customRecipe.add();
			discover.add(customRecipe.getKey());
		}

		if (RECIPES.isEmpty()) return;
		Bukkit.updateRecipes();

		if (!discover.isEmpty())
			for (Player player : Bukkit.getOnlinePlayers())
				player.discoverRecipes(discover);
	}

	public static void clearRecipes() {
		List<NamespacedKey> undiscover = new ArrayList<>();
		for (CustomRecipe recipe : RECIPES) {
			recipe.remove();
			undiscover.add(recipe.getKey());
		}

		if (!undiscover.isEmpty())
			for (Player player : Bukkit.getOnlinePlayers())
				player.undiscoverRecipes(undiscover);

		RECIPES.clear();
		Bukkit.updateRecipes();
	}

	public static List<CustomRecipe> getRecipes() {
		return RECIPES;
	}

}
