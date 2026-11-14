package com.nisovin.magicspells.util.recipes.types;

import java.util.*;
import java.lang.reflect.Field;

import org.jetbrains.annotations.Nullable;

import com.destroystokyo.paper.MaterialTags;
import com.destroystokyo.paper.MaterialSetTag;

import org.bukkit.Tag;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.set.RegistryKeySet;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.ConfigReaderUtil;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.recipes.wrapper.CustomRecipe;

@SuppressWarnings("UnstableApiUsage")
public abstract class RecipeFactory {

	private static final Map<String, Tag<Material>> MATERIAL_TAGS = new HashMap<>();
	static {
		for (Field field : MaterialTags.class.getDeclaredFields()) {
			try {
				if (!(field.get(null) instanceof MaterialSetTag tag)) continue;
				MATERIAL_TAGS.put(field.getName(), tag);
			}
			catch (IllegalAccessException ignored) {}
		}
	}

	@Nullable
	public final CustomRecipe create(ConfigurationSection config) {
		String keyString = config.getString("namespaced-key", config.getString("namespace-key", config.getName()));
		NamespacedKey key = NamespacedKey.fromString(keyString, MagicSpells.getInstance());
		if (key == null) {
			MagicSpells.error("Invalid 'namespaced-key' on custom recipe '%s': %s".formatted(config.getName(), keyString));
			return null;
		}

		MagicItem magicItem = getMagicItem(config.get("result"));
		if (magicItem == null) {
			MagicSpells.error("Invalid magic item defined for 'result' on custom recipe '%s'.".formatted(config.getName()));
			return null;
		}
		ItemStack result = magicItem.getItemStack().clone();
		result.setAmount(Math.max(1, config.getInt("quantity", 1)));

		boolean autoDiscover = config.getBoolean("auto-discover", true);

		Recipe recipe = createRecipe(config, key, result);
		if (recipe == null) return null;

		return new CustomRecipe(recipe, key, autoDiscover);
	}

	@Nullable
	protected abstract Recipe createRecipe(ConfigurationSection config, NamespacedKey key, ItemStack result);

	protected final RecipeChoice resolveRecipeChoice(ConfigurationSection config, String path) {
		if (!config.isList(path)) {
			Object object = config.get(path);
			if (object instanceof String tagName && (tagName.startsWith("tag:") || tagName.startsWith("#"))) {
				RegistryKeySet<ItemType> set = resolveMaterialTag(config, path, tagName);
				return set == null ? null : RecipeChoice.itemType(set);
			}

			MagicItem magicItem = getMagicItem(object);
			if (magicItem == null || magicItem.getItemStack() == null) {
				MagicSpells.error("Invalid magic item defined for '%s' on custom recipe '%s'.".formatted(path, config.getName()));
				return null;
			}
			return RecipeChoice.exactChoice(getItalicVariants(magicItem.getItemStack()));
		}

		List<ItemStack> items = new ArrayList<>();
		List<ItemType> itemTypes = new ArrayList<>();
		List<?> list = config.getList(path, new ArrayList<>());
		for (int i = 0; i < list.size(); i++) {
			Object object = list.get(i);

			if (object instanceof String tagName && (tagName.startsWith("tag:") || tagName.startsWith("#"))) {
				RegistryKeySet<ItemType> set = resolveMaterialTag(config, path, tagName);
				if (set == null) return null;
				itemTypes.addAll(set.resolve(Registry.ITEM));
				continue;
			}

			if (!itemTypes.isEmpty()) {
				MagicSpells.error("Invalid entry on custom recipe '%s' at index %d of '%s' - you cannot mix material tags and item-based recipe choices together.".formatted(config.getName(), i, path));
				return null;
			}

			MagicItem magicItem = getMagicItem(object);
			if (magicItem == null || magicItem.getItemStack() == null) {
				MagicSpells.error("Invalid magic item listed on custom recipe '%s' at index %d of '%s'.".formatted(config.getName(), i, path));
				return null;
			}
			items.addAll(getItalicVariants(magicItem.getItemStack()));
		}

		return itemTypes.isEmpty() ?
			RecipeChoice.exactChoice(items) :
			RecipeChoice.itemType(RegistrySet.keySetFromValues(RegistryKey.ITEM, itemTypes));
	}

	/*
	 * If the item has italics, allow items with unset italics to work.
	 */
	private List<ItemStack> getItalicVariants(ItemStack original) {
		ItemStack clone = original.clone();
		if (!original.hasItemMeta()) return List.of(clone);

		Component customName = original.getItemMeta().customName();
		if (customName == null || !customName.hasDecoration(TextDecoration.ITALIC))
			return List.of(clone);

		ItemStack item = original.clone();
		item.editMeta(meta -> meta.customName(customName.decoration(TextDecoration.ITALIC, TextDecoration.State.NOT_SET)));
		return List.of(clone, item);
	}

	private MagicItem getMagicItem(Object object) {
		return switch (object) {
			case String string -> MagicItems.getMagicItemFromString(string);
			case Map<?, ?> map -> MagicItems.getMagicItemFromSection(ConfigReaderUtil.mapToSection(map));
			case null, default -> null;
		};
	}

	private RegistryKeySet<ItemType> resolveMaterialTag(ConfigurationSection config, String path, String tagName) {
		String tagString = tagName.replaceFirst("tag:|#", "");
		Tag<Material> matTag = MATERIAL_TAGS.get(tagString.toUpperCase());
		if (matTag != null) {
			Set<ItemType> items = new HashSet<>();

			for (Material material : matTag.getValues()) {
				ItemType itemType = material.asItemType();

				if (itemType == null) {
					MagicSpells.error("Invalid material tag '%s' on option '%s' of custom recipe '%s' - cannot use non-item entry '%s'.".formatted(tagName, path, config.getName(), material));
					return null;
				}

				items.add(itemType);
			}

			return RegistrySet.keySetFromValues(RegistryKey.ITEM, items);
		}

		NamespacedKey key = NamespacedKey.fromString(tagString);
		if (key != null) {
			try {
				return Registry.ITEM.getTag(TagKey.create(RegistryKey.ITEM, key));
			} catch (NoSuchElementException _) {}
		}

		MagicSpells.error("Invalid material tag '%s' on option '%s' of custom recipe '%s'.".formatted(tagName, path, config.getName()));
		return null;
	}

	protected final <E extends Enum<E>> E resolveEnum(ConfigurationSection config, String path, Class<E> enumClass) {
		String received = config.getString(path);
		if (received == null) return null;

		E value = Util.enumValueSafe(enumClass, received);
		if (value != null) return value;

		MagicSpells.error("Invalid %s '%s' for option '%s' on custom recipe '%s'.".formatted(enumClass.getSimpleName(), received, path, config.getName()));
		return null;
	}

}
