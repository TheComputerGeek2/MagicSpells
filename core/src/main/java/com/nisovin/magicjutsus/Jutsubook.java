package com.nisovin.magicjutsus;

import java.io.File;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedWriter;

import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.CastItem;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuSelectionChangedEvent;

public class Jutsubook {

	private MagicJutsus plugin;

	private Player player;
	private String playerName;
	private String uniqueId;

	private TreeSet<Jutsu> allJutsus = new TreeSet<Jutsu>() {

		private static final long serialVersionUID = 1L;

		@Override
		public boolean remove(Object o) {
			boolean ret = super.remove(o);
			if (o instanceof Jutsu) {
				Jutsu s = (Jutsu) o;
				s.unloadPlayerEffectTracker(player);
			}
			return ret;
		}

		@Override
		public boolean add(Jutsu s) {
			boolean ret = super.add(s);
			s.initializePlayerEffectTracker(player);
			return ret;
		}

		@Override
		public void clear() {
			for (Jutsu s : this) {
				s.unloadPlayerEffectTracker(player);
			}
			super.clear();
		}

	};

	private Map<CastItem, List<Jutsu>> itemJutsus = new HashMap<>();
	private Map<CastItem, Integer> activeJutsus = new HashMap<>();
	private Map<Jutsu, Set<CastItem>> customBindings = new HashMap<>();
	private Map<Plugin, Set<Jutsu>> temporaryJutsus = new HashMap<>();
	private Set<String> cantLearn = new HashSet<>();

	// DEBUG INFO: level 1, loaded player jutsu list (player)
	public Jutsubook(Player player, MagicJutsus plugin) {
		this.plugin = plugin;
		this.player = player;
		playerName = player.getName();
		uniqueId = Util.getUniqueId(player);

		MagicJutsus.debug(1, "Loading player jutsu list: " + playerName);
		load();
	}

	public void destroy() {
		allJutsus.clear();
		itemJutsus.clear();
		activeJutsus.clear();
		customBindings.clear();
		temporaryJutsus.clear();
		cantLearn.clear();
		player = null;
		playerName = null;
	}

	public void load() {
		load(player.getWorld());
	}

	// DEBUG INFO: level 2, "op, granting all jutsus"
	public void load(World playerWorld) {
		// Load jutsus from file
		loadFromFile(playerWorld);

		// Give all jutsus to ops, or if ignoring grant perms
		if ((plugin.ignoreGrantPerms && plugin.ignoreGrantPermsFakeValue) || (player.isOp() && plugin.opsHaveAllJutsus)) {
			MagicJutsus.debug(2, "  Op, granting all jutsus...");
			for (Jutsu jutsu : MagicJutsus.getJutsusOrdered()) {
				if (jutsu.isHelperJutsu()) continue;
				if (!allJutsus.contains(jutsu)) addJutsu(jutsu);
			}
		}

		// Add jutsus granted by permissions
		if (!plugin.ignoreGrantPerms) addGrantedJutsus();

		// Sort jutsus or pre-select if just one
		for (CastItem i : itemJutsus.keySet()) {
			List<Jutsu> jutsus = itemJutsus.get(i);
			if (jutsus.size() == 1 && !plugin.allowCycleToNoJutsu) activeJutsus.put(i, 0);
			else Collections.sort(jutsus);
		}
	}

