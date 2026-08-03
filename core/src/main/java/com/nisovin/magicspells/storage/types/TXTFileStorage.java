package com.nisovin.magicspells.storage.types;

import java.io.File;
import java.io.Writer;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import java.util.Set;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;

import java.nio.charset.StandardCharsets;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.storage.StorageHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;

public class TXTFileStorage extends StorageHandler {

	public TXTFileStorage(MagicSpells plugin) {
		super(plugin);
	}

	@Override
	public void initialize() {

	}

	@Override
	public void load(Spellbook spellbook) {
		Player pl = spellbook.getPlayer();
		String worldName = pl.getWorld().getName();
		String id = Util.getUniqueId(pl);
		try {
			MagicSpells.debug("  ...retrieving files...");
			File file;
			String path = "spellbooks" + File.separator;
			if (MagicSpells.arePlayerSpellsSeparatedPerWorld()) {
				File folder = new File(plugin.getDataFolder(), path + worldName);
				if (!folder.exists()) folder.mkdir();
				file = new File(plugin.getDataFolder(), path + worldName + File.separator + id + ".txt");
			} else file = new File(plugin.getDataFolder(), path + id + ".txt");

			if (!file.exists()) return;

			String line;
			Spell spell;
			String[] data;
			List<CastItem> items;
			String[] split;
			CastItem castItem;
			Scanner scanner = new Scanner(file, StandardCharsets.UTF_8);
			while (scanner.hasNext()) {
				line = scanner.nextLine();
				if (line.isEmpty()) continue;

				if (!line.contains(":")) {
					spell = MagicSpells.getSpellByInternalName(line);
					if (spell == null) continue;
					spellbook.addSpell(spell);
				}

				data = line.split(":", 2);
				spell = MagicSpells.getSpellByInternalName(data[0]);

				if (spell == null) continue;
				if (data.length == 1) continue;

				items = new ArrayList<>();
				split = data[1].split(MagicItemDataParser.DATA_REGEX);
				for (String value : split) {
					try {
						castItem = new CastItem(value);
						items.add(castItem);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				spellbook.addSpell(spell, items.toArray(new CastItem[0]));
			}
			scanner.close();

		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
		}
	}

	@Override
	public void save(Spellbook spellbook) {
		Player pl = spellbook.getPlayer();
		String worldName = pl.getWorld().getName();
		String id = Util.getUniqueId(pl);
		try {
			File file;
			String path = "spellbooks" + File.separator;
			if (MagicSpells.arePlayerSpellsSeparatedPerWorld()) {
				File folder = new File(plugin.getDataFolder(), path + worldName);
				if (!folder.exists()) folder.mkdirs();
				file = new File(plugin.getDataFolder(), path + worldName + File.separator + id + ".txt");
			} else file = new File(plugin.getDataFolder(), path + id + ".txt");

			Set<CastItem> items;
			StringBuilder builder;
			CastItem castItem;
			Writer writer = new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8);
			for (Spell spell : spellbook.getSpells()) {
				writer.append(spell.getInternalName());

				if (spellbook.getCustomBindings().containsKey(spell)) {
					items = spellbook.getCustomBindings().get(spell);
					builder = new StringBuilder();
					for (CastItem item : items) {
						builder.append((builder.isEmpty()) ? "" : "|").append(item);
					}

					// When you unbind an item with no binds left, restore the original cast item.
					castItem = (CastItem) items.toArray()[0];
					if (items.size() == 1 && castItem.getType() == null) {
						writer.write("\n");
						continue;
					}
					writer.append(":").append(builder.toString());
				}
				writer.write("\n");
			}

			writer.flush();
			writer.close();
			MagicSpells.debug("Saved spellbook for player '" + pl.getName() + "'.");
		} catch (Exception e) {
			plugin.getLogger().severe("Error saving spellbook for player '" + pl.getName() + "'.");
			e.printStackTrace();
		}
	}

	@Override
	public void disable() {

	}

}
