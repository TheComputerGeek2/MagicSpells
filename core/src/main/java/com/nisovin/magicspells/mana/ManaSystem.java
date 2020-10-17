package com.nisovin.magicspells.mana;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.castmodifiers.ModifierSet;

public class ManaSystem extends ManaHandler {

	private String defaultBarPrefix;
	private char defaultSymbol;
	private int defaultBarSize;
	private ChatColor defaultBarColorFull;
	private ChatColor defaultBarColorEmpty;

	private int defaultMaxMana;
	private int defaultStartingMana;
	private int defaultRegenAmount;
	private int defaultRegenInterval;

	private boolean showManaOnUse;
	private boolean showManaOnRegen;
	private boolean showManaOnHungerBar;
	private boolean showManaOnActionBar;
	private boolean showManaOnExperienceBar;

	private List<String> modifierList;
	private ModifierSet modifiers;
	
	private ManaRank defaultRank;
	private List<ManaRank> ranks;
	
	private Map<UUID, ManaBar> manaBars;
	private Set<Regenerator> regenerators;

	public ManaSystem(MagicConfig config) {
		String path = "mana.";
		defaultBarPrefix = config.getString(path + "default-prefix", "Mana:");
		defaultSymbol = config.getString(path + "default-symbol", "=").charAt(0);
		defaultBarSize = config.getInt(path + "default-size", 35);
		defaultBarColorFull = ChatColor.getByChar(config.getString(path + "default-color-full", ChatColor.GREEN.getChar() + ""));
		defaultBarColorEmpty = ChatColor.getByChar(config.getString(path + "default-color-empty", ChatColor.BLACK.getChar() + ""));

		defaultMaxMana = config.getInt(path + "default-max-mana", 100);
		defaultStartingMana = config.getInt(path + "default-starting-mana", defaultMaxMana);
		defaultRegenAmount = config.getInt(path + "default-regen-amount", 5);
		defaultRegenInterval = config.getInt(path + "default-regen-interval", TimeUtil.TICKS_PER_SECOND);

		showManaOnUse = config.getBoolean(path + "show-mana-on-use", false);
		showManaOnRegen = config.getBoolean(path + "show-mana-on-regen", false);
		showManaOnHungerBar = config.getBoolean(path + "show-mana-on-hunger-bar", false);
		showManaOnActionBar = config.getBoolean(path + "show-mana-on-action-bar", false);
		showManaOnExperienceBar = config.getBoolean(path + "show-mana-on-experience-bar", true);

		modifierList = config.getStringList(path + "modifiers", null);
		
		defaultRank = new ManaRank("default", defaultBarPrefix, defaultSymbol, defaultBarSize, defaultMaxMana, defaultStartingMana, defaultRegenAmount, defaultRegenInterval, defaultBarColorFull, defaultBarColorEmpty);

		regenerators = new HashSet<>();
		ranks = new ArrayList<>();
		manaBars = new HashMap<>();

		Set<String> rankKeys = config.getKeys("mana.ranks");
		if (rankKeys != null) {
			for (String key : rankKeys) {
				String keyPath = "mana.ranks." + key + ".";

				ManaRank r = new ManaRank();
				r.setName(key);
				r.setPrefix(config.getString(keyPath + "prefix", defaultBarPrefix));
				r.setSymbol(config.getString(keyPath + "symbol", defaultSymbol + "").charAt(0));
				r.setBarSize(config.getInt(keyPath + "size", defaultBarSize));
				r.setMaxMana(config.getInt(keyPath + "max-mana", defaultMaxMana));
				r.setStartingMana(config.getInt(keyPath + "starting-mana", defaultStartingMana));
				r.setRegenAmount(config.getInt(keyPath + "regen-amount", defaultRegenAmount));
				r.setRegenInterval(config.getInt(keyPath + "regen-interval", defaultRegenAmount));
				r.setColorFull(ChatColor.getByChar(config.getString(keyPath + "color-full", defaultBarColorFull.getChar() + "")));
				r.setColorEmpty(ChatColor.getByChar(config.getString(keyPath + "color-empty", defaultBarColorEmpty.getChar() + "")));

				regenerators.add(new Regenerator(r, r.getRegenInterval()));

				ranks.add(r);
			}
		}

		regenerators.add(new Regenerator(defaultRank, defaultRegenInterval));
	}
	
