package com.nisovin.magicjutsus.jutsus.instant;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.RegexUtil;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class ConjureBookJutsu extends InstantJutsu implements TargetedLocationJutsu {

	private static final Pattern NAME_VARIABLE_PATTERN = Pattern.compile(Pattern.quote("{{name}}"));
	private static final Pattern DISPLAY_NAME_VARIABLE_PATTERN = Pattern.compile(Pattern.quote("{{disp}}"));

	private int pickupDelay;

	private boolean gravity;
	private boolean addToInventory;

	private ItemStack book;

	public ConjureBookJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		pickupDelay = getConfigInt("pickup-delay", 0);
		pickupDelay = Math.max(pickupDelay, 0);

		gravity = getConfigBoolean("gravity", true);
		addToInventory = getConfigBoolean("add-to-inventory", true);

		String title = getConfigString("title", "Book");
		String author = getConfigString("author", "Steve");
		List<String> pages = getConfigStringList("pages", null);
		List<String> lore = getConfigStringList("lore", null);

		book = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta) book.getItemMeta();
		meta.setTitle(ChatColor.translateAlternateColorCodes('&', title));
		meta.setAuthor(ChatColor.translateAlternateColorCodes('&', author));
		if (pages != null) {
			for (int i = 0; i < pages.size(); i++) {
				pages.set(i, ChatColor.translateAlternateColorCodes('&', pages.get(i)));
			}
			meta.setPages(pages);
		}
		if (lore != null) {
			for (int i = 0; i < lore.size(); i++) {
				lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
			}
			meta.setLore(lore);
		}
		book.setItemMeta(meta);
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			boolean added = false;
			ItemStack item = getBook(player, args);
			if (addToInventory) {
				if (player.getEquipment().getItemInMainHand() == null || BlockUtils.isAir(player.getEquipment().getItemInMainHand().getType())) {
					player.getEquipment().setItemInMainHand(item);
					added = true;
				} else added = Util.addToInventory(player.getInventory(), item, false, false);
			}
			if (!added) {
				Item dropped = player.getWorld().dropItem(player.getLocation(), item);
				dropped.setItemStack(item);
				dropped.setPickupDelay(pickupDelay);
				dropped.setGravity(gravity);
				playJutsuEffects(EffectPosition.SPECIAL, dropped);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		ItemStack item = book.clone();
		Item dropped = target.getWorld().dropItem(target, item);
		dropped.setItemStack(item);
		dropped.setPickupDelay(pickupDelay);
		dropped.setGravity(gravity);
		playJutsuEffects(EffectPosition.SPECIAL, dropped);
		return true;
	}

	private ItemStack getBook(Player player, String[] args) {
		ItemStack item = book.clone();
		BookMeta meta = (BookMeta) item.getItemMeta();
		String title = meta.getTitle();
		String author = meta.getAuthor();
		List<String> lore = null;
		if (meta.getLore() != null) lore = new ArrayList<>(meta.getLore());
		List<String> pages = new ArrayList<>(meta.getPages());

		if (player != null) {
			String playerName = player.getName();
			String playerDisplayName = player.getDisplayName();

			title = applyVariables(title, playerName, playerDisplayName);
			author = applyVariables(author, playerName, playerDisplayName);
			if (lore != null && !lore.isEmpty()) {
				for (int l = 0; l < lore.size(); l++) {
					lore.set(l, applyVariables(lore.get(l), playerName, playerDisplayName));
				}
			}
			if (pages != null && !pages.isEmpty()) {
				for (int p = 0; p < pages.size(); p++) {
					pages.set(p, applyVariables(pages.get(p), playerName, playerDisplayName));
				}
			}
		}

		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				title = title.replace("{{" + i + "}}", args[i]);
				author = author.replace("{{" + i + "}}", args[i]);
				if (lore != null && !lore.isEmpty()) {
					for (int l = 0; l < lore.size(); l++) {
						lore.set(l, lore.get(l).replace("{{" + i + "}}", args[i]));
					}
				}
				if (pages != null && !pages.isEmpty()) {
					for (int p = 0; p < pages.size(); p++) {
						pages.set(p, pages.get(p).replace("{{" + i + "}}", args[i]));
					}
				}
			}
		}

		meta.setTitle(title);
		meta.setAuthor(author);
		meta.setLore(lore);
		meta.setPages(pages);
		item.setItemMeta(meta);
		return item;
	}
	
	private static String applyVariables(String raw, String playerName, String displayName) {
		raw = RegexUtil.replaceAll(NAME_VARIABLE_PATTERN, raw, playerName);
		raw = RegexUtil.replaceAll(DISPLAY_NAME_VARIABLE_PATTERN, raw, displayName);
		return raw;
	}
	
}
