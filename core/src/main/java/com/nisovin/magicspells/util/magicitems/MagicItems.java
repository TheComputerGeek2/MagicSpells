package com.nisovin.magicspells.util.magicitems;

import java.util.*;
import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.itemreader.*;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.handlers.EnchantmentHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute;
import com.nisovin.magicspells.util.itemreader.alternative.AlternativeReaderManager;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.*;

public class MagicItems {

	private static final Map<String, MagicItem> magicItems = new HashMap<>();

	private static final Cache<ItemStack, MagicItemData> itemStackCache = Caffeine.newBuilder()
		.expireAfterAccess(Duration.ofMinutes(5))
		.maximumSize(10000)
		.build();

	public static Map<String, MagicItem> getMagicItems() {
		return magicItems;
	}

	public static Collection<String> getMagicItemKeys() {
		return magicItems.keySet();
	}

	public static Collection<MagicItem> getMagicItemValues() {
		return magicItems.values();
	}

	public static MagicItem getMagicItemByInternalName(String internalName) {
		if (!magicItems.containsKey(internalName)) return null;
		if (magicItems.get(internalName) == null) return null;
		return magicItems.get(internalName);
	}

	public static ItemStack getItemByInternalName(String internalName) {
		if (!magicItems.containsKey(internalName)) return null;
		if (magicItems.get(internalName) == null) return null;
		if (magicItems.get(internalName).getItemStack() == null) return null;
		return magicItems.get(internalName).getItemStack().clone();
	}

	public static MagicItemData getMagicItemDataByInternalName(String internalName) {
		if (!magicItems.containsKey(internalName)) return null;
		if (magicItems.get(internalName) == null) return null;
		return magicItems.get(internalName).getMagicItemData();
	}

	public static MagicItemData getMagicItemDataFromItemStack(ItemStack item) {
		if (item == null) return null;

		MagicItemData data = itemStackCache.getIfPresent(item);
		if (data != null) return data;

		data = getMagicItemDataFromItemStackInternal(item);
		itemStackCache.put(item.clone(), data);

		return data;
	}

	private static MagicItemData getMagicItemDataFromItemStackInternal(ItemStack item) {
		MagicItemData data = new MagicItemData();

		// type
		data.setAttribute(TYPE, item.getType());
		if (item.getType().isAir()) return data;

		// amount
		data.setAttribute(AMOUNT, item.getAmount());

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return data;

		// name
		NameHandler.processMagicItemData(meta, data);

		// durability
		if (item.getType().getMaxDurability() > 0) DurabilityHandler.processMagicItemData(meta, data);

		// repairCost
		RepairableHandler.processMagicItemData(meta, data);

		// customModelData
		CustomModelDataHandler.processMagicItemData(meta, data);

		// power, fireworkEffects
		FireworkHandler.processMagicItemData(meta, data);

		// unbreakable
		data.setAttribute(UNBREAKABLE, meta.isUnbreakable());

		// tooltip
		boolean tooltip = true;
		for (ItemFlag itemFlag : ItemFlag.values()) {
			if (!meta.getItemFlags().contains(itemFlag)) tooltip = false;
		}
		data.setAttribute(HIDE_TOOLTIP, tooltip);

		// color
		LeatherArmorHandler.processMagicItemData(meta, data);

		// potion, potionEffects, potionColor
		PotionHandler.processMagicItemData(meta, data);

		// suspiciousStew
		SuspiciousStewHandler.processMagicItemData(meta, data);

		// fireworkEffect
		FireworkEffectHandler.processMagicItemData(meta, data);

		// skullOwner
		SkullHandler.processMagicItemData(meta, data);

		// author, title, pages
		WrittenBookHandler.processMagicItemData(meta, data);

		if (meta.hasEnchantmentGlintOverride() && meta.getEnchantmentGlintOverride())
			data.setAttribute(FAKE_GLINT, true);

		// enchantments
		if (meta.hasEnchants())
			data.setAttribute(ENCHANTS, meta.getEnchants());

		// attributes
		AttributeHandler.processMagicItemData(meta, data);

		// lore
		if (meta.hasLore())
			data.setAttribute(LORE, meta.lore());

		// patterns
		BannerHandler.processMagicItemData(meta, data);

		// block data
		BlockDataHandler.processMagicItemData(meta, data, item.getType());

		return data;
	}

