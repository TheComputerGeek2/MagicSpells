package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class ConjureBookSpell extends InstantSpell implements TargetedLocationSpell {

	private static final Pattern NAME_VARIABLE_PATTERN = Pattern.compile(Pattern.quote("{{name}}"));
	private static final Pattern DISPLAY_NAME_VARIABLE_PATTERN = Pattern.compile(Pattern.quote("{{disp}}"));

	private boolean openInstead;

	private ConfigData<Integer> pickupDelay;

	private boolean gravity;
	private boolean addToInventory;

	private String title;
	private String author;
	private List<String> pages;
	private List<String> lore;

	public ConjureBookSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		openInstead = getConfigBoolean("open-instead", false);

		pickupDelay = getConfigDataInt("pickup-delay", 0);

		gravity = getConfigBoolean("gravity", true);
		addToInventory = getConfigBoolean("add-to-inventory", true);

		title = getConfigString("title", "Book");
		author = getConfigString("author", "Steve");
		pages = getConfigStringList("pages", new ArrayList<>());
		lore = getConfigStringList("lore", new ArrayList<>());
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player player) {
			boolean added = false;
			ItemStack item = createBook(player, args);
			if (openInstead) player.openBook(item);
			else {
				if (addToInventory) {
					player.getEquipment().getItemInMainHand();
					if (BlockUtils.isAir(player.getEquipment().getItemInMainHand().getType())) {
						player.getEquipment().setItemInMainHand(item);
						added = true;
					} else added = Util.addToInventory(player.getInventory(), item, false, false);
				}
				if (!added) {
					Item dropped = player.getWorld().dropItem(player.getLocation(), item);
					dropped.setItemStack(item);
					dropped.setGravity(gravity);

					int delay = Math.max(pickupDelay.get(caster, null, power, args), 0);
					dropped.setPickupDelay(delay);

					playSpellEffects(EffectPosition.SPECIAL, dropped);
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		Player player = caster instanceof Player p ? p : null;

		ItemStack item = createBook(player, args);
		Item dropped = target.getWorld().dropItem(target, item);
		dropped.setItemStack(item);
		dropped.setGravity(gravity);

		int delay = Math.max(pickupDelay.get(caster, null, power, args), 0);
		dropped.setPickupDelay(delay);

		playSpellEffects(EffectPosition.SPECIAL, dropped);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return castAtLocation(null, target, power, args);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power, null);
	}

	private static Component createComponent(String raw, Player player, String displayName) {
		if (player != null) {
			raw = RegexUtil.replaceAll(NAME_VARIABLE_PATTERN, raw, player.getName());
			if (displayName != null) raw = RegexUtil.replaceAll(DISPLAY_NAME_VARIABLE_PATTERN, raw, displayName);
			raw = MagicSpells.doVariableReplacements(player, raw);
		}
		return Util.getMiniMessage(raw);
	}

	private ItemStack createBook(Player player, String[] args) {
		String playerDisplayName = null;
		if (player != null) playerDisplayName = Util.getStringFromComponent(player.displayName());

		String title = this.title;
		String author = this.author;
		List<String> lore = new ArrayList<>(this.lore);
		List<String> pages = new ArrayList<>(this.pages);

		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				title = title.replace("{{" + i + "}}", args[i]);
				author = author.replace("{{" + i + "}}", args[i]);
				for (int l = 0; l < lore.size(); l++) {
					lore.set(l, lore.get(l).replace("{{" + i + "}}", args[i]));
				}
				for (int p = 0; p < pages.size(); p++) {
					pages.set(p, pages.get(p).replace("{{" + i + "}}", args[i]));
				}
			}
		}

		List<Component> pagesRaw = new ArrayList<>();
		for (String page : pages) {
			pagesRaw.add(createComponent(page, player, playerDisplayName));
		}
		List<Component> loreRaw = new ArrayList<>();
		for (String line : lore) {
			loreRaw.add(createComponent(line, player, playerDisplayName));
		}

		ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta) ((BookMeta) item.getItemMeta())
				.title(createComponent(title, player, playerDisplayName))
				.author(createComponent(author, player, playerDisplayName))
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

	public boolean isOpenInstead() {
		return openInstead;
	}

	public void setOpenInstead(boolean openInstead) {
		this.openInstead = openInstead;
	}

	public boolean hasGravity() {
		return gravity;
	}

	public void setGravity(boolean gravity) {
		this.gravity = gravity;
	}

	public boolean shouldAddToInventory() {
		return addToInventory;
	}

	public void setAddToInventory(boolean addToInventory) {
		this.addToInventory = addToInventory;
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