	// DEBUG INFO: level 2, loading jutsus from player file
	private void loadFromFile(World playerWorld) {
		try {
			MagicJutsus.debug(2, "  Loading jutsus from player file...");
			File file;
			if (plugin.separatePlayerJutsusPerWorld) {
				File folder = new File(plugin.getDataFolder(), "jutsubooks" + File.separator + player.getWorld().getName());
				if (!folder.exists()) folder.mkdir();

				file = new File(plugin.getDataFolder(), "jutsubooks" + File.separator + playerWorld.getName() + File.separator + uniqueId + ".txt");
				if (!file.exists()) {
					File file2 = new File(plugin.getDataFolder(), "jutsubooks" + File.separator + playerWorld.getName() + File.separator + playerName.toLowerCase() + ".txt");
					if (file2.exists()) file2.renameTo(file);
				}
			} else {
				file = new File(plugin.getDataFolder(), "jutsubooks" + File.separator + uniqueId + ".txt");
				if (!file.exists()) {
					File file2 = new File(plugin.getDataFolder(), "jutsubooks" + File.separator + playerName.toLowerCase() + ".txt");
					if (file2.exists()) file2.renameTo(file);
				}
			}
			if (file.exists()) {
				Scanner scanner = new Scanner(file);
				while (scanner.hasNext()) {
					String line = scanner.nextLine();
					if (line.isEmpty()) continue;
					if (!line.contains(":")) {
						Jutsu jutsu = MagicJutsus.getJutsuByInternalName(line);
						if (jutsu != null) addJutsu(jutsu);
						continue;
					}
					String[] data = line.split(":", 2);
					Jutsu jutsu = MagicJutsus.getJutsuByInternalName(data[0]);
					if (jutsu == null) continue;

					List<CastItem> items = new ArrayList<>();
					String[] s = data[1].split(",");
					for (int i = 0; i < s.length; i++) {
						try {
							CastItem castItem = new CastItem(s[i]);
							items.add(castItem);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					addJutsu(jutsu, items.toArray(new CastItem[items.size()]));
				}
				scanner.close();
			}
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
		}
	}

	// DEBUG INFO: level 2, adding granted jutsus
	// DEBUG INFO: level 3, checking jutsu <internal name>
	public void addGrantedJutsus() {
		MagicJutsus.debug(2, "  Adding granted jutsus...");
		boolean added = false;
		for (Jutsu jutsu : MagicJutsus.getJutsusOrdered()) {
			MagicJutsus.debug(3, "    Checking jutsu " + jutsu.getInternalName() + "...");
			if (jutsu.isHelperJutsu()) continue;
			if (hasJutsu(jutsu, false)) continue;
			if (jutsu.isAlwaysGranted() || Perm.GRANT.has(player, jutsu)) {
				addJutsu(jutsu);
				added = true;
			}
		}
		if (added) save();
	}

	// DEBUG INFO: level 2, cannot learn jutsu because helper
	// DEBUG INFO: level 2, cannot learn because precludes
	// DEBUG INFO: level 2, cannot learn because prereq not meet
	// DEBUG INFO: level 2, cannot learn insufficient magic xp
	// DEBUG INFO: level 2, checking learn permissions
	public boolean canLearn(Jutsu jutsu) {
		if (jutsu.isHelperJutsu()) {
			MagicJutsus.debug("Cannot learn " + jutsu.getName() + " because it is a helper jutsu");
			return false;
		}

		if (cantLearn.contains(jutsu.getInternalName().toLowerCase())) {
			MagicJutsus.debug("Cannot learn " + jutsu.getName() + " because another jutsu precludes it.");
			return false;
		}

		if (jutsu.prerequisites != null) {
			for (String jutsuName : jutsu.prerequisites) {
				Jutsu sp = MagicJutsus.getJutsuByInternalName(jutsuName);
				if (sp == null || !hasJutsu(sp)) {
					MagicJutsus.debug("Cannot learn " + jutsu.getName() + " because the prerequisite of " + jutsuName + " has not been satisfied");
					return false;
				}
			}
		}

		if (jutsu.xpRequired != null) {
			MagicXpHandler handler = MagicJutsus.getMagicXpHandler();
			if (handler != null) {
				for (String school : jutsu.xpRequired.keySet()) {
					if (handler.getXp(player, school) < jutsu.xpRequired.get(school)) {
						MagicJutsus.debug("Cannot learn " + jutsu.getName() + " because the target does not have enough magic xp");
						return false;
					}
				}
			}
		}

		MagicJutsus.debug("Checking learn permissions for " + player.getName());
		return Perm.LEARN.has(player, jutsu);
	}

	public boolean canCast(Jutsu jutsu) {
		if (jutsu.isHelperJutsu()) return true;
		return plugin.ignoreCastPerms || Perm.CAST.has(player, jutsu);
	}

	public boolean canTeach(Jutsu jutsu) {
		if (jutsu.isHelperJutsu()) return false;
		return Perm.TEACH.has(player, jutsu);
	}

	public boolean hasAdvancedPerm(String jutsu) {
		return player.hasPermission(Perm.ADVANCED.getNode() + jutsu);
	}

	public Jutsu getJutsuByName(String jutsuName) {
		Jutsu jutsu = MagicJutsus.getJutsuByInGameName(jutsuName);
		if (jutsu != null && hasJutsu(jutsu)) return jutsu;
		return null;
	}

	public Set<Jutsu> getJutsus() {
		return allJutsus;
	}

	public Map<CastItem, List<Jutsu>> getItemJutsus() {
		return itemJutsus;
	}

	public Map<CastItem, Integer> getActiveJutsus() {
		return activeJutsus;
	}

	public Map<Jutsu, Set<CastItem>> getCustomBindings() {
		return customBindings;
	}

	public Map<Plugin, Set<Jutsu>> getTemporaryJutsus() {
		return temporaryJutsus;
	}

	public List<String> tabComplete(String partial) {
		String[] data = Util.splitParams(partial, 2);
		if (data.length == 1) {
			// Complete jutsu name
			partial = data[0].toLowerCase();
			List<String> options = new ArrayList<>();
			for (Jutsu jutsu : allJutsus) {
				if (!jutsu.canCastByCommand()) continue;
				if (jutsu.isHelperJutsu()) continue;
				if (jutsu.getName().toLowerCase().startsWith(partial)) {
					options.add(jutsu.getName());
					continue;
				}

				String[] aliases = jutsu.getAliases();
				if (aliases == null || aliases.length <= 0) continue;
				for (String alias : aliases) {
					if (!alias.toLowerCase().startsWith(partial)) continue;
					options.add(alias);
				}
			}
			if (!options.isEmpty()) return options;
			return null;
		}
		// Complete jutsu params
		Jutsu jutsu = getJutsuByName(data[0]);
		if (jutsu == null) return null;
		List<String> ret = jutsu.tabComplete(player, data[1]);
		if (ret == null || ret.isEmpty()) return null;
		return ret;
	}

	protected CastItem getCastItemForCycling(ItemStack item) {
		CastItem castItem;
		if (item != null) castItem = new CastItem(item);
		else castItem = new CastItem(Material.AIR);
		List<Jutsu> jutsus = itemJutsus.get(castItem);
		if (jutsus != null && (jutsus.size() > 1 || (jutsus.size() == 1 && plugin.allowCycleToNoJutsu))) return castItem;
		return null;
	}

	protected Jutsu nextJutsu(ItemStack item) {
		CastItem castItem = getCastItemForCycling(item);
		if (castItem != null) return nextJutsu(castItem);
		return null;
	}

	protected Jutsu nextJutsu(CastItem castItem) {
		Integer i = activeJutsus.get(castItem); // Get the index of the active jutsu for the cast item
		if (i == null) return null;
		List<Jutsu> jutsus = itemJutsus.get(castItem); // Get all the jutsus for the cast item
		if (!(jutsus.size() > 1 || i.equals(-1) || plugin.allowCycleToNoJutsu || plugin.alwaysShowMessageOnCycle)) return null;
		int count = 0;
		while (count++ < jutsus.size()) {
			i++;
			if (i >= jutsus.size()) {
				if (plugin.allowCycleToNoJutsu) {
					activeJutsus.put(castItem, -1);
					EventUtil.call(new JutsuSelectionChangedEvent(null, player, castItem, this));
					MagicJutsus.sendMessage(plugin.strJutsuChangeEmpty, player, MagicJutsus.NULL_ARGS);
					return null;
				} else {
					i = 0;
				}
			}
			if (!plugin.onlyCycleToCastableJutsus || canCast(jutsus.get(i))) {
				activeJutsus.put(castItem, i);
				EventUtil.call(new JutsuSelectionChangedEvent(jutsus.get(i), player, castItem, this));
				return jutsus.get(i);
			}
		}
		return null;
	}

	protected Jutsu prevJutsu(ItemStack item) {
		CastItem castItem = getCastItemForCycling(item);
		if (castItem != null) return prevJutsu(castItem);
		return null;
	}

	protected Jutsu prevJutsu(CastItem castItem) {
		Integer i = activeJutsus.get(castItem); // Get the index of the active jutsu for the cast item
		if (i == null) return null;
		List<Jutsu> jutsus = itemJutsus.get(castItem); // Get all the jutsus for the cast item
		if (jutsus.size() > 1 || i.equals(-1) || plugin.allowCycleToNoJutsu) {
			int count = 0;
			while (count++ < jutsus.size()) {
				i--;
				if (i < 0) {
					if (plugin.allowCycleToNoJutsu && i == -1) {
						activeJutsus.put(castItem, -1);
						EventUtil.call(new JutsuSelectionChangedEvent(null, player, castItem, this));
						MagicJutsus.sendMessage(plugin.strJutsuChangeEmpty, player, MagicJutsus.NULL_ARGS);
						return null;
					} else {
						i = jutsus.size() - 1;
					}
				}
				if (!plugin.onlyCycleToCastableJutsus || canCast(jutsus.get(i))) {
					activeJutsus.put(castItem, i);
					EventUtil.call(new JutsuSelectionChangedEvent(jutsus.get(i), player, castItem, this));
					return jutsus.get(i);
				}
			}
			return null;
		}
		return null;
	}

	public Jutsu getActiveJutsu(ItemStack item) {
		CastItem castItem = new CastItem(item);
		return getActiveJutsu(castItem);
	}

	public Jutsu getActiveJutsu(CastItem castItem) {
		Integer i = activeJutsus.get(castItem);
		if (i != null && i != -1) return itemJutsus.get(castItem).get(i);
		return null;
	}

	public boolean hasJutsu(Jutsu jutsu) {
		return hasJutsu(jutsu, true);
	}

	// DEBUG INFO: level 2, adding granted jutsu for player, jutsu
	public boolean hasJutsu(Jutsu jutsu, boolean checkGranted) {
		if (plugin.ignoreGrantPerms && plugin.ignoreGrantPermsFakeValue) return true;
		boolean has = allJutsus.contains(jutsu);
		if (has) return true;
		if (checkGranted && !plugin.ignoreGrantPerms && Perm.GRANT.has(player, jutsu)) {
			MagicJutsus.debug(2, "Adding granted jutsu for " + player.getName() + ": " + jutsu.getName());
			addJutsu(jutsu);
			save();
			return true;
		}
		return MagicJutsus.plugin.enableTempGrantPerms && Perm.TEMPGRANT.has(player, jutsu);
	}

	public void addJutsu(Jutsu jutsu) {
		addJutsu(jutsu, (CastItem[]) null);
	}

	public void addJutsu(Jutsu jutsu, CastItem castItem) {
		addJutsu(jutsu, new CastItem[] {castItem});
	}

	// DEBUG INFO: level 3, added jutsu, internalname
	// DEBUG INFO: level 3, cast item item, custom|default
	// DEBUG INFO: level 3, removing replaced jutsu, internalname
	public void addJutsu(Jutsu jutsu, CastItem[] castItems) {
		if (jutsu == null) return;
		MagicJutsus.debug(3, "    Added jutsu: " + jutsu.getInternalName());
		allJutsus.add(jutsu);
		if (jutsu.canCastWithItem()) {
			CastItem[] items = jutsu.getCastItems();
			if (castItems != null && castItems.length > 0) {
				items = castItems;
				Set<CastItem> set = new HashSet<>();
				for (CastItem item : items) {
					if (item != null) set.add(item);
				}
				customBindings.put(jutsu, set);
			} else if (plugin.ignoreDefaultBindings) return;

			for (CastItem i : items) {
				MagicJutsus.debug(3, "        Cast item: " + i + (castItems != null ? " (custom)" : " (default)"));
				if (i == null) continue;
				List<Jutsu> temp = itemJutsus.get(i);
				if (temp != null) {
					temp.add(jutsu);
					continue;
				}
				temp = new ArrayList<>();
				temp.add(jutsu);
				itemJutsus.put(i, temp);
				activeJutsus.put(i, plugin.allowCycleToNoJutsu ? -1 : 0);
			}
		}
		// Remove any jutsus that this jutsu replaces
		if (jutsu.replaces != null) {
			for (String jutsuName : jutsu.replaces) {
				Jutsu sp = MagicJutsus.getJutsuByInternalName(jutsuName);
				if (sp == null) continue;
				MagicJutsus.debug(3, "        Removing replaced jutsu: " + sp.getInternalName());
				removeJutsu(sp);
			}
		}
		// Prevent learning of jutsus this jutsu precludes
		if (jutsu.precludes != null) {
			for (String s : jutsu.precludes) {
				cantLearn.add(s.toLowerCase());
			}
		}
	}

	public void removeJutsu(Jutsu jutsu) {
		if (jutsu instanceof BuffJutsu) ((BuffJutsu) jutsu).turnOff(player);
		CastItem[] items = jutsu.getCastItems();
		if (customBindings.containsKey(jutsu)) items = customBindings.remove(jutsu).toArray(new CastItem[]{});
		for (CastItem item : items) {
			if (item == null) continue;
			List<Jutsu> temp = itemJutsus.get(item);
			if (temp == null) continue;
			temp.remove(jutsu);
			if (temp.isEmpty()) {
				itemJutsus.remove(item);
				activeJutsus.remove(item);
				continue;
			}
			activeJutsus.put(item, -1);
		}
		allJutsus.remove(jutsu);
	}

	public void addTemporaryJutsu(Jutsu jutsu, Plugin plugin) {
		if (!hasJutsu(jutsu)) {
			addJutsu(jutsu);
			Set<Jutsu> temps = temporaryJutsus.computeIfAbsent(plugin, pl -> new HashSet<>());
			if (temps == null) throw new IllegalStateException("temporary jutsus should not contain a null value!");
			temps.add(jutsu);
		}
	}

	public void removeTemporaryJutsus(Plugin plugin) {
		Set<Jutsu> temps = temporaryJutsus.remove(plugin);
		if (temps == null) return;
		for (Jutsu jutsu : temps) {
			removeJutsu(jutsu);
		}
	}

	private boolean isTemporary(Jutsu jutsu) {
		for (Set<Jutsu> temps : temporaryJutsus.values()) {
			if (temps.contains(jutsu)) return true;
		}
		return false;
	}

	public void addCastItem(Jutsu jutsu, CastItem castItem) {
		// Add to custom bindings
		Set<CastItem> bindings = customBindings.computeIfAbsent(jutsu, s -> new HashSet<>());
		if (bindings == null) throw new IllegalStateException("customBindings jutsus should not contain a null value!");
		if (!bindings.contains(castItem)) bindings.add(castItem);

		// Add to item bindings
		List<Jutsu> bindList = itemJutsus.get(castItem);
		if (bindList == null) {
			bindList = new ArrayList<>();
			itemJutsus.put(castItem, bindList);
			activeJutsus.put(castItem, plugin.allowCycleToNoJutsu ? -1 : 0);
		}
		bindList.add(jutsu);
	}

	public boolean removeCastItem(Jutsu jutsu, CastItem castItem) {
		boolean removed = false;

		// Remove from custom bindings
		Set<CastItem> bindings = customBindings.get(jutsu);
		if (bindings != null) {
			removed = bindings.remove(castItem);
			if (bindings.isEmpty()) bindings.add(new CastItem((Material) null));
		}

		// Remove from active bindings
		List<Jutsu> bindList = itemJutsus.get(castItem);
		if (bindList != null) {
			removed = bindList.remove(jutsu) || removed;
			if (bindList.isEmpty()) {
				itemJutsus.remove(castItem);
				activeJutsus.remove(castItem);
			} else activeJutsus.put(castItem, -1);
		}

		return removed;
	}

	public void removeAllCustomBindings() {
		customBindings.clear();
		save();
		reload();
	}

	public void removeAllJutsus() {
		for (Jutsu jutsu : allJutsus) {
			if (jutsu instanceof BuffJutsu) ((BuffJutsu) jutsu).turnOff(player);
		}
		allJutsus.clear();
		itemJutsus.clear();
		activeJutsus.clear();
		customBindings.clear();
	}

	// DEBUG INFO: level 1, reloading player jutsu list, playername
	public void reload() {
		MagicJutsus.debug(1, "Reloading player jutsu list: " + playerName);
		removeAllJutsus();
		load();
	}

	// DEBUG INFO: level 2, saved jutsubook file, playername
	public void save() {
		try {
			File file;
			if (plugin.separatePlayerJutsusPerWorld) {
				File folder = new File(plugin.getDataFolder(), "jutsubooks" + File.separator + player.getWorld().getName());
				if (!folder.exists()) folder.mkdirs();
				File oldfile = new File(plugin.getDataFolder(), "jutsubooks" + File.separator + player.getWorld().getName() + File.separator + playerName + ".txt");
				if (oldfile.exists()) oldfile.delete();
				file = new File(plugin.getDataFolder(), "jutsubooks" + File.separator + player.getWorld().getName() + File.separator + uniqueId + ".txt");
			} else {
				File oldfile = new File(plugin.getDataFolder(), "jutsubooks" + File.separator + playerName + ".txt");
				if (oldfile.exists()) oldfile.delete();
				file = new File(plugin.getDataFolder(), "jutsubooks" + File.separator + uniqueId + ".txt");
			}
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
			for (Jutsu jutsu : allJutsus) {
				if (isTemporary(jutsu)) continue;
				writer.append(jutsu.getInternalName());
				if (customBindings.containsKey(jutsu)) {
					Set<CastItem> items = customBindings.get(jutsu);
					StringBuilder s = new StringBuilder();
					for (CastItem i : items) {
						s.append((s.length() == 0) ? "" : ",").append(i);
					}
					writer.append(String.valueOf(':')).append(s.toString());
				}
				writer.newLine();
			}
			writer.close();
			MagicJutsus.debug(2, "Saved jutsubook file: " + playerName.toLowerCase());
		} catch (Exception e) {
			plugin.getServer().getLogger().severe("Error saving player jutsubook: " + playerName);
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "Jutsubook:[playerName=" + playerName
				+ ",uniqueId=" + uniqueId
				+ ",allJutsus=" + allJutsus
				+ ",itemJutsus=" + itemJutsus
				+ ",activeJutsus=" + activeJutsus
				+ ",customBindings=" + customBindings
				+ ",temporaryJutsus=" + temporaryJutsus
				+ ",cantLearn=" + cantLearn
				+ ']';
	}

}
