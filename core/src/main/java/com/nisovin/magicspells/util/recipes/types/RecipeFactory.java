package com.nisovin.magicspells.util.recipes.types;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.reflect.Field;

import org.jetbrains.annotations.Nullable;

import com.destroystokyo.paper.MaterialTags;
import com.destroystokyo.paper.MaterialSetTag;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.ConfigReaderUtil;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.recipes.wrapper.CustomRecipe;

public abstract class RecipeFactory<R extends CustomRecipe> {

	private static final Map<String, MaterialSetTag> MATERIAL_TAGS = new HashMap<>();
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
		String keyString = config.getString("namespace-key", config.getName());
		NamespacedKey key;
		try {
			key = new NamespacedKey(MagicSpells.getInstance(), keyString);
		} catch (IllegalArgumentException e) {
			MagicSpells.error("Invalid 'namespace-key' on custom recipe '%s': %s".formatted(config.getName(), keyString));
			return null;
		}

		MagicItem magicItem = getMagicItem(config.get("result"));
		if (magicItem == null) {
			MagicSpells.error("Invalid magic item defined for 'result' on custom recipe '%s'.".formatted(config.getName()));
			return null;
		}
		ItemStack result = magicItem.getItemStack().clone();
		result.setAmount(Math.max(1, config.getInt("quantity", 1)));

		return createRecipe(config, key, result);
	}

	@Nullable
	protected abstract R createRecipe(ConfigurationSection config, NamespacedKey key, ItemStack result);

	protected final RecipeChoice resolveRecipeChoice(ConfigurationSection config, String path) {
		if (!config.isList(path)) {
			Object object = config.get(path);
			if (object instanceof String tagName && tagName.startsWith("tag:")) {
				MaterialSetTag tag = resolveMaterialTag(config, path, tagName);
				return tag == null ? null : new RecipeChoice.MaterialChoice(tag);
			}

			MagicItem magicItem = getMagicItem(object);
			if (magicItem == null) {
				MagicSpells.error("Invalid magic item defined for '%s' on custom recipe '%s'.".formatted(path, config.getName()));
				return null;
			}
			return new RecipeChoice.ExactChoice(getLoreVariants(magicItem));
		}

		boolean isExpectingTags = false;
		List<ItemStack> items = new ArrayList<>();
		List<Material> materials = new ArrayList<>();
		List<?> list = config.getList(path, new ArrayList<>());
		for (int i = 0; i < list.size(); i++) {
			Object object = list.get(i);

			if (object instanceof String tagName && tagName.startsWith("tag:")) {
				isExpectingTags = true;
				MaterialSetTag tag = resolveMaterialTag(config, path, tagName);
				if (tag == null) return null;
				materials.addAll(tag.getValues());
				continue;
			}

			if (isExpectingTags) {
				MagicSpells.error("Invalid entry on custom recipe '%s' at index %d of '%s' - you cannot mix material tags and item-based recipe choices together.".formatted(config.getName(), i, path));
				return null;
			}

			MagicItem magicItem = getMagicItem(object);
			if (magicItem == null) {
				MagicSpells.error("Invalid magic item listed on custom recipe '%s' at index %d of '%s'.".formatted(config.getName(), i, path));
				return null;
			}
			items.addAll(getLoreVariants(magicItem));
		}
		return isExpectingTags ?
				new RecipeChoice.MaterialChoice(materials) :
				new RecipeChoice.ExactChoice(items);
	}

	private List<ItemStack> getLoreVariants(MagicItem magicItem) {
		List<ItemStack> list = new ArrayList<>();
		ItemStack originalItem = magicItem.getItemStack().clone();
		list.add(originalItem);
		ItemStack item = originalItem.clone();

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return list;
		Component displayName = meta.displayName();
		if (displayName == null) return list;
		if (displayName.hasDecoration(TextDecoration.ITALIC)) return list;
		// Remove default "false" italics.
		meta.displayName(displayName.decoration(TextDecoration.ITALIC, TextDecoration.State.NOT_SET));
		item.setItemMeta(meta);
		list.add(item);
		return list;
	}

	private MagicItem getMagicItem(Object object) {
		return switch (object) {
			case String string -> MagicItems.getMagicItemFromString(string);
			case Map<?, ?> map -> MagicItems.getMagicItemFromSection(ConfigReaderUtil.mapToSection(map));
			case null, default -> null;
		};
	}

	private MaterialSetTag resolveMaterialTag(ConfigurationSection config, String path, String tagName) {
		tagName = tagName.replaceFirst("tag:", "");
		MaterialSetTag tag = MATERIAL_TAGS.get(tagName.toUpperCase());
		if (tag != null) return tag;

		MagicSpells.error("Invalid material tag '%s' on option '%s' of custom recipe '%s'.".formatted(tagName, path, config.getName()));
		return null;
	}

	protected final <E extends Enum<E>> E resolveEnum(ConfigurationSection config, String path, Class<E> enumClass) {
		String received = config.getString(path);
		if (received == null) return null;

		E value = Util.enumValueSafe(enumClass, received.toUpperCase());
		if (value != null) return value;

		MagicSpells.error("Invalid %s '%s' for option '%s' on custom recipe '%s'.".formatted(enumClass.getSimpleName(), received, path, config.getName()));
		return null;
	}

}
