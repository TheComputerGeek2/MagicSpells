package com.nisovin.magicspells.util.recipes;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.recipes.types.*;
import com.nisovin.magicspells.util.recipes.wrapper.CustomRecipe;

public enum RecipeType {

	FURNACE(new CookingRecipeFactory.Furnace()),
	SMOKING(new CookingRecipeFactory.Smoking()),
	CAMPFIRE(new CookingRecipeFactory.Campfire()),
	BLASTING(new CookingRecipeFactory.Blasting()),
	STONECUTTING(new StonecuttingRecipeFactory()),
	SMITHING(new SmithingRecipeFactory()),
	SHAPELESS(new ShapelessRecipeFactory()),
	SHAPED(new ShapedRecipeFactory()),

	;

	private final RecipeFactory<? extends CustomRecipe> factory;

	RecipeType(RecipeFactory<? extends CustomRecipe> factory) {
		this.factory = factory;
	}

	public CustomRecipe create(ConfigurationSection config) {
		return factory.create(config);
	}

}
