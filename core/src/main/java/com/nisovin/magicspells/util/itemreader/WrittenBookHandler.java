package com.nisovin.magicspells.util.itemreader;

import java.util.List;
import java.util.ArrayList;

import net.kyori.adventure.text.Component;

import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.PAGES;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.TITLE;
import static com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute.AUTHOR;

public class WrittenBookHandler {

	private static final String AUTHOR_CONFIG_NAME = AUTHOR.toString();
	private static final String PAGES_CONFIG_NAME = PAGES.toString();
	private static final String TITLE_CONFIG_NAME = TITLE.toString();

	public static void process(ConfigurationSection config, ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof BookMeta bookMeta)) return;

		if (config.isString(TITLE_CONFIG_NAME)) {
			Component title = Util.getItemMiniMessage(config.getString(TITLE_CONFIG_NAME));

			bookMeta.title(title);
			data.setAttribute(TITLE, bookMeta.title());
		}

		if (config.isString(AUTHOR_CONFIG_NAME)) {
			Component author = Util.getItemMiniMessage(config.getString(AUTHOR_CONFIG_NAME));

			bookMeta.author(author);
			data.setAttribute(AUTHOR, bookMeta.author());
		}

		if (config.isList(PAGES_CONFIG_NAME)) {
			List<Component> pages = new ArrayList<>();
			for (String page : config.getStringList(PAGES_CONFIG_NAME)) pages.add(Util.getItemMiniMessage(page));

			if (pages.isEmpty()) return;

			bookMeta.pages(pages);
			data.setAttribute(PAGES, bookMeta.pages());
		}
	}

	public static void processItemMeta(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof BookMeta bookMeta)) return;

		if (data.hasAttribute(TITLE)) bookMeta.title((Component) data.getAttribute(TITLE));
		if (data.hasAttribute(AUTHOR)) bookMeta.author((Component) data.getAttribute(AUTHOR));
		if (data.hasAttribute(PAGES)) bookMeta.pages((List<Component>) data.getAttribute(PAGES));
	}

	public static void processMagicItemData(ItemMeta meta, MagicItemData data) {
		if (!(meta instanceof BookMeta bookMeta)) return;

		if (bookMeta.hasAuthor()) data.setAttribute(AUTHOR, bookMeta.author());
		if (bookMeta.hasTitle()) data.setAttribute(TITLE, bookMeta.title());
		if (bookMeta.hasPages()) data.setAttribute(PAGES, bookMeta.pages());
	}

	public static Component getTitle(ItemMeta meta) {
		return meta instanceof BookMeta bookMeta ? bookMeta.title() : null;
	}

	public static Component getAuthor(ItemMeta meta) {
		return meta instanceof BookMeta bookMeta ? bookMeta.author() : null;
	}
	
}
