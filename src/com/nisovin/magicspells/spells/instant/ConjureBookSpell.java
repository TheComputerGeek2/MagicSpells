package com.nisovin.magicspells.spells.instant;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

// the special position plays on the dropped items
// replaces {{name}} with the user's username
// replaces {{disp}} with the user's display name
// replaces {{<integer>}} with the argument at index <integer>
public class ConjureBookSpell extends InstantSpell implements TargetedLocationSpell {

	boolean addToInventory;
	ItemStack book;
	private boolean projectileHasGravity;
	
	public ConjureBookSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		addToInventory = getConfigBoolean("add-to-inventory", true);
		
		String title = getConfigString("title", "Book");
		String author = getConfigString("author", "Steve");
		List<String> pages = getConfigStringList("pages", null);
		List<String> lore = getConfigStringList("lore", null);
		projectileHasGravity = getConfigBoolean("gravity", true);
		
		book = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta)book.getItemMeta();
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
	
	private ItemStack getBook(Player player, String[] args) {
		ItemStack item = book.clone();
		BookMeta meta = (BookMeta)item.getItemMeta();
		String title = meta.getTitle();
		String author = meta.getAuthor();
		List<String> lore = meta.getLore();
		List<String> pages = meta.getPages();
		if (player != null) {
			title = title.replace("{{name}}", player.getName()).replace("{{disp}}", player.getDisplayName());
			author = author.replace("{{name}}", player.getName()).replace("{{disp}}", player.getDisplayName());
			if (lore != null && lore.size() > 0) {
				for (int l = 0; l < lore.size(); l++) {
					lore.set(l, lore.get(l).replace("{{name}}", player.getName()).replace("{{disp}}", player.getDisplayName()));
				}
			}
			if (pages != null && pages.size() > 0) {
				for (int p = 0; p < pages.size(); p++) {
					pages.set(p, pages.get(p).replace("{{name}}", player.getName()).replace("{{disp}}", player.getDisplayName()));
				}
			}
		}
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				title = title.replace("{{"+i+"}}", args[i]);
				author = author.replace("{{"+i+"}}", args[i]);
				if (lore != null && lore.size() > 0) {
					for (int l = 0; l < lore.size(); l++) {
						lore.set(l, lore.get(l).replace("{{"+i+"}}", args[i]));
					}
				}
				if (pages != null && pages.size() > 0) {
					for (int p = 0; p < pages.size(); p++) {
						pages.set(p, pages.get(p).replace("{{"+i+"}}", args[i]));
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
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			boolean added = false;
			ItemStack item = getBook(player, args);
			if (addToInventory) {
				if (MagicSpells.getVolatileCodeHandler().getItemInMainHand(player) == null || MagicSpells.getVolatileCodeHandler().getItemInMainHand(player).getType() == Material.AIR) {
					MagicSpells.getVolatileCodeHandler().setItemInMainHand(player, item);
					added = true;
				} else {
					added = Util.addToInventory(player.getInventory(), item, false, false);
				}
			}
			if (!added) {
				Item dropped = player.getWorld().dropItem(player.getLocation(), item);
				dropped.setItemStack(item);
				dropped.setGravity(projectileHasGravity);
				playSpellEffects(EffectPosition.SPECIAL, dropped);
				//player.getWorld().dropItem(player.getLocation(), item).setItemStack(item);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		return castAtLocation(target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		ItemStack item = book.clone();
		Item dropped = target.getWorld().dropItem(target, item);
		dropped.setItemStack(item);
		dropped.setGravity(projectileHasGravity);
		playSpellEffects(EffectPosition.SPECIAL, dropped);
		//target.getWorld().dropItem(target, item).setItemStack(item);
		return true;
	}
	
}
