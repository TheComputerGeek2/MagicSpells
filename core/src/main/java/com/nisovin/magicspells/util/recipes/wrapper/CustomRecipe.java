package com.nisovin.magicspells.util.recipes.wrapper;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record CustomRecipe(Recipe recipe, NamespacedKey key, boolean autoDiscover) {
}