	// DEBUG INFO: level 2, adding mana modifiers
	@Override
	public void initialize() {
		if (modifierList != null && !modifierList.isEmpty()) {
			MagicSpells.debug(2, "Adding mana modifiers");
			modifiers = new ModifierSet(modifierList);
			modifierList = null;
		}
	}
	
	// DEBUG INFO: level 1, creating mana bar for player playername with rank rankname
	private ManaBar getManaBar(Player player) {
		ManaBar bar = manaBars.get(player.getUniqueId());
		if (bar == null) {
			// Create the mana bar
			ManaRank rank = getRank(player);
			bar = new ManaBar(player, rank);
			MagicSpells.debug(1, "Creating mana bar for player " + player.getName() + " with rank " + rank.getName());
			manaBars.put(player.getUniqueId(), bar);
		}
		return bar;
	}
	
	// DEBUG INFO: level 1, updating mana bar for player playername with rank rankname
	@Override
	public void createManaBar(final Player player) {
		boolean update = manaBars.containsKey(player.getUniqueId());
		ManaBar bar = getManaBar(player);
		if (bar == null) return;
		if (update) {
			ManaRank rank = getRank(player);
			if (rank != bar.getManaRank()) {
				MagicSpells.debug(1, "Updating mana bar for player " + player.getName() + " with rank " + rank.getName());
				bar.setRank(rank);
			}
		}
		MagicSpells.scheduleDelayedTask(() -> showMana(player), 11);
	}
	
	@Override
	public boolean updateManaRankIfNecessary(Player player) {
		if (manaBars.containsKey(player.getUniqueId())) {
			ManaBar bar = getManaBar(player);
			ManaRank rank = getRank(player);
			if (bar.getManaRank() != rank) {
				bar.setRank(rank);
				return true;
			}
		} else getManaBar(player);

		return false;
	}
	
	// DEBUG INFO: level 3, fetching mana rank for playername
	// DEBUG INFO: level 3, checking rank rankname
	// DEBUG INFO: level 3, rank found
	// DEBUG INFO: level 3, no rank found
	private ManaRank getRank(Player player) {
		MagicSpells.debug(3, "Fetching mana rank for player " + player.getName() + "...");
		for (ManaRank rank : ranks) {
			MagicSpells.debug(3, "    checking rank " + rank.getName());
			if (player.hasPermission("magicspells.rank." + rank.getName())) {
				MagicSpells.debug(3, "    rank found");
				return rank;
			}
		}
		MagicSpells.debug(3, "    no rank found");
		return defaultRank;
	}

	@Override
	public int getMaxMana(Player player) {
		ManaBar bar = getManaBar(player);
		if (bar != null) return bar.getMaxMana();
		return 0;
	}
	
	@Override
	public void setMaxMana(Player player, int amount) {
		ManaBar bar = getManaBar(player);
		if (bar != null) bar.setMaxMana(amount);
	}
	
	@Override
	public int getRegenAmount(Player player) {
		ManaBar bar = getManaBar(player);
		if (bar == null) return 0;
		return bar.getRegenAmount();
	}

	@Override
	public void setRegenAmount(Player player, int amount) {
		ManaBar bar = getManaBar(player);
		if (bar == null) return;
		bar.setRegenAmount(amount);
	}

	@Override
	public int getMana(Player player) {
		ManaBar bar = getManaBar(player);
		if (bar == null) return 0;
		return bar.getMana();
	}
	
	@Override
	public boolean hasMana(Player player, int amount) {
		ManaBar bar = getManaBar(player);
		if (bar == null) return false;
		return bar.has(amount);
	}

