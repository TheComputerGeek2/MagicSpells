package com.nisovin.magicspells.util.pdc;

import com.nisovin.magicspells.MagicSpells;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;

import io.papermc.paper.persistence.PersistentDataContainerView;

@NullMarked
public record PersistentDataEntry<P, C>(PersistentDataType<P, C> type, NamespacedKey key) {

	/**
	 * Uses the "magicspells" plugin namespace.
	 * @throws IllegalArgumentException If the created {@link NamespacedKey} is invalid.
	 */
	public PersistentDataEntry(PersistentDataType<P, C> type, String key) {
		this(type, new NamespacedKey(MagicSpells.getInstance(), key));
	}

	/**
	 * @see PersistentDataContainerView#has(NamespacedKey, PersistentDataType)
	 */
	public boolean has(PersistentDataContainerView container) {
		return container.has(key, type);
	}

	/**
	 * @see PersistentDataContainerView#get(NamespacedKey, PersistentDataType)
	 */
	public @Nullable C get(PersistentDataContainerView container) {
		return container.get(key, type);
	}

	/**
	 * @see PersistentDataContainerView#getOrDefault(NamespacedKey, PersistentDataType, C)
	 */
	public C getOrDefault(PersistentDataContainerView container, C defaultValue) {
		return container.getOrDefault(key, type, defaultValue);
	}

	/**
	 * @see PersistentDataContainer#set(NamespacedKey, PersistentDataType, C)
	 */
	public void set(PersistentDataContainer container, C value) {
		container.set(key, type, value);
	}

	/**
	 * @see PersistentDataContainer#remove(NamespacedKey)
	 */
	public void remove(PersistentDataContainer container) {
		container.remove(key);
	}

}
