package com.nisovin.magicspells.util.recipes.wrapper;

import org.bukkit.NamespacedKey;

import org.jetbrains.annotations.NotNull;

public abstract class CustomRecipe {

	@NotNull
	private final NamespacedKey key;

	public CustomRecipe(@NotNull NamespacedKey key) {
		this.key = key;
	}

	@NotNull
	public NamespacedKey getKey() {
		return key;
	}

	public abstract void add();

	public abstract void remove();

}