	@Override
	public boolean addMana(Player player, int amount, ManaChangeReason reason) {
		ManaBar bar = getManaBar(player);
		if (bar == null) return false;
		boolean r = bar.changeMana(amount, reason);
		if (r) showMana(player, showManaOnUse);
		return r;
	}

	@Override
	public boolean removeMana(Player player, int amount, ManaChangeReason reason) {
		return addMana(player, -amount, reason);
	}
	
	@Override
	public boolean setMana(Player player, int amount, ManaChangeReason reason) {
		ManaBar bar = getManaBar(player);
		if (bar == null) return false;
		boolean r = bar.setMana(amount, reason);
		if (r) showMana(player, showManaOnUse);
		return r;
	}

	@Override
	public void showMana(Player player, boolean showInChat) {
		ManaBar bar = getManaBar(player);
		if (bar == null) return;
		if (showInChat) showManaInChat(player, bar);
		if (showManaOnHungerBar) showManaOnHungerBar(player, bar);
		if (showManaOnActionBar) showManaOnActionBar(player, bar);
		if (showManaOnExperienceBar) showManaOnExperienceBar(player, bar);
	}
	
	@Override
	public ModifierSet getModifiers() {
		return modifiers;
	}

	private String getManaMessage(ManaBar bar) {
		int segments = (int) (((double) bar.getMana() / (double) bar.getMaxMana()) * bar.getManaRank().getBarSize());
		StringBuilder text = new StringBuilder(MagicSpells.getTextColor() + bar.getPrefix() + MagicSpells.getTextColor() + " {" + bar.getColorFull());
		int i = 0;
		for (; i < segments; i++) {
			text.append(bar.getManaRank().getSymbol());
		}
		text.append(bar.getColorEmpty());
		for (; i < bar.getManaRank().getBarSize(); i++) {
			text.append(bar.getManaRank().getSymbol());
		}
		text.append(MagicSpells.getTextColor()).append("} [").append(bar.getMana()).append('/').append(bar.getMaxMana()).append(']');

		return text.toString();
	}
	
	private void showManaInChat(Player player, ManaBar bar) {
		player.sendMessage(getManaMessage(bar));
	}
	
	private void showManaOnHungerBar(Player player, ManaBar bar) {
		player.setFoodLevel(Math.round(((float) bar.getMana() / (float) bar.getMaxMana()) * 20));
		player.setSaturation(20);
	}

	private void showManaOnActionBar(Player player, ManaBar bar) {
		MagicSpells.getVolatileCodeHandler().sendActionBarMessage(player, getManaMessage(bar));
	}
	
	private void showManaOnExperienceBar(Player player, ManaBar bar) {
		MagicSpells.getExpBarManager().update(player, bar.getMana(), (float) bar.getMana() / (float) bar.getMaxMana());
	}

	public boolean usingHungerBar() {
		return showManaOnHungerBar;
	}

	public boolean usingActionBar() {
		return showManaOnActionBar;
	}
	
	public boolean usingExperienceBar() {
		return showManaOnExperienceBar;
	}

	@Override
	public void turnOff() {
		ranks.clear();
		manaBars.clear();

		for (Regenerator regenerator : regenerators) {
			MagicSpells.cancelTask(regenerator.taskId);
		}
		regenerators.clear();
	}
	
	private class Regenerator implements Runnable {

		private ManaRank rank;

		private int taskId;

		Regenerator(ManaRank rank, int regenInterval) {
			this.rank = rank;
			taskId = MagicSpells.scheduleRepeatingTask(this, regenInterval, regenInterval);
		}

		@Override
		public void run() {
			for (ManaBar bar : manaBars.values()) {
				if (!bar.getManaRank().equals(rank)) continue;

				boolean r = bar.regenerate();
				if (!r) continue;

				Player p = bar.getPlayer();
				if (p == null) continue;

				showMana(p, showManaOnRegen);
			}
		}
		
	}

}
