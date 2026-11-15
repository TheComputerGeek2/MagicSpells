package com.nisovin.magicspells.util;

import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.itemreader.PotionHandler;
import com.nisovin.magicspells.util.itemreader.DurabilityHandler;
import com.nisovin.magicspells.util.itemreader.WrittenBookHandler;
import com.nisovin.magicspells.util.itemreader.LeatherArmorHandler;

import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.*;

public class CastItem {

	private Material type = null;
	private String name = null;

	private int amount = 0;
	private int durability = -1;
	private int customModelData = 0;
	private boolean unbreakable = false;

	private NamespacedKey itemModel = null;

	private Color color = null;
	private PotionType potionType = null;
	private String title = null;
	private String author = null;

	private Map<Enchantment, Integer> enchants = null;
	private List<String> lore = null;

	public CastItem() {

	}

	public CastItem(ItemStack item) {
		if (item == null) throw new NullPointerException("itemStack");
		ItemMeta meta = item.getItemMeta();

		type = item.getType();
		if (isTypeValid()) {
			if (!MagicSpells.ignoreCastItemNames()) {
				if (MagicSpells.ignoreCastItemNameColors())
					name = PlainTextComponentSerializer.plainText().serializeOrNull(meta.customName());
				else
					name = LegacyComponentSerializer.legacySection().serializeOrNull(meta.customName());
			}
			if (!MagicSpells.ignoreCastItemAmount()) amount = item.getAmount();
			if (!MagicSpells.ignoreCastItemDurability(type) && type.getMaxDurability() > 0) durability = DurabilityHandler.getDurability(meta);
			if (!MagicSpells.ignoreCastItemCustomModelData()) customModelData = ItemUtil.getCustomModelData(meta);
			if (!MagicSpells.ignoreCastItemItemModel()) itemModel = meta.getItemModel();
			if (!MagicSpells.ignoreCastItemBreakability()) unbreakable = meta.isUnbreakable();
			if (!MagicSpells.ignoreCastItemColor()) color = LeatherArmorHandler.getColor(meta);
			if (!MagicSpells.ignoreCastItemPotionType()) potionType = PotionHandler.getPotionType(meta);
			if (!MagicSpells.ignoreCastItemTitle()) title = LegacyComponentSerializer.legacySection().serializeOrNull(WrittenBookHandler.getTitle(meta));
			if (!MagicSpells.ignoreCastItemAuthor()) author = LegacyComponentSerializer.legacySection().serializeOrNull(WrittenBookHandler.getAuthor(meta));
			if (!MagicSpells.ignoreCastItemEnchants()) enchants = meta.getEnchants();
			if (!MagicSpells.ignoreCastItemLore() && meta.hasLore()) {
				List<Component> itemLore = meta.lore();
				lore = itemLore.stream().map(LegacyComponentSerializer.legacySection()::serialize).collect(Collectors.toList());
			}
		}
	}

	public CastItem(String string) {
		MagicItemData data = MagicItems.getMagicItemDataFromString(string);
		if (data != null) {
			type = (Material) data.getAttribute(TYPE);
			if (isTypeValid()) {
				if (!MagicSpells.ignoreCastItemNames() && data.hasAttribute(NAME)) {
					if (MagicSpells.ignoreCastItemNameColors())
						name = PlainTextComponentSerializer.plainText().serialize((Component) data.getAttribute(NAME));
					else
						name = LegacyComponentSerializer.legacySection().serialize((Component) data.getAttribute(NAME));
				}

				if (!MagicSpells.ignoreCastItemAmount() && data.hasAttribute(AMOUNT))
					amount = (int) data.getAttribute(AMOUNT);

				if (!MagicSpells.ignoreCastItemDurability(type) && type.getMaxDurability() > 0 && data.hasAttribute(DURABILITY))
					durability = (int) data.getAttribute(DURABILITY);

				if (!MagicSpells.ignoreCastItemCustomModelData() && data.hasAttribute(CUSTOM_MODEL_DATA))
					customModelData = (int) data.getAttribute(CUSTOM_MODEL_DATA);

				if (!MagicSpells.ignoreCastItemItemModel() && data.hasAttribute(ITEM_MODEL))
					itemModel = (NamespacedKey) data.getAttribute(ITEM_MODEL);

				if (!MagicSpells.ignoreCastItemBreakability() && data.hasAttribute(UNBREAKABLE))
					unbreakable = (boolean) data.getAttribute(UNBREAKABLE);

				if (!MagicSpells.ignoreCastItemColor() && data.hasAttribute(COLOR))
					color = (Color) data.getAttribute(COLOR);

				if (!MagicSpells.ignoreCastItemPotionType() && data.hasAttribute(POTION_TYPE))
					potionType = (PotionType) data.getAttribute(POTION_TYPE);

				if (!MagicSpells.ignoreCastItemTitle() && data.hasAttribute(TITLE))
					title = LegacyComponentSerializer.legacySection().serialize((Component) data.getAttribute(TITLE));

				if (!MagicSpells.ignoreCastItemAuthor() && data.hasAttribute(AUTHOR))
					author = LegacyComponentSerializer.legacySection().serialize((Component) data.getAttribute(AUTHOR));

				if (!MagicSpells.ignoreCastItemEnchants() && data.hasAttribute(ENCHANTS))
					enchants = (Map<Enchantment, Integer>) data.getAttribute(ENCHANTS);

				if (!MagicSpells.ignoreCastItemLore() && data.hasAttribute(LORE)) {
					List<Component> itemLore = (List<Component>) data.getAttribute(LORE);
					lore = itemLore.stream().map(LegacyComponentSerializer.legacySection()::serialize).collect(Collectors.toList());
				}
			}
		}
	}