	public static MagicItemData getMagicItemDataFromString(String str) {
		if (str == null) return null;
		if (magicItems.containsKey(str)) return magicItems.get(str).getMagicItemData();

		return MagicItemDataParser.parseMagicItemData(str);
	}

	public static MagicItem getMagicItemFromString(String str) {
		if (str == null) return null;
		if (magicItems.containsKey(str)) return magicItems.get(str);

		MagicItem magicItem;
		MagicItemData itemData = MagicItemDataParser.parseMagicItemData(str);
		if (itemData == null) return null;

		magicItem = getMagicItemFromData(itemData);
		return magicItem;
	}

	public static MagicItem getMagicItemFromData(MagicItemData data) {
		if (data == null) return null;

		Material type = (Material) data.getAttribute(TYPE);
		if (type == null) return null;

		ItemStack item = new ItemStack(type);

		if (type.isAir()) return new MagicItem(item, data);

		if (data.hasAttribute(AMOUNT)) {
			int amount = (int) data.getAttribute(AMOUNT);
			if (amount >= 1) item.setAmount(amount);
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return new MagicItem(item, data);

		// Name
		NameHandler.processItemMeta(meta, data);

		// Lore
		LoreHandler.processItemMeta(meta, data);

		// Custom Model Data
		CustomModelDataHandler.processItemMeta(meta, data);

		// Enchantments
		if (data.hasAttribute(ENCHANTS)) {
			Map<Enchantment, Integer> enchantments = (Map<Enchantment, Integer>) data.getAttribute(ENCHANTS);
			for (Enchantment enchant : enchantments.keySet()) {
				int level = enchantments.get(enchant);

				if (meta instanceof EnchantmentStorageMeta) ((EnchantmentStorageMeta) meta).addStoredEnchant(enchant, level, true);
				else meta.addEnchant(enchant, level, true);
			}
		}

		if (data.hasAttribute(FAKE_GLINT) && (boolean) data.getAttribute(FAKE_GLINT))
			meta.setEnchantmentGlintOverride(true);

		// Armor color
		LeatherArmorHandler.processItemMeta(meta, data);

		// Potion effects and potion color
		PotionHandler.processItemMeta(meta, data);

		// Skull owner
		SkullHandler.processItemMeta(meta, data);

		// Durability
		if (type.getMaxDurability() > 0) DurabilityHandler.processItemMeta(meta, data);

		// Repair cost
		RepairableHandler.processItemMeta(meta, data);

		// Written book
		WrittenBookHandler.processItemMeta(meta, data);

		// Banner
		BannerHandler.processItemMeta(meta, data);

		// Firework Star
		FireworkEffectHandler.processItemMeta(meta, data);

		// Firework
		FireworkHandler.processItemMeta(meta, data);

		// Suspicious Stew
		SuspiciousStewHandler.processItemMeta(meta, data);

		// Block Data
		BlockDataHandler.processItemMeta(meta, data);

		// Attributes
		AttributeHandler.processItemMeta(meta, data);

		// Unbreakable
		if (data.hasAttribute(UNBREAKABLE))
			meta.setUnbreakable((boolean) data.getAttribute(UNBREAKABLE));

		// Hide tooltip
		if (data.hasAttribute(HIDE_TOOLTIP) && (boolean) data.getAttribute(HIDE_TOOLTIP))
			meta.addItemFlags(ItemFlag.values());

		// Set meta
		item.setItemMeta(meta);

		return new MagicItem(item, data);
	}

	public static MagicItem getMagicItemFromSection(ConfigurationSection section) {
		try {
			// It MUST have a type option
			if (!section.contains("type")) return null;

			// See if this is managed by an alternative reader
			ItemStack item = AlternativeReaderManager.deserialize(section);
			if (item != null) {
				MagicItem magicItem = new MagicItem(item, getMagicItemDataFromItemStack(item));

				if (section.isList("ignored-attributes")) {
					Set<MagicItemAttribute> ignoredAttributes = magicItem.getMagicItemData().getIgnoredAttributes();
					List<String> ignoredAttributeStrings = section.getStringList("ignored-attributes");

					for (String attr : ignoredAttributeStrings) {
						String attrValue = attr.toUpperCase().replace("-", "_");

						try {
							ignoredAttributes.add(MagicItemAttribute.valueOf(attrValue));
						} catch (IllegalArgumentException e) {
							switch (attrValue) {
								case "ENCHANTMENTS" -> ignoredAttributes.add(ENCHANTS);
								case "POTION_DATA" -> ignoredAttributes.add(POTION_TYPE);
								default -> DebugHandler.debugBadEnumValue(MagicItemAttribute.class, attr);
							}
						}
					}
				}

				if (section.isList("blacklisted-attributes")) {
					Set<MagicItemAttribute> blacklistedAttributes = magicItem.getMagicItemData().getBlacklistedAttributes();
					List<String> blacklistedAttributeStrings = section.getStringList("blacklisted-attributes");

					for (String attr : blacklistedAttributeStrings) {
						String attrValue = attr.toUpperCase().replace("-", "_");

						try {
							blacklistedAttributes.add(MagicItemAttribute.valueOf(attrValue));
						} catch (IllegalArgumentException e) {
							switch (attrValue) {
								case "ENCHANTMENTS" -> blacklistedAttributes.add(ENCHANTS);
								case "POTION_DATA" -> blacklistedAttributes.add(POTION_TYPE);
								default -> DebugHandler.debugBadEnumValue(MagicItemAttribute.class, attr);
							}
						}
					}
				}

				if (section.isBoolean("strict-enchants"))
					magicItem.getMagicItemData().setStrictEnchants(section.getBoolean("strict-enchants"));

				if (section.isBoolean("strict-block-data"))
					magicItem.getMagicItemData().setStrictBlockData(section.getBoolean("strict-block-data"));

				if (section.isBoolean("strict-durability"))
					magicItem.getMagicItemData().setStrictDurability(section.getBoolean("strict-durability"));

				if (section.isBoolean("strict-enchant-level"))
					magicItem.getMagicItemData().setStrictEnchantLevel(section.getBoolean("strict-enchant-level"));

				return magicItem;
			}

			MagicItemData itemData = new MagicItemData();

			String typeString = section.getString("type");
			Material type = Util.getMaterial(typeString);
			if (type == null) {
				DebugHandler.debugBadEnumValue(Material.class, typeString);
				return null;
			}

			item = new ItemStack(type);
			itemData.setAttribute(TYPE, type);

			if (type.isAir()) return new MagicItem(item, itemData);

			if (section.isInt("amount")) {
				int amount = section.getInt("amount");

				if (amount >= 1) {
					item.setAmount(amount);
					itemData.setAttribute(AMOUNT, amount);
				}
			}

			ItemMeta meta = item.getItemMeta();
			if (meta == null) return new MagicItem(item, itemData);

			// Name
			NameHandler.process(section, meta, itemData);

			// Lore
			LoreHandler.process(section, meta, itemData);

			// CustomModelData
			CustomModelDataHandler.process(section, meta, itemData);

			// Enchants
			// <enchantmentName> <level>
			if (section.isList("enchants")) {
				List<String> enchants = section.getStringList("enchants");
				for (String enchant : enchants) {

					String[] data = enchant.split(" ");
					Enchantment e = EnchantmentHandler.getEnchantment(data[0]);
					if (e == null) {
						MagicSpells.error('\'' + data[0] + "' could not be connected to an enchantment");
						continue;
					}

					int level = 0;
					if (data.length > 1) {
						try {
							level = Integer.parseInt(data[1]);
						} catch (NumberFormatException ex) {
							DebugHandler.debugNumberFormat(ex);
						}
					}

					if (meta instanceof EnchantmentStorageMeta) ((EnchantmentStorageMeta) meta).addStoredEnchant(e, level, true);
					else meta.addEnchant(e, level, true);
				}

				if (meta instanceof EnchantmentStorageMeta storageMeta) {

					if (storageMeta.hasStoredEnchants()) {
						Map<Enchantment, Integer> enchantments = storageMeta.getStoredEnchants();
						if (!enchantments.isEmpty()) itemData.setAttribute(ENCHANTS, enchantments);
					}
				} else if (meta.hasEnchants()) {
					Map<Enchantment, Integer> enchantments = meta.getEnchants();
					if (!enchantments.isEmpty()) itemData.setAttribute(ENCHANTS, enchantments);
				}
			}

			if (section.getBoolean("fake-glint")) {
				meta.setEnchantmentGlintOverride(true);
				itemData.setAttribute(FAKE_GLINT, true);
			}

			// Armor color
			LeatherArmorHandler.process(section, meta, itemData);

			// Potion effects, color, type
			PotionHandler.process(section, meta, itemData);

			// Skull owner
			SkullHandler.process(section, meta, itemData);

			// Durability
			if (type.getMaxDurability() > 0) DurabilityHandler.process(section, meta, itemData);

			// Repair cost
			RepairableHandler.process(section, meta, itemData);

			// Written book
			WrittenBookHandler.process(section, meta, itemData);

			// Banner
			BannerHandler.process(section, meta, itemData);

			// Firework Star
			FireworkEffectHandler.process(section, meta, itemData);

			// Firework
			FireworkHandler.process(section, meta, itemData);

			// Suspicious Stew
			SuspiciousStewHandler.process(section, meta, itemData);

			// Block Data
			BlockDataHandler.process(section, meta, itemData, type);

			// Unbreakable
			if (section.isBoolean("unbreakable")) {
				boolean unbreakable = section.getBoolean("unbreakable");

				meta.setUnbreakable(unbreakable);
				itemData.setAttribute(UNBREAKABLE, unbreakable);
			}

			if (MagicSpells.hideMagicItemTooltips()) {
				meta.addItemFlags(ItemFlag.values());
				itemData.setAttribute(HIDE_TOOLTIP, true);
			} else if (section.isBoolean("hide-tooltip")) {
				boolean hideTooltip = section.getBoolean("hide-tooltip");

				if (hideTooltip) meta.addItemFlags(ItemFlag.values());
				itemData.setAttribute(HIDE_TOOLTIP, hideTooltip);
			}

			// Attributes
			AttributeHandler.process(section, meta, itemData);

			// Set meta
			item.setItemMeta(meta);

			if (section.isList("ignored-attributes")) {
				List<String> ignoredAttributeStrings = section.getStringList("ignored-attributes");
				Set<MagicItemAttribute> ignoredAttributes = itemData.getIgnoredAttributes();

				for (String attr : ignoredAttributeStrings) {
					String attrValue = attr.toUpperCase().replace("-", "_");

					try {
						ignoredAttributes.add(MagicItemAttribute.valueOf(attrValue));
					} catch (IllegalArgumentException e) {
						switch (attrValue) {
							case "ENCHANTMENTS" -> ignoredAttributes.add(ENCHANTS);
							case "POTION_DATA" -> ignoredAttributes.add(POTION_TYPE);
							default -> DebugHandler.debugBadEnumValue(MagicItemAttribute.class, attr);
						}
					}
				}
			}

			if (section.isList("blacklisted-attributes")) {
				List<String> blacklistedAttributeStrings = section.getStringList("blacklisted-attributes");
				Set<MagicItemAttribute> blacklistedAttributes = itemData.getBlacklistedAttributes();

				for (String attr : blacklistedAttributeStrings) {
					String attrValue = attr.toUpperCase().replace("-", "_");

					try {
						blacklistedAttributes.add(MagicItemAttribute.valueOf(attrValue));
					} catch (IllegalArgumentException e) {
						switch (attrValue) {
							case "ENCHANTMENTS" -> blacklistedAttributes.add(ENCHANTS);
							case "POTION_DATA" -> blacklistedAttributes.add(POTION_TYPE);
							default -> DebugHandler.debugBadEnumValue(MagicItemAttribute.class, attr);
						}
					}
				}
			}

			if (section.isBoolean("strict-enchants"))
				itemData.setStrictEnchants(section.getBoolean("strict-enchants"));

			if (section.isBoolean("strict-block-data"))
				itemData.setStrictBlockData(section.getBoolean("strict-block-data"));

			if (section.isBoolean("strict-durability"))
				itemData.setStrictDurability(section.getBoolean("strict-durability"));

			if (section.isBoolean("strict-enchant-level"))
				itemData.setStrictEnchantLevel(section.getBoolean("strict-enchant-level"));

			return new MagicItem(item, itemData);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
