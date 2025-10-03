package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.castmodifiers.Condition;

@Name("hasrecipe")
public class HasRecipeCondition extends Condition {

	private final List<NamespacedKey> recipes = new ArrayList<>();

	@Override
	public boolean initialize(@NotNull String var) {
		if (var.isEmpty()) return false;

		for (String tagString : var.split(",")) {
			NamespacedKey key = NamespacedKey.fromString(tagString);
			if (key == null) return false;
			recipes.add(key);
		}

		return !recipes.isEmpty();
	}

	@Override
	public boolean check(LivingEntity caster) {
		return hasRecipes(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return hasRecipes(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean hasRecipes(LivingEntity entity) {
		if (!(entity instanceof Player player)) return false;

		for (NamespacedKey recipe : recipes)
			if (player.hasDiscoveredRecipe(recipe))
				return true;

		return false;
	}

}
