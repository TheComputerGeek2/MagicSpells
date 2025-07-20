package com.nisovin.magicspells.util.recipes.types;

import org.jetbrains.annotations.NotNull;

import org.bukkit.inventory.*;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.recipe.CookingBookCategory;

public abstract class CookingRecipeFactory<R extends CookingRecipe<R>> extends RecipeFactory {

	@Override
	protected final Recipe createCrafting(ConfigurationSection config, NamespacedKey key, ItemStack result) {
		int cookingTime = config.getInt("cooking-time", 0);
		RecipeChoice ingredient = resolveRecipeChoice(config, "ingredient");
		if (ingredient == null) return null;
		float experience = (float) config.getDouble("experience", 0);

		R recipe = createCooking(new CookingData(key, result, ingredient, experience, cookingTime));

		String group = config.getString("group", "");
		if (!group.isBlank()) recipe.setGroup(group);

		CookingBookCategory category = resolveEnum(config, "category", CookingBookCategory.class);
		if (category != null) recipe.setCategory(category);

		return recipe;
	}

	@NotNull
	protected abstract R createCooking(CookingData data);

	protected record CookingData(
		@NotNull NamespacedKey key,
		@NotNull ItemStack result,
		@NotNull RecipeChoice ingredient,
		float experience,
		int cookingTime
	) {}

	public static class Blasting extends CookingRecipeFactory<BlastingRecipe> {

		@Override
		@NotNull
		protected BlastingRecipe createCooking(CookingData data) {
			return new BlastingRecipe(data.key(), data.result(), data.ingredient(), data.experience(), data.cookingTime());
		}

	}

	public static class Campfire extends CookingRecipeFactory<CampfireRecipe> {

		@Override
		@NotNull
		protected CampfireRecipe createCooking(CookingData data) {
			return new CampfireRecipe(data.key(), data.result(), data.ingredient(), data.experience(), data.cookingTime());
		}

	}

	public static class Furnace extends CookingRecipeFactory<FurnaceRecipe> {

		@Override
		@NotNull
		protected FurnaceRecipe createCooking(CookingData data) {
			return new FurnaceRecipe(data.key(), data.result(), data.ingredient(), data.experience(), data.cookingTime());
		}

	}

	public static class Smoking extends CookingRecipeFactory<SmokingRecipe> {

		@Override
		@NotNull
		protected SmokingRecipe createCooking(CookingData data) {
			return new SmokingRecipe(data.key(), data.result(), data.ingredient(), data.experience(), data.cookingTime());
		}

	}

}
