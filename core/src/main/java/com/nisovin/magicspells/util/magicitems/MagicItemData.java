package com.nisovin.magicspells.util.magicitems;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Multimap;
import com.google.common.collect.Iterables;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;

import org.bukkit.*;
import org.bukkit.potion.PotionType;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffect;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.attribute.AttributeModifier;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.RegistryAccess;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.AttributeUtil;

public class MagicItemData {

	private final Map<MagicItemAttribute, Object> itemAttributes = new EnumMap<>(MagicItemAttribute.class);
	private final Set<MagicItemAttribute> blacklistedAttributes = EnumSet.noneOf(MagicItemAttribute.class);
	private final Set<MagicItemAttribute> ignoredAttributes = EnumSet.noneOf(MagicItemAttribute.class);

	private boolean strictEnchantLevel = true;
	private boolean strictDurability = true;
	private boolean strictBlockData = true;
	private boolean strictEnchants = true;

	public Object getAttribute(MagicItemAttribute attr) {
		return itemAttributes.get(attr);
	}

	public void setAttribute(MagicItemAttribute attr, Object obj) {
		if (obj == null) return;
		if (!attr.getDataType().isAssignableFrom(obj.getClass())) return;

		itemAttributes.put(attr, obj);
	}

	public void removeAttribute(MagicItemAttribute attr) {
		itemAttributes.remove(attr);
	}

	public boolean hasAttribute(MagicItemAttribute atr) {
		return itemAttributes.containsKey(atr);
	}

	public Set<MagicItemAttribute> getBlacklistedAttributes() {
		return blacklistedAttributes;
	}

	public Set<MagicItemAttribute> getIgnoredAttributes() {
		return ignoredAttributes;
	}

	public boolean isStrictEnchantLevel() {
		return strictEnchantLevel;
	}

	public void setStrictEnchantLevel(boolean strictEnchantLevel) {
		this.strictEnchantLevel = strictEnchantLevel;
	}

	public boolean isStrictDurability() {
		return strictDurability;
	}

	public void setStrictDurability(boolean strictDurability) {
		this.strictDurability = strictDurability;
	}

	public boolean isStrictBlockData() {
		return strictBlockData;
	}

	public void setStrictBlockData(boolean strictBlockData) {
		this.strictBlockData = strictBlockData;
	}

	public boolean isStrictEnchants() {
		return strictEnchants;
	}

	public void setStrictEnchants(boolean strictEnchants) {
		this.strictEnchants = strictEnchants;
	}

