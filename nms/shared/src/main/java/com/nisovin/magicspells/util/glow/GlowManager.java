package com.nisovin.magicspells.util.glow;

import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface GlowManager {

	void load();

	void unload();

	void applyGlow(@NotNull Entity entity, @NotNull NamespacedKey key, @NotNull NamedTextColor color, int priority, @Range(from = 0, to = Integer.MAX_VALUE) int duration);

	void applyGlow(@NotNull Player player, @NotNull Entity entity, @NotNull NamespacedKey key, @NotNull NamedTextColor color, int priority, @Range(from = 0, to = Integer.MAX_VALUE) int duration);

	void removeGlow(@NotNull Entity entity, @NotNull NamespacedKey key);

	void removeGlow(@NotNull Player player, @NotNull Entity entity, @NotNull NamespacedKey key);

}
