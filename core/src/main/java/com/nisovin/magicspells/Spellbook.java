package com.nisovin.magicspells;

import java.util.*;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.handlers.MagicXpHandler;
import com.nisovin.magicspells.events.SpellSelectionChangeEvent;
import com.nisovin.magicspells.events.SpellSelectionChangedEvent;

public class Spellbook {

	private Player player;
	private String playerName;
	private String uniqueId;

	private final Set<Spell> spells = new HashSet<>() {

		@Override
		public boolean remove(Object o) {
			boolean ret = super.remove(o);
			if (o instanceof Spell spell) spell.unloadPlayerEffectTracker(player);
			return ret;
		}

		@Override
		public boolean add(Spell s) {
			if (s == null) throw new NullPointerException("Spell cant be null here");
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

	private final Set<String> cantLearn = new HashSet<>();

	private final Map<CastItem, List<Spell>> itemSpells = new HashMap<>();
	private final Map<CastItem, Integer> activeSpells = new HashMap<>();
	private final Map<Spell, Set<CastItem>> customBindings = new HashMap<>();
	private final Map<Plugin, Set<Spell>> temporarySpells = new HashMap<>();

	public Spellbook(Player player) {
		this.player = player;
		playerName = player.getName();
		uniqueId = Util.getUniqueId(player);

		MagicSpells.debug(1, "Loading player spell list: " + playerName);
		load();
	}

	public void destroy() {
		spells.clear();
		cantLearn.clear();
		itemSpells.clear();
		activeSpells.clear();
		customBindings.clear();
		temporarySpells.clear();

		player = null;
		playerName = null;
	}

	public void load() {
		MagicSpells.debug("  Loading data for player '" + player.getName() + "'...");
		MagicSpells.getStorageHandler().load(this);

		// Give all spells to ops, or if ignoring grant perms
		if ((MagicSpells.ignoreGrantPerms() && MagicSpells.ignoreGrantPermsFakeValue()) || (player.isOp() && MagicSpells.grantOpsAllSpells())) {
			MagicSpells.debug("  ...is Op, granting all spells...");
			for (Spell spell : MagicSpells.getSpellsOrdered()) {
				if (spell.isHelperSpell()) continue;
				if (!spells.contains(spell)) addSpell(spell);
			}
		}

		// Add spells granted by permissions
		if (!MagicSpells.ignoreGrantPerms()) addGrantedSpells();

		// Sort spells or pre-select if just one
		for (CastItem i : itemSpells.keySet()) {
			List<Spell> spells = itemSpells.get(i);
			if (spells.size() == 1 && !MagicSpells.canCycleToNoSpell()) activeSpells.put(i, 0);
			else Collections.sort(spells);
		}

		MagicSpells.debug("  ...done");
	}

	public void save() {
		MagicSpells.getStorageHandler().save(this);
	}

	public void reload() {
		MagicSpells.debug(1, "Reloading data for player '" + playerName + "'...");
		removeAllSpells();
		MagicSpells.getStorageHandler().load(this);
		MagicSpells.debug(1, "...done");
	}

	public void addGrantedSpells() {
		MagicSpells.debug( "  Adding granted spells...");
		boolean added = false;
		for (Spell spell : MagicSpells.getSpellsOrdered()) {
			MagicSpells.debug(3, "    ...checking spell '" + spell.getInternalName() + "'...");
			if (spell.isHelperSpell()) continue;
			if (hasSpell(spell, false)) continue;
			if (spell.isAlwaysGranted() || Perm.GRANT.has(player, spell)) {
				addSpell(spell);
				added = true;
			}
		}
		if (added) save();
	}

	public boolean canLearn(Spell spell) {
		if (spell.isHelperSpell()) {
			MagicSpells.debug("Cannot learn spell '" + spell.getName() + "' because it is a helper spell.");
			return false;
		}

		if (cantLearn.contains(spell.getInternalName().toLowerCase())) {
			MagicSpells.debug("Cannot learn spell '" + spell.getName() + "' because another spell precludes it.");
			return false;
		}

		if (spell.getPrerequisites() != null) {
			for (String spellName : spell.getPrerequisites()) {
				Spell sp = MagicSpells.getSpellByInternalName(spellName);
				if (sp == null || !hasSpell(sp)) {
					MagicSpells.debug("Cannot learn " + spell.getName() + " because the prerequisite of " + spellName + " has not been satisfied.");
					return false;
				}
			}
		}

		if (spell.getXpRequired() != null) {
			MagicXpHandler handler = MagicSpells.getMagicXpHandler();
			if (handler != null) {
				for (String school : spell.getXpRequired().keySet()) {
					if (handler.getXp(player, school) < spell.getXpRequired().get(school)) {
						MagicSpells.debug("Cannot learn spell '" + spell.getName() + "' because the target does not have enough magic xp.");
						return false;
					}
				}
			}
		}

		MagicSpells.debug("Checking learn permissions for player '" + player.getName() + "'.");
		return Perm.LEARN.has(player, spell);
	}

	public boolean canCast(Spell spell) {
		if (spell.isHelperSpell()) return true;
		return MagicSpells.hasCastPermsIgnored() || Perm.CAST.has(player, spell);
	}

	public boolean canTeach(Spell spell) {
		if (spell.isHelperSpell()) return false;
		return Perm.TEACH.has(player, spell);
	}

	public boolean hasAdvancedPerm(String spell) {
		return player.hasPermission(Perm.ADVANCED.getNode() + spell);
	}

	public void addSpell(Spell spell) {
		addSpell(spell, (CastItem[]) null);
	}

	public void addSpell(Spell spell, CastItem castItem) {
		addSpell(spell, new CastItem[] {castItem});
	}

	public void addSpell(Spell spell, CastItem[] castItems) {
		if (spell == null) return;
		MagicSpells.debug(3, "    Added spell '" + spell.getInternalName() + "'.");
		spells.add(spell);

		// Add cast items
		CastItem[] items = spell.getCastItems();
		if (castItems != null && castItems.length > 0) {
			items = castItems;
			Set<CastItem> set = new HashSet<>();
			for (CastItem item : items) {
				if (item == null) continue;
				set.add(item);
			}
			customBindings.put(spell, set);
		} else if (MagicSpells.ignoreDefaultBindings()) return;

		for (CastItem item : items) {
			MagicSpells.debug(3, "        Cast item: " + item + (castItems != null ? " (custom)" : " (default)"));
			if (item == null) continue;
			List<Spell> temp = itemSpells.get(item);
			if (temp != null) {
				temp.add(spell);
				continue;
			}
			temp = new ArrayList<>();
			temp.add(spell);
			itemSpells.put(item, temp);
			activeSpells.put(item, MagicSpells.canCycleToNoSpell() ? -1 : 0);
		}

		// Remove any spells that this spell replaces
		if (spell.getReplaces() != null) {
			for (String spellName : spell.getReplaces()) {
				Spell sp = MagicSpells.getSpellByInternalName(spellName);
				if (sp == null) continue;
				MagicSpells.debug(3, "        Removing replaced spell '" + sp.getInternalName() + "'.");
				removeSpell(sp);
			}
		}
		// Prevent learning of spells this spell precludes
		if (spell.getPrecludes() != null) {
			for (String s : spell.getPrecludes()) {
				cantLearn.add(s.toLowerCase());
			}
		}
	}

	public void removeSpell(Spell spell) {
		if (spell instanceof BuffSpell buffSpell) buffSpell.turnOff(player);
		CastItem[] items = spell.getCastItems();
		if (customBindings.containsKey(spell)) items = customBindings.remove(spell).toArray(new CastItem[]{});
		for (CastItem item : items) {
			if (item == null) continue;
			List<Spell> temp = itemSpells.get(item);
			if (temp == null) continue;
			temp.remove(spell);
			if (temp.isEmpty()) {
				itemSpells.remove(item);
				activeSpells.remove(item);
				continue;
			}
			activeSpells.put(item, -1);
		}
		spells.remove(spell);
	}

	public boolean hasSpell(Spell spell) {
		return hasSpell(spell, true);
	}

	public boolean hasSpell(Spell spell, boolean checkGranted) {
		if (MagicSpells.ignoreGrantPerms() && MagicSpells.ignoreGrantPermsFakeValue()) return true;
		if (spells.contains(spell)) return true;

		if (checkGranted && !MagicSpells.ignoreGrantPerms() && Perm.GRANT.has(player, spell)) {
			MagicSpells.debug("Adding granted spell '" + spell.getName() + "' for player '" + player.getName() + "'.");
			addSpell(spell);
			return true;
		}
		return MagicSpells.areTempGrantPermsEnabled() && Perm.TEMPGRANT.has(player, spell);
	}

	protected CastItem getCastItemForCycling(ItemStack item) {
		CastItem castItem;
		if (item != null) castItem = new CastItem(item);
		else castItem = new CastItem(new ItemStack(Material.AIR));

		List<Spell> spells = itemSpells.get(castItem);
		if (spells != null && (spells.size() > 1 || (spells.size() == 1 && MagicSpells.canCycleToNoSpell()))) return castItem;
		return null;
	}

	public Spell nextSpell(ItemStack item) {
		CastItem castItem = getCastItemForCycling(item);
		if (castItem != null) return nextSpell(castItem);
		return null;
	}

	protected Spell nextSpell(CastItem castItem) {
		Integer i = activeSpells.get(castItem); // Get the index of the active spell for the cast item
		if (i == null) return null;
		List<Spell> spells = itemSpells.get(castItem); // Get all the spells for the cast item
		if (!(spells.size() > 1 || i.equals(-1) || MagicSpells.canCycleToNoSpell() || MagicSpells.showMessageOnCycle())) return null;
		int count = 0;
		SpellSelectionChangeEvent event;
		while (count++ < spells.size()) {
			i++;
			if (i >= spells.size()) {
				if (MagicSpells.canCycleToNoSpell()) {
					event = new SpellSelectionChangeEvent(null, player, castItem, this);
					EventUtil.call(event);
					if (event.isCancelled()) return null;

					activeSpells.put(castItem, -1);
					EventUtil.call(new SpellSelectionChangedEvent(null, player, castItem, this));
					MagicSpells.sendMessage(MagicSpells.getSpellChangeEmptyMessage(), player, MagicSpells.NULL_ARGS);
					return null;
				} else {
					i = 0;
				}
			}
			if (!MagicSpells.cycleToCastableSpells() || canCast(spells.get(i))) {
				event = new SpellSelectionChangeEvent(spells.get(i), player, castItem, this);
				EventUtil.call(event);
				if (event.isCancelled()) return null;

				activeSpells.put(castItem, i);
				EventUtil.call(new SpellSelectionChangedEvent(spells.get(i), player, castItem, this));
				return spells.get(i);
			}
		}
		return null;
	}

	public Spell prevSpell(ItemStack item) {
		CastItem castItem = getCastItemForCycling(item);
		if (castItem != null) return prevSpell(castItem);
		return null;
	}

	protected Spell prevSpell(CastItem castItem) {
		Integer i = activeSpells.get(castItem); // Get the index of the active spell for the cast item
		if (i == null) return null;

		List<Spell> spells = itemSpells.get(castItem); // Get all the spells for the cast item
		if (spells.size() > 1 || i.equals(-1) || MagicSpells.canCycleToNoSpell()) {
			int count = 0;
			SpellSelectionChangeEvent event;
			while (count++ < spells.size()) {
				i--;
				if (i < 0) {
					if (MagicSpells.canCycleToNoSpell() && i == -1) {
						event = new SpellSelectionChangeEvent(null, player, castItem, this);
						EventUtil.call(event);
						if (event.isCancelled()) return null;

						activeSpells.put(castItem, -1);
						EventUtil.call(new SpellSelectionChangedEvent(null, player, castItem, this));
						MagicSpells.sendMessage(MagicSpells.getSpellChangeEmptyMessage(), player, MagicSpells.NULL_ARGS);
						return null;
					} else {
						i = spells.size() - 1;
					}
				}
				if (!MagicSpells.cycleToCastableSpells() || canCast(spells.get(i))) {
					event = new SpellSelectionChangeEvent(spells.get(i), player, castItem, this);
					EventUtil.call(event);
					if (event.isCancelled()) return null;

					activeSpells.put(castItem, i);
					EventUtil.call(new SpellSelectionChangedEvent(spells.get(i), player, castItem, this));
					return spells.get(i);
				}
			}
			return null;
		}
		return null;
	}

	public Spell getActiveSpell(ItemStack item) {
		CastItem castItem = new CastItem(item);
		return getActiveSpell(castItem);
	}

	public Spell getActiveSpell(CastItem castItem) {
		Integer i = activeSpells.get(castItem);
		if (i != null && i != -1) return itemSpells.get(castItem).get(i);
		return null;
	}

	public void addTemporarySpell(Spell spell, Plugin plugin) {
		if (hasSpell(spell)) return;
		addSpell(spell);
		Set<Spell> temps = temporarySpells.computeIfAbsent(plugin, pl -> new HashSet<>());
		temps.add(spell);
	}

	public void removeTemporarySpells(Plugin plugin) {
		Set<Spell> temps = temporarySpells.remove(plugin);
		if (temps == null) return;
		for (Spell spell : temps) {
			removeSpell(spell);
		}
	}

	public boolean isTemporary(Spell spell) {
		for (Set<Spell> temps : temporarySpells.values()) {
			if (temps.contains(spell)) return true;
		}
		return false;
	}

	public void addCastItem(Spell spell, CastItem castItem) {
		// Add to custom bindings
		Set<CastItem> bindings = customBindings.computeIfAbsent(spell, s -> new HashSet<>());
		bindings.add(castItem);

		// Add to item bindings
		List<Spell> bindList = itemSpells.get(castItem);
		if (bindList == null) {
			bindList = new ArrayList<>();
			itemSpells.put(castItem, bindList);
			activeSpells.put(castItem, MagicSpells.canCycleToNoSpell() ? -1 : 0);
		}
		bindList.add(spell);
	}

	public boolean removeCastItem(Spell spell, CastItem castItem) {
		boolean removed = false;

		// Remove from custom bindings
		Set<CastItem> bindings = customBindings.get(spell);
		if (bindings != null) {
			removed = bindings.remove(castItem);
			if (bindings.isEmpty()) bindings.add(new CastItem());
		}

		// Remove from active bindings
		List<Spell> bindList = itemSpells.get(castItem);
		if (bindList != null) {
			removed = bindList.remove(spell) || removed;
			if (bindList.isEmpty()) {
				itemSpells.remove(castItem);
				activeSpells.remove(castItem);
			} else activeSpells.put(castItem, -1);
		}

		return removed;
	}

	public void removeAllCustomBindings() {
		customBindings.clear();
		save();
		reload();
	}

	public void removeAllSpells() {
		for (Spell spell : spells) {
			if (!(spell instanceof BuffSpell buffSpell)) continue;
			buffSpell.turnOff(player);
		}

		spells.clear();
		itemSpells.clear();
		activeSpells.clear();
		customBindings.clear();
	}

	public Player getPlayer() {
		return player;
	}

	public Set<Spell> getSpells() {
		return spells;
	}

	public Set<String> getCantLearn() {
		return cantLearn;
	}

	public Map<CastItem, List<Spell>> getItemSpells() {
		return itemSpells;
	}

	public Map<CastItem, Integer> getActiveSpells() {
		return activeSpells;
	}

	public Map<Spell, Set<CastItem>> getCustomBindings() {
		return customBindings;
	}

	public Map<Plugin, Set<Spell>> getTemporarySpells() {
		return temporarySpells;
	}

	@Override
	public String toString() {
		return "Spellbook:[playerName=" + playerName
				+ ",uniqueId=" + uniqueId
				+ ",spells=" + spells
				+ ",itemSpells=" + itemSpells
				+ ",activeSpells=" + activeSpells
				+ ",customBindings=" + customBindings
				+ ",temporarySpells=" + temporarySpells
				+ ",cantLearn=" + cantLearn
				+ ']';
	}

}