	public boolean isTypeValid() {
		return type != null && !type.isAir();
	}

	public Material getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof CastItem item) return equalsCastItem(item);
		if (o instanceof ItemStack item) return equalsCastItem(new CastItem(item));
		return false;
	}

	public boolean equalsCastItem(CastItem i) {
		if (i == null) return false;

		return type == i.type
			&& (MagicSpells.ignoreCastItemDurability(type) || durability == i.durability)
			&& (MagicSpells.ignoreCastItemAmount() || amount == i.amount)
			&& (MagicSpells.ignoreCastItemNames() || Objects.equals(name, i.name))
			&& (MagicSpells.ignoreCastItemCustomModelData() || customModelData == i.customModelData)
			&& (MagicSpells.ignoreCastItemItemModel() || Objects.equals(itemModel, i.itemModel))
			&& (MagicSpells.ignoreCastItemBreakability() || unbreakable == i.unbreakable)
			&& (MagicSpells.ignoreCastItemColor() || Objects.equals(color, i.color))
			&& (MagicSpells.ignoreCastItemPotionType() || Objects.equals(potionType, i.potionType))
			&& (MagicSpells.ignoreCastItemTitle() || Objects.equals(title, i.title))
			&& (MagicSpells.ignoreCastItemAuthor() || Objects.equals(author, i.author))
			&& (MagicSpells.ignoreCastItemEnchants() || Objects.equals(enchants, i.enchants))
			&& (MagicSpells.ignoreCastItemLore() || Objects.equals(lore, i.lore));
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, name, amount, durability, customModelData, itemModel, unbreakable, color, potionType, title, author, enchants, lore);
	}

	@Override
	public String toString() {
		if (type == null) return "";

		JsonObject castItem = new JsonObject();

		if (!MagicSpells.ignoreCastItemNames() && name != null)
			castItem.addProperty("name", name);

		if (!MagicSpells.ignoreCastItemAmount())
			castItem.addProperty("amount", amount);

		if (!MagicSpells.ignoreCastItemDurability(type) && type.getMaxDurability() > 0)
			castItem.addProperty("durability", durability);

		if (!MagicSpells.ignoreCastItemCustomModelData())
			castItem.addProperty("custommodeldata", customModelData);

		if (!MagicSpells.ignoreCastItemItemModel() && itemModel != null)
			castItem.addProperty("itemmodel", itemModel.asString());

		if (!MagicSpells.ignoreCastItemBreakability())
			castItem.addProperty("unbreakable", unbreakable);

		if (!MagicSpells.ignoreCastItemColor() && color != null)
			castItem.addProperty("color", Integer.toHexString(color.asRGB()));

		if (!MagicSpells.ignoreCastItemPotionType() && potionType != null) {
			String potionDataString = potionType.getKey().getKey();
			castItem.addProperty("potiondata", potionDataString);
		}

		if (!MagicSpells.ignoreCastItemTitle() && title != null)
			castItem.addProperty("title", title);

		if (!MagicSpells.ignoreCastItemAuthor() && author != null)
			castItem.addProperty("author", author);

		if (!MagicSpells.ignoreCastItemEnchants() && enchants != null) {
			JsonObject enchantsObject = new JsonObject();
			for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet())
				enchantsObject.addProperty(entry.getKey().getKey().getKey(), entry.getValue());

			castItem.add("enchants", enchantsObject);
		}

		if (!MagicSpells.ignoreCastItemLore() && lore != null) {
			JsonArray loreArray = new JsonArray(lore.size());
			for (String line : lore) loreArray.add(line);

			castItem.add("lore", loreArray);
		}

		return type.getKey().getKey() + castItem;
	}

}
