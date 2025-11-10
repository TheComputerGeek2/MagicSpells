package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.EntityEquipment;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class ConjureBookSpell extends InstantSpell implements TargetedLocationSpell {

	private static final Pattern NAME_VARIABLE_PATTERN = Pattern.compile(Pattern.quote("{{name}}"));
	private static final Pattern DISPLAY_NAME_VARIABLE_PATTERN = Pattern.compile(Pattern.quote("{{disp}}"));

	private ConfigData<Boolean> openInstead;

	private ConfigData<Integer> pickupDelay;

	private ConfigData<Boolean> gravity;
	private ConfigData<Boolean> addToInventory;

	private String title;
	private String author;
	private List<String> pages;
	private List<String> lore;

	public ConjureBookSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		openInstead = getConfigDataBoolean("open-instead", false);

		pickupDelay = getConfigDataInt("pickup-delay", 0);

		gravity = getConfigDataBoolean("gravity", true);
		addToInventory = getConfigDataBoolean("add-to-inventory", true);

		title = getConfigString("title", "Book");
		author = getConfigString("author", "Steve");
		pages = getConfigStringList("pages", new ArrayList<>());
		lore = getConfigStringList("lore", new ArrayList<>());
	}

	@Override
	public CastResult cast(SpellData data) {
		Player player = data.caster() instanceof Player p ? p : null;

		boolean openInstead = this.openInstead.get(data);
		if (openInstead && player == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		ItemStack book = createBook(player, data);
		if (openInstead) {
			player.openBook(book);
			playSpellEffects(data);

			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		boolean added = false;
		if (addToInventory.get(data)) {
			EntityEquipment eq = data.caster().getEquipment();
			if (eq != null && eq.getItemInMainHand().getType().isAir()) {
				eq.setItemInMainHand(book);
				added = true;
			} else if (player != null) {
				added = Util.addToInventory(player.getInventory(), book, false, false);
			}
		}

		if (!added) {
			Item dropped = data.caster().getWorld().dropItem(data.caster().getLocation(), book, item -> {
				item.setPickupDelay(Math.max(pickupDelay.get(data), 0));
				item.setGravity(gravity.get(data));
			});

			playSpellEffects(EffectPosition.SPECIAL, dropped, data);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Player player = data.caster() instanceof Player p ? p : null;

		ItemStack book = createBook(player, data);
		Location location = data.location();

		Item dropped = location.getWorld().dropItem(location, book);
		dropped.setPickupDelay(Math.max(pickupDelay.get(data), 0));
		dropped.setGravity(gravity.get(data));

		playSpellEffects(data);
		playSpellEffects(EffectPosition.SPECIAL, dropped, data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private static Component createComponent(String raw, Player player, String displayName, SpellData data) {
		if (player != null) {
			raw = NAME_VARIABLE_PATTERN.matcher(raw).replaceAll(player.getName());
			if (displayName != null) raw = DISPLAY_NAME_VARIABLE_PATTERN.matcher(raw).replaceAll(displayName);
			raw = MagicSpells.doReplacements(raw, player, data);
		}
		return Util.getItemMiniMessage(raw);
	}

	private ItemStack createBook(Player player, SpellData data) {
		String playerDisplayName = null;
		if (player != null) playerDisplayName = Util.getStringFromComponent(player.displayName());

		String title = this.title;
		String author = this.author;
		List<String> lore = new ArrayList<>(this.lore);
		List<String> pages = new ArrayList<>(this.pages);

		if (data.hasArgs()) {
			for (int i = 0; i < data.args().length; i++) {
				title = title.replace("{{" + i + "}}", data.args()[i]);
				author = author.replace("{{" + i + "}}", data.args()[i]);
				for (int l = 0; l < lore.size(); l++) {
					lore.set(l, lore.get(l).replace("{{" + i + "}}", data.args()[i]));
				}
				for (int p = 0; p < pages.size(); p++) {
					pages.set(p, pages.get(p).replace("{{" + i + "}}", data.args()[i]));
				}
			}
		}

		List<Component> pagesRaw = new ArrayList<>();
		for (String page : pages) {
			pagesRaw.add(createComponent(page, player, playerDisplayName, data));
		}
		List<Component> loreRaw = new ArrayList<>();
		for (String line : lore) {
			loreRaw.add(createComponent(line, player, playerDisplayName, data));
		}

		ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta) ((BookMeta) item.getItemMeta())
			.title(createComponent(title, player, playerDisplayName, data))
			.author(createComponent(author, player, playerDisplayName, data))
			.pages(pagesRaw);
		meta.lore(loreRaw);
		item.setItemMeta(meta);
		return item;
	}

	public static Pattern getNameVariablePattern() {
		return NAME_VARIABLE_PATTERN;
	}

	public static Pattern getDisplayNameVariablePattern() {
		return DISPLAY_NAME_VARIABLE_PATTERN;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public List<String> getPages() {
		return pages;
	}

	public void setPages(List<String> pages) {
		this.pages = pages;
	}

	public List<String> getLore() {
		return lore;
	}

	public void setLore(List<String> lore) {
		this.lore = lore;
	}

}
