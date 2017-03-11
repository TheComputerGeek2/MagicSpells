package com.nisovin.magicspells;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.nisovin.magicspells.events.SpellSelectionChangedEvent;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.Util;

public class Spellbook {
	private File spellbookDir;
	private MagicSpells plugin;

	private Player player;
	private String playerName;
	private String uniqueId;

	private TreeSet<Spell> allSpells = new TreeSet<Spell>() {

		private static final long serialVersionUID = 1L;

		@Override
		public boolean remove(Object o) {
			boolean ret = super.remove(o);
			if (o != null && o instanceof Spell) {
				Spell s = (Spell) o;
				s.unloadPlayerEffectTracker(player);
			}
			return ret;
		}

		@Override
		public boolean add(Spell s) {
			boolean ret = super.add(s);
			s.initializePlayerEffectTracker(player);
			return ret;
		}

		@Override
		public void clear() {
			for (Spell s : this) {
				s.unloadPlayerEffectTracker(player);
			}
			super.clear();
		}

	};
	private HashMap<CastItem, ArrayList<Spell>> itemSpells = new HashMap<CastItem, ArrayList<Spell>>();
	private HashMap<CastItem, Integer> activeSpells = new HashMap<CastItem, Integer>();
	private HashMap<Spell, Set<CastItem>> customBindings = new HashMap<Spell, Set<CastItem>>();
	private HashMap<Plugin, Set<Spell>> temporarySpells = new HashMap<Plugin, Set<Spell>>();
	private Set<String> cantLearn = new HashSet<String>();

	public Spellbook(Player player, MagicSpells plugin) {
		this.plugin = plugin;
		this.player = player;
		this.playerName = player.getName();
		this.uniqueId = Util.getUniqueId(player);

		MagicSpells.debug(1, "Loading player spell list: " + playerName);
		load();
	}

	public void destroy() {
		allSpells.clear();
		itemSpells.clear();
		activeSpells.clear();
		customBindings.clear();
		temporarySpells.clear();
		cantLearn.clear();
		player = null;
		playerName = null;
	}

	public void load() {
		load(player.getWorld());
	}

	public void load(World playerWorld) {
		// load spells from file
		loadFromFile(playerWorld);

		// give all spells to ops, or if ignoring grant perms
		if (plugin.ignoreGrantPerms || (player.isOp() && plugin.opsHaveAllSpells)) {
			MagicSpells.debug(2, "  Op, granting all spells...");
			for (Spell spell : plugin.spellsOrdered) {
				if (!spell.isHelperSpell() && !allSpells.contains(spell)) {
					addSpell(spell);
				}
			}
		}

		// add spells granted by permissions
		if (!plugin.ignoreGrantPerms) {
			addGrantedSpells();
		}

		// sort spells or pre-select if just one
		for (CastItem i : itemSpells.keySet()) {
			ArrayList<Spell> spells = itemSpells.get(i);
			if (spells.size() == 1 && !plugin.allowCycleToNoSpell) {
				activeSpells.put(i, 0);
			} else {
				Collections.sort(spells);
			}
		}
	}