	public boolean matches(MagicItemData data) {
		if (this == data) return true;

		Set<MagicItemAttribute> keysSelf = itemAttributes.keySet();
		Set<MagicItemAttribute> keysOther = data.itemAttributes.keySet();

		for (MagicItemAttribute attr : keysSelf) {
			if (ignoredAttributes.contains(attr)) continue;
			if (!keysOther.contains(attr)) return false;
		}

		for (MagicItemAttribute attr : blacklistedAttributes) {
			if (keysOther.contains(attr)) return false;
		}

		for (MagicItemAttribute attr : keysSelf) {
			if (ignoredAttributes.contains(attr)) continue;

			switch (attr) {
				case ATTRIBUTES -> {
					Multimap<Attribute, AttributeModifier> self = (Multimap<Attribute, AttributeModifier>) itemAttributes.get(attr);
					Multimap<Attribute, AttributeModifier> other = (Multimap<Attribute, AttributeModifier>) data.itemAttributes.get(attr);

					if (!Iterables.elementsEqual(self.entries(), other.entries())) return false;
				}
				case AUTHOR, NAME, TITLE -> {
					String legacySelf = Util.getLegacyFromComponent((Component) itemAttributes.get(attr));
					String legacyOther = Util.getLegacyFromComponent((Component) data.itemAttributes.get(attr));

					if (!legacySelf.equals(legacyOther)) return false;
				}
				case BLOCK_DATA -> {
					BlockData blockDataSelf = (BlockData) itemAttributes.get(attr);
					BlockData blockDataOther = (BlockData) data.itemAttributes.get(attr);

					if (strictBlockData) {
						if (!blockDataSelf.equals(blockDataOther))
							return false;

						continue;
					}

					if (!blockDataOther.matches(blockDataSelf)) return false;
				}
				case DURABILITY -> {
					Integer durabilitySelf = (Integer) itemAttributes.get(attr);
					Integer durabilityOther = (Integer) data.itemAttributes.get(attr);

					int compare = durabilitySelf.compareTo(durabilityOther);
					if (strictDurability ? compare != 0 : compare < 0) return false;
				}
				case ENCHANTS -> {
					if (strictEnchants && strictEnchantLevel) {
						if (!itemAttributes.get(attr).equals(data.itemAttributes.get(attr)))
							return false;

						continue;
					}

					Map<Enchantment, Integer> enchantsSelf = (Map<Enchantment, Integer>) itemAttributes.get(attr);
					Map<Enchantment, Integer> enchantsOther = (Map<Enchantment, Integer>) data.itemAttributes.get(attr);

					if (strictEnchants && enchantsSelf.size() != enchantsOther.size()) return false;

					for (Enchantment enchant : enchantsSelf.keySet()) {
						if (!enchantsOther.containsKey(enchant)) return false;

						Integer levelSelf = enchantsSelf.get(enchant);
						Integer levelOther = enchantsOther.get(enchant);

						int compare = levelSelf.compareTo(levelOther);
						if (strictEnchantLevel ? compare != 0 : compare > 0) return false;
					}
				}
				case LORE, PAGES -> {
					List<Component> componentsSelf = (List<Component>) itemAttributes.get(attr);
					List<Component> componentsOther = (List<Component>) data.itemAttributes.get(attr);
					if (componentsSelf.size() != componentsOther.size()) return false;

					for (int i = 0; i < componentsSelf.size(); i++) {
						String legacySelf = Util.getLegacyFromComponent(componentsSelf.get(i));
						String legacyOther = Util.getLegacyFromComponent(componentsOther.get(i));

						if (!legacySelf.equals(legacyOther)) return false;
					}
				}
				default -> {
					if (!itemAttributes.get(attr).equals(data.itemAttributes.get(attr))) return false;
				}
			}
		}

		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MagicItemData other)) return false;
		return itemAttributes.equals(other.itemAttributes)
			&& ignoredAttributes.equals(other.ignoredAttributes)
			&& blacklistedAttributes.equals(other.blacklistedAttributes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(itemAttributes, ignoredAttributes, blacklistedAttributes);
	}

	@Override
	public MagicItemData clone() {
		MagicItemData data = new MagicItemData();

		if (!itemAttributes.isEmpty()) data.itemAttributes.putAll(itemAttributes);
		if (!ignoredAttributes.isEmpty()) data.ignoredAttributes.addAll(ignoredAttributes);
		if (!blacklistedAttributes.isEmpty()) data.blacklistedAttributes.addAll(blacklistedAttributes);

		return data;
	}

	public enum MagicItemAttribute {

		TYPE(Material.class),
		NAME(Component.class),
		AMOUNT(Integer.class),
		DURABILITY(Integer.class),
		REPAIR_COST(Integer.class),
		CUSTOM_MODEL_DATA(Integer.class),
		POWER(Integer.class),
		UNBREAKABLE(Boolean.class),
		HIDE_TOOLTIP(Boolean.class),
		FAKE_GLINT(Boolean.class),
		POTION_TYPE(PotionType.class),
		COLOR(Color.class),
		FIREWORK_EFFECT(FireworkEffect.class),
		TITLE(Component.class),
		AUTHOR(Component.class),
		UUID(UUID.class),
		TEXTURE(String.class),
		SIGNATURE(String.class),
		SKULL_OWNER(String.class),
		BLOCK_DATA(BlockData.class),
		ENCHANTS(Map.class),
		LORE(List.class),
		PAGES(List.class),
		POTION_EFFECTS(List.class),
		PATTERNS(List.class),
		FIREWORK_EFFECTS(List.class),
		ATTRIBUTES(Multimap.class);

		private final Class<?> dataType;
		private final String asString;

		MagicItemAttribute(Class<?> dataType) {
			this.dataType = dataType;
			asString = name().toLowerCase().replace('_', '-');
		}

		public Class<?> getDataType() {
			return dataType;
		}

		@Override
		public String toString() {
			return asString;
		}

	}

	@Override
	public String toString() {
		JsonObject magicItem = new JsonObject();

		if (hasAttribute(MagicItemAttribute.NAME))
			magicItem.addProperty("name", Util.getStringFromComponent((Component) getAttribute(MagicItemAttribute.NAME)));

		if (hasAttribute(MagicItemAttribute.AMOUNT))
			magicItem.addProperty("amount", (int) getAttribute(MagicItemAttribute.AMOUNT));

		if (hasAttribute(MagicItemAttribute.DURABILITY))
			magicItem.addProperty("durability", (int) getAttribute(MagicItemAttribute.DURABILITY));

		if (hasAttribute(MagicItemAttribute.REPAIR_COST))
			magicItem.addProperty("repair-cost", (int) getAttribute(MagicItemAttribute.REPAIR_COST));

		if (hasAttribute(MagicItemAttribute.CUSTOM_MODEL_DATA))
			magicItem.addProperty("custom-model-data", (int) getAttribute(MagicItemAttribute.CUSTOM_MODEL_DATA));

		if (hasAttribute(MagicItemAttribute.POWER))
			magicItem.addProperty("power", (int) getAttribute(MagicItemAttribute.POWER));

		if (hasAttribute(MagicItemAttribute.UNBREAKABLE))
			magicItem.addProperty("unbreakable", (boolean) getAttribute(MagicItemAttribute.UNBREAKABLE));

		if (hasAttribute(MagicItemAttribute.HIDE_TOOLTIP))
			magicItem.addProperty("hide-tooltip", (boolean) getAttribute(MagicItemAttribute.HIDE_TOOLTIP));

		if (hasAttribute(MagicItemAttribute.COLOR)) {
			Color color = (Color) getAttribute(MagicItemAttribute.COLOR);
			magicItem.addProperty("color", Integer.toHexString(color.asRGB()));
		}

		if (hasAttribute(MagicItemAttribute.POTION_TYPE)) {
			PotionType potionType = (PotionType) getAttribute(MagicItemAttribute.POTION_TYPE);
			magicItem.addProperty("potion-type", potionType.getKey().getKey());
		}

		if (hasAttribute(MagicItemAttribute.FIREWORK_EFFECT)) {
			FireworkEffect effect = (FireworkEffect) getAttribute(MagicItemAttribute.FIREWORK_EFFECT);

			StringBuilder effectBuilder = new StringBuilder();
			effectBuilder
				.append(effect.getType())
				.append(' ')
				.append(effect.hasTrail())
				.append(' ')
				.append(effect.hasFlicker());

			if (!effect.getColors().isEmpty()) {
				effectBuilder.append(' ');

				boolean previousColor = false;
				for (Color color : effect.getColors()) {
					if (previousColor) effectBuilder.append(',');

					effectBuilder.append(Integer.toHexString(color.asRGB()));
					previousColor = true;
				}

				if (!effect.getFadeColors().isEmpty()) {
					effectBuilder.append(' ');

					previousColor = false;
					for (Color color : effect.getFadeColors()) {
						if (previousColor) effectBuilder.append(',');

						effectBuilder.append(Integer.toHexString(color.asRGB()));
						previousColor = true;
					}
				}
			}

			magicItem.addProperty("firework-effect", effectBuilder.toString());
		}

		if (hasAttribute(MagicItemAttribute.SKULL_OWNER))
			magicItem.addProperty("skull-owner", (String) getAttribute(MagicItemAttribute.SKULL_OWNER));

		if (hasAttribute(MagicItemAttribute.TITLE))
			magicItem.addProperty("title", Util.getStringFromComponent((Component) getAttribute(MagicItemAttribute.TITLE)));

		if (hasAttribute(MagicItemAttribute.AUTHOR))
			magicItem.addProperty("author", Util.getStringFromComponent((Component) getAttribute(MagicItemAttribute.AUTHOR)));

		if (hasAttribute(MagicItemAttribute.UUID))
			magicItem.addProperty("uuid", getAttribute(MagicItemAttribute.UUID).toString());

		if (hasAttribute(MagicItemAttribute.TEXTURE))
			magicItem.addProperty("texture", (String) getAttribute(MagicItemAttribute.TEXTURE));

		if (hasAttribute(MagicItemAttribute.SIGNATURE))
			magicItem.addProperty("signature", (String) getAttribute(MagicItemAttribute.SIGNATURE));

		if (hasAttribute(MagicItemAttribute.ENCHANTS)) {
			Map<Enchantment, Integer> enchants = (Map<Enchantment, Integer>) getAttribute(MagicItemAttribute.ENCHANTS);

			JsonObject enchantsObject = new JsonObject();
			for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet())
				enchantsObject.addProperty(entry.getKey().getKey().getKey(), entry.getValue());

			magicItem.add("enchants", enchantsObject);
		}

		if (hasAttribute(MagicItemAttribute.FAKE_GLINT))
			magicItem.addProperty("fake-glint", (boolean) getAttribute(MagicItemAttribute.FAKE_GLINT));

		if (hasAttribute(MagicItemAttribute.ATTRIBUTES)) {
			Multimap<Attribute, AttributeModifier> attributes = (Multimap<Attribute, AttributeModifier>) getAttribute(MagicItemAttribute.ATTRIBUTES);

			JsonArray attributesArray = new JsonArray();
			for (Map.Entry<Attribute, AttributeModifier> entry : attributes.entries()) {
				AttributeModifier modifier = entry.getValue();

				JsonObject modifierObject = new JsonObject();
				modifierObject.addProperty("id", modifier.getKey().asString());
				modifierObject.addProperty("type", entry.getKey().getKey().asString());
				modifierObject.addProperty("operation", AttributeUtil.getOperationName(modifier.getOperation()));
				modifierObject.addProperty("amount", modifier.getAmount());
				modifierObject.addProperty("slot", modifier.getSlotGroup().toString());

				attributesArray.add(modifierObject);
			}

			magicItem.add("attributes", attributesArray);
		}

		if (hasAttribute(MagicItemAttribute.LORE)) {
			List<Component> lore = (List<Component>) getAttribute(MagicItemAttribute.LORE);

			JsonArray loreArray = new JsonArray(lore.size());
			for (Component line : lore) loreArray.add(Util.getStringFromComponent(line));

			magicItem.add("lore", loreArray);
		}

		if (hasAttribute(MagicItemAttribute.PAGES)) {
			List<Component> pages = (List<Component>) getAttribute(MagicItemAttribute.PAGES);

			JsonArray pagesArray = new JsonArray(pages.size());
			for (Component line : pages) pagesArray.add(Util.getStringFromComponent(line));

			magicItem.add("pages", pagesArray);
		}

		if (hasAttribute(MagicItemAttribute.PATTERNS)) {
			List<Pattern> patterns = (List<Pattern>) getAttribute(MagicItemAttribute.PATTERNS);
			Registry<@NotNull PatternType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN);

			JsonArray patternsArray = new JsonArray(patterns.size());
			for (Pattern pattern : patterns) {
				NamespacedKey key = registry.getKey(pattern.getPattern());
				if (key == null) continue;

				String patternString = key.asMinimalString().toLowerCase() + " " + pattern.getColor().name().toLowerCase();
				patternsArray.add(patternString);
			}

			magicItem.add("patterns", patternsArray);
		}

		if (hasAttribute(MagicItemAttribute.POTION_EFFECTS)) {
			List<PotionEffect> effects = (List<PotionEffect>) getAttribute(MagicItemAttribute.POTION_EFFECTS);

			StringBuilder effectBuilder = new StringBuilder();
			JsonArray potionEffectsArray = new JsonArray(effects.size());
			for (PotionEffect effect : effects) {
				effectBuilder.setLength(0);

				effectBuilder
					.append(effect.getType().getKey().getKey())
					.append(' ')
					.append(effect.getAmplifier())
					.append(' ')
					.append(effect.getDuration())
					.append(' ')
					.append(effect.isAmbient())
					.append(' ')
					.append(effect.hasParticles())
					.append(' ')
					.append(effect.hasIcon());

				potionEffectsArray.add(effectBuilder.toString());
			}

			magicItem.add("potion-effects", potionEffectsArray);
		}

		if (hasAttribute(MagicItemAttribute.FIREWORK_EFFECTS)) {
			List<FireworkEffect> effects = (List<FireworkEffect>) getAttribute(MagicItemAttribute.FIREWORK_EFFECTS);

			StringBuilder effectBuilder = new StringBuilder();
			JsonArray fireworkEffectsArray = new JsonArray(effects.size());
			for (FireworkEffect effect : effects) {
				effectBuilder.setLength(0);

				effectBuilder
					.append(effect.getType())
					.append(' ')
					.append(effect.hasTrail())
					.append(' ')
					.append(effect.hasFlicker());

				boolean previousColor = false;
				if (!effect.getColors().isEmpty()) {
					effectBuilder.append(' ');

					for (Color color : effect.getColors()) {
						if (previousColor) effectBuilder.append(',');
						effectBuilder.append(Integer.toHexString(color.asRGB()));
						previousColor = true;
					}

					if (!effect.getFadeColors().isEmpty()) {
						effectBuilder.append(' ');

						previousColor = false;
						for (Color color : effect.getFadeColors()) {
							if (previousColor) effectBuilder.append(',');
							effectBuilder.append(Integer.toHexString(color.asRGB()));
							previousColor = true;
						}
					}
				}

				fireworkEffectsArray.add(effectBuilder.toString());
			}

			magicItem.add("firework-effects", fireworkEffectsArray);
		}

		if (!ignoredAttributes.isEmpty()) {
			JsonArray ignoredAttributesArray = new JsonArray(ignoredAttributes.size());
			for (MagicItemAttribute attribute : ignoredAttributes) ignoredAttributesArray.add(attribute.name());

			magicItem.add("ignored-attributes", ignoredAttributesArray);
		}

		if (!blacklistedAttributes.isEmpty()) {
			JsonArray blacklistedAttributesArray = new JsonArray(blacklistedAttributes.size());
			for (MagicItemAttribute attribute : blacklistedAttributes) blacklistedAttributesArray.add(attribute.name());

			magicItem.add("blacklisted-attributes", blacklistedAttributesArray);
		}

		if (!strictEnchants) magicItem.addProperty("strict-enchants", false);
		if (!strictDurability) magicItem.addProperty("strict-durability", false);
		if (!strictBlockData) magicItem.addProperty("strict-block-data", false);
		if (!strictEnchantLevel) magicItem.addProperty("strict-enchant-level", false);

		String output = magicItem.toString();
		if (hasAttribute(MagicItemAttribute.TYPE))
			output = ((Material) getAttribute(MagicItemAttribute.TYPE)).getKey().getKey() + output;

		return output;
	}

}