	private void loadFromFile(World playerWorld) {
		try {
			MagicSpells.debug(2, "  Loading spells from player file...");
			if (spellbookDir == null)
				spellbookDir = new File(plugin.getDataFolder(), "spellbooks");
			File file;
			if (plugin.separatePlayerSpellsPerWorld) {
				File folder = new File(spellbookDir, player.getWorld().getName());
				if (!folder.exists()) {
					folder.mkdir();
				}
				file = new File(folder, uniqueId + ".txt");
				if (!file.exists()) {
					File file2 = new File(folder, playerName.toLowerCase() + ".txt");
					if (file2.exists()) {
						file2.renameTo(file);
					}
				}
			} else {
				file = new File(spellbookDir, uniqueId + ".txt");
				if (!file.exists()) {
					File file2 = new File(spellbookDir, playerName.toLowerCase() + ".txt");
					if (file2.exists()) {
						file2.renameTo(file);
					}
				}
			}
			if (file.exists()) {
				Scanner scanner = new Scanner(file);
				while (scanner.hasNext()) {
					String line = scanner.nextLine();
					if (!line.equals("")) {
						if (!line.contains(":")) {
							Spell spell = MagicSpells.getSpellByInternalName(line);
							if (spell != null) {
								addSpell(spell);
							}
						} else {
							String[] data = line.split(":", 2);
							Spell spell = MagicSpells.getSpellByInternalName(data[0]);
							if (spell != null) {
								ArrayList<CastItem> items = new ArrayList<CastItem>();
								String[] s = data[1].split(",");
								for (int i = 0; i < s.length; i++) {
									try {
										CastItem castItem = new CastItem(s[i]);
										items.add(castItem);
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
								addSpell(spell, items.toArray(new CastItem[items.size()]));
							}
						}
					}
				}
				scanner.close();
			}
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
		}
	}

	public void addGrantedSpells() {
		MagicSpells.debug(2, "  Adding granted spells...");
		boolean added = false;
		for (Spell spell : plugin.spellsOrdered) {
			MagicSpells.debug(3, "    Checking spell " + spell.getInternalName() + "...");
			if (!spell.isHelperSpell() && !hasSpell(spell, false)) {
				if (spell.isAlwaysGranted() || player.hasPermission("magicspells.grant." + spell.getPermissionName())) {
					addSpell(spell);
					added = true;
				}
			}
		}
		if (added) {
			save();
		}
	}

	public boolean canLearn(Spell spell) {
		if (spell.isHelperSpell()) {
			MagicSpells.debug("Cannot learn " + spell.getName() + " because it is a helper spell");
			return false;
		}
		if (cantLearn.contains(spell.getInternalName().toLowerCase())) {
			MagicSpells.debug("Cannot learn " + spell.getName() + " because another spell precludes it.");
			return false;
		}
		if (spell.prerequisites != null) {
			for (String spellName : spell.prerequisites) {
				Spell sp = MagicSpells.getSpellByInternalName(spellName);
				if (sp == null || !hasSpell(sp)) {
					MagicSpells.debug("Cannot learn " + spell.getName() + " because the prerequisite of " + spellName
							+ " has not been satisfied");
					return false;
				}
			}
		}
		if (spell.xpRequired != null) {
			MagicXpHandler handler = MagicSpells.getMagicXpHandler();
			if (handler != null) {
				for (String school : spell.xpRequired.keySet()) {
					if (handler.getXp(player, school) < spell.xpRequired.get(school)) {
						MagicSpells.debug("Cannot learn " + spell.getName()
								+ " because the target does not have enough magic xp");
						return false;
					}
				}
			}
		}
		MagicSpells.debug("Checking learn permissions for " + player.getName());
		return player.hasPermission("magicspells.learn." + spell.getPermissionName());
	}

	public boolean canCast(Spell spell) {
		if (spell.isHelperSpell())
			return true;
		return plugin.ignoreCastPerms || player.hasPermission("magicspells.cast." + spell.getPermissionName());
	}

	public boolean canTeach(Spell spell) {
		if (spell.isHelperSpell())
			return false;
		return player.hasPermission("magicspells.teach." + spell.getPermissionName());
	}

	public boolean hasAdvancedPerm(String spell) {
		return player.hasPermission("magicspells.advanced." + spell);
	}

	public Spell getSpellByName(String spellName) {
		Spell spell = MagicSpells.getSpellByInGameName(spellName);
		if (spell != null && hasSpell(spell)) {
			return spell;
		} else {
			return null;
		}
	}

	public Set<Spell> getSpells() {
		return this.allSpells;
	}

	public List<String> tabComplete(String partial) {
		String[] data = Util.splitParams(partial, 2);
		if (data.length == 1) {
			// complete spell name
			partial = data[0].toLowerCase(); // TODO find an alternative to
												// reassigning the parameter
			List<String> options = new ArrayList<String>();
			for (Spell spell : allSpells) {
				if (spell.canCastByCommand() && !spell.isHelperSpell()) {
					if (spell.getName().toLowerCase().startsWith(partial)) {
						options.add(spell.getName());
					} else {
						String[] aliases = spell.getAliases();
						if (aliases != null && aliases.length > 0) {
							for (String alias : aliases) {
								if (alias.toLowerCase().startsWith(partial)) {
									options.add(alias);
								}
							}
						}
					}
				}
			}
			if (options.size() > 0) {
				return options;
			} else {
				return null;
			}
		} else {
			// complete spell params
			Spell spell = getSpellByName(data[0]);
			if (spell == null) {
				return null;
			} else {
				List<String> ret = spell.tabComplete(player, data[1]);
				if (ret == null || ret.size() == 0) {
					return null;
				} else {
					return ret;
				}
			}
		}
	}

	protected CastItem getCastItemForCycling(ItemStack item) {
		CastItem castItem;
		if (item != null) {
			castItem = new CastItem(item);
		} else {
			castItem = new CastItem(0);
		}
		ArrayList<Spell> spells = itemSpells.get(castItem);
		if (spells != null && (spells.size() > 1 || (spells.size() == 1 && plugin.allowCycleToNoSpell))) {
			return castItem;
		} else {
			return null;
		}
	}

	protected Spell nextSpell(ItemStack item) {
		CastItem castItem = getCastItemForCycling(item);
		if (castItem != null) {
			return nextSpell(castItem);
		}
		return null;
	}

	protected Spell nextSpell(CastItem castItem) {
		Integer i = activeSpells.get(castItem); // get the index of the active
												// spell for the cast item
		if (i != null) {
			ArrayList<Spell> spells = itemSpells.get(castItem); // get all the
																// spells for
																// the cast item
			if (spells.size() > 1 || i.equals(-1) || plugin.allowCycleToNoSpell || plugin.alwaysShowMessageOnCycle) {
				int count = 0;
				while (count++ < spells.size()) {
					i++;
					if (i >= spells.size()) {
						if (plugin.allowCycleToNoSpell) {
							activeSpells.put(castItem, -1);
							Bukkit.getPluginManager()
									.callEvent(new SpellSelectionChangedEvent(null, player, castItem, this));
							MagicSpells.sendMessage(plugin.strSpellChangeEmpty, player, MagicSpells.NULL_ARGS);
							return null;
						} else {
							i = 0;
						}
					}
					if (!plugin.onlyCycleToCastableSpells || canCast(spells.get(i))) {
						activeSpells.put(castItem, i);
						Bukkit.getPluginManager()
								.callEvent(new SpellSelectionChangedEvent(spells.get(i), player, castItem, this));
						return spells.get(i);
					}
				}
				return null;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	protected Spell prevSpell(ItemStack item) {
		CastItem castItem = getCastItemForCycling(item);
		if (castItem != null) {
			return prevSpell(castItem);
		}
		return null;
	}

	protected Spell prevSpell(CastItem castItem) {
		Integer i = activeSpells.get(castItem); // get the index of the active
												// spell for the cast item
		if (i != null) {
			ArrayList<Spell> spells = itemSpells.get(castItem); // get all the
																// spells for
																// the cast item
			if (spells.size() > 1 || i.equals(-1) || plugin.allowCycleToNoSpell) {
				int count = 0;
				while (count++ < spells.size()) {
					i--;
					if (i < 0) {
						if (plugin.allowCycleToNoSpell && i == -1) {
							activeSpells.put(castItem, -1);
							Bukkit.getPluginManager()
									.callEvent(new SpellSelectionChangedEvent(null, player, castItem, this));
							MagicSpells.sendMessage(plugin.strSpellChangeEmpty, player, MagicSpells.NULL_ARGS);
							return null;
						} else {
							i = spells.size() - 1;
						}
					}
					if (!plugin.onlyCycleToCastableSpells || canCast(spells.get(i))) {
						activeSpells.put(castItem, i);
						Bukkit.getPluginManager()
								.callEvent(new SpellSelectionChangedEvent(spells.get(i), player, castItem, this));
						return spells.get(i);
					}
				}
				return null;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public Spell getActiveSpell(ItemStack item) {
		CastItem castItem = new CastItem(item);
		return getActiveSpell(castItem);
	}

	public Spell getActiveSpell(CastItem castItem) {
		Integer i = activeSpells.get(castItem);
		if (i != null && i != -1) {
			return itemSpells.get(castItem).get(i);
		} else {
			return null;
		}
	}

	public boolean hasSpell(Spell spell) {
		return hasSpell(spell, true);
	}

	public boolean hasSpell(Spell spell, boolean checkGranted) {
		if (plugin.ignoreGrantPerms)
			return true;
		boolean has = allSpells.contains(spell);
		if (has) {
			return true;
		} else if (checkGranted && player.hasPermission("magicspells.grant." + spell.getPermissionName())) {
			MagicSpells.debug(2, "Adding granted spell for " + player.getName() + ": " + spell.getName());
			addSpell(spell);
			save();
			return true;
		} else if (MagicSpells.plugin.enableTempGrantPerms
				&& player.hasPermission("magicspells.tempgrant." + spell.getPermissionName())) {
			return true;
		} else {
			return false;
		}
	}

	public void addSpell(Spell spell) {
		addSpell(spell, (CastItem[]) null);
	}

	public void addSpell(Spell spell, CastItem castItem) {
		addSpell(spell, new CastItem[] { castItem });
	}

	public void addSpell(Spell spell, CastItem[] castItems) {
		if (spell == null)
			return;
		MagicSpells.debug(3, "    Added spell: " + spell.getInternalName());
		allSpells.add(spell);
		if (spell.canCastWithItem()) {
			CastItem[] items = spell.getCastItems();
			if (castItems != null && castItems.length > 0) {
				items = castItems;
				HashSet<CastItem> set = new HashSet<CastItem>();
				for (CastItem item : items) {
					if (item != null) {
						set.add(item);
					}
				}
				customBindings.put(spell, set);
			} else if (plugin.ignoreDefaultBindings) {
				return; // no cast item provided and ignoring default, so just
						// stop here
			}
			for (CastItem i : items) {
				MagicSpells.debug(3, "        Cast item: " + i + (castItems != null ? " (custom)" : " (default)"));
				if (i != null) {
					ArrayList<Spell> temp = itemSpells.get(i);
					if (temp != null) {
						temp.add(spell);
					} else {
						temp = new ArrayList<Spell>();
						temp.add(spell);
						itemSpells.put(i, temp);
						activeSpells.put(i, plugin.allowCycleToNoSpell ? -1 : 0);
					}
				}
			}
		}
		// remove any spells that this spell replaces
		if (spell.replaces != null) {
			for (String spellName : spell.replaces) {
				Spell sp = MagicSpells.getSpellByInternalName(spellName);
				if (sp != null) {
					MagicSpells.debug(3, "        Removing replaced spell: " + sp.getInternalName());
					removeSpell(sp);
				}
			}
		}
		// prevent learning of spells this spell precludes
		if (spell.precludes != null) {
			for (String s : spell.precludes) {
				cantLearn.add(s.toLowerCase());
			}
		}
	}

	public void removeSpell(Spell spell) {
		if (spell instanceof BuffSpell) {
			((BuffSpell) spell).turnOff(player);
		}
		CastItem[] items = spell.getCastItems();
		if (customBindings.containsKey(spell)) {
			items = customBindings.remove(spell).toArray(new CastItem[] {});
		}
		for (CastItem item : items) {
			if (item != null) {
				ArrayList<Spell> temp = itemSpells.get(item);
				if (temp != null) {
					temp.remove(spell);
					if (temp.size() == 0) {
						itemSpells.remove(item);
						activeSpells.remove(item);
					} else {
						activeSpells.put(item, -1);
					}
				}
			}
		}
		allSpells.remove(spell);
	}

	public void addTemporarySpell(Spell spell, Plugin plugin) {
		if (!hasSpell(spell)) {
			addSpell(spell);
			Set<Spell> temps = temporarySpells.get(plugin);
			if (temps == null) {
				temps = new HashSet<Spell>();
				temporarySpells.put(plugin, temps);
			}
			temps.add(spell);
		}
	}

	public void removeTemporarySpells(Plugin plugin) {
		Set<Spell> temps = temporarySpells.remove(plugin);
		if (temps != null) {
			for (Spell spell : temps) {
				removeSpell(spell);
			}
		}
	}

	private boolean isTemporary(Spell spell) {
		for (Set<Spell> temps : temporarySpells.values()) {
			if (temps.contains(spell)) {
				return true;
			}
		}
		return false;
	}

	public void addCastItem(Spell spell, CastItem castItem) {
		// add to custom bindings
		Set<CastItem> bindings = customBindings.get(spell);
		if (bindings == null) {
			bindings = new HashSet<CastItem>();
			customBindings.put(spell, bindings);
		}
		if (!bindings.contains(castItem)) {
			bindings.add(castItem);
		}

		// add to item bindings
		ArrayList<Spell> bindList = itemSpells.get(castItem);
		if (bindList == null) {
			bindList = new ArrayList<Spell>();
			itemSpells.put(castItem, bindList);
			activeSpells.put(castItem, plugin.allowCycleToNoSpell ? -1 : 0);
		}
		bindList.add(spell);
	}

	public boolean removeCastItem(Spell spell, CastItem castItem) {
		boolean removed = false;

		// remove from custom bindings
		Set<CastItem> bindings = customBindings.get(spell);
		if (bindings != null) {
			removed = bindings.remove(castItem);
			if (bindings.size() == 0) {
				bindings.add(new CastItem(-1));
			}
		}

		// remove from active bindings
		ArrayList<Spell> bindList = itemSpells.get(castItem);
		if (bindList != null) {
			removed = bindList.remove(spell) || removed;
			if (bindList.size() == 0) {
				itemSpells.remove(castItem);
				activeSpells.remove(castItem);
			} else {
				activeSpells.put(castItem, -1);
			}
		}

		return removed;
	}

	public void removeAllCustomBindings() {
		customBindings.clear();
		save();
		reload();
	}

	public void removeAllSpells() {
		for (Spell spell : allSpells) {
			if (spell instanceof BuffSpell) {
				((BuffSpell) spell).turnOff(player);
			}
		}
		allSpells.clear();
		itemSpells.clear();
		activeSpells.clear();
		customBindings.clear();
	}

	public void reload() {
		MagicSpells.debug(1, "Reloading player spell list: " + playerName);
		removeAllSpells();
		load();
	}

	public void save() {
		try {
			File file;
			if (plugin.separatePlayerSpellsPerWorld) {
				File folder = new File(spellbookDir, player.getWorld().getName());
				if (!folder.exists()) {
					folder.mkdirs();
				}
				File oldfile = new File(folder, playerName + ".txt");
				if (oldfile.exists())
					oldfile.delete();
				file = new File(folder, uniqueId + ".txt");
			} else {
				File oldfile = new File(spellbookDir, playerName + ".txt");
				if (oldfile.exists())
					oldfile.delete();
				file = new File(spellbookDir, uniqueId + ".txt");
			}
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
			for (Spell spell : allSpells) {
				if (!isTemporary(spell)) {
					writer.append(spell.getInternalName());
					if (customBindings.containsKey(spell)) {
						Set<CastItem> items = customBindings.get(spell);
						String s = "";
						for (CastItem i : items) {
							s += (s.isEmpty() ? "" : ",") + i;
						}
						writer.append(":" + s);
					}
					writer.newLine();
				}
			}
			writer.close();
			MagicSpells.debug(2, "Saved spellbook file: " + playerName.toLowerCase());
		} catch (Exception e) {
			plugin.getServer().getLogger().severe("Error saving player spellbook: " + playerName);
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "Spellbook:[playerName=" + playerName + ",uniqueId=" + uniqueId + ",allSpells=" + allSpells
				+ ",ItemSpells=" + itemSpells + ",ActiveSpells=" + activeSpells + ",CustomBindings=" + customBindings
				+ ",temporarySpells=" + temporarySpells + ",cantLearn=" + cantLearn + "]";
	}

}
