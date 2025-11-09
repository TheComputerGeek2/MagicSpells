package com.nisovin.magicspells.mana;

import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.castmodifiers.ModifierSet;

public class ManaSystem extends ManaHandler {

	private final boolean showManaOnUse;
	private final boolean showManaOnRegen;
	private final boolean showManaOnHungerBar;
	private final boolean showManaOnActionBar;
	private final boolean showManaOnExperienceBar;

	private List<String> modifierList;
	private ModifierSet modifiers;

	private final ManaRank defaultRank;
	private final List<ManaRank> ranks;

	private final Map<UUID, ManaBar> manaBars;
	private final Set<Regenerator> regenerators;

	public ManaSystem(MagicConfig config) {
		String path = "mana.";

		String legacyDefaultBarPrefix = Util.getStrictString(config.getString(path + "default-prefix", "Mana:"));
		NamedTextColor legacyDefaultColorFull = Util.getLegacyColor(config.getString(path + "default-color-full", null), NamedTextColor.GREEN);
		NamedTextColor legacyDefaultColorEmpty = Util.getLegacyColor(config.getString(path + "default-color-empty", null), NamedTextColor.BLACK);

		String defaultBarFormat = config.getString(path + "default-bar-format", null);
		boolean legacyFormat = defaultBarFormat == null;
		if (legacyFormat) {
			defaultBarFormat = "<" + MagicSpells.getLegacyTextColor() + ">"
				+ legacyDefaultBarPrefix
				+ " {<" + legacyDefaultColorFull + "><full_segment></" + legacyDefaultColorFull + ">"
				+ "<" + legacyDefaultColorEmpty + "><empty_segment></" + legacyDefaultColorEmpty + ">} [<mana>/<max_mana>]"
				+ "</" + MagicSpells.getLegacyTextColor() + ">";
		}

		char defaultSymbol = config.getString(path + "default-symbol", "=").charAt(0);
		int defaultBarSize = config.getInt(path + "default-size", 35);

		int defaultMaxMana = config.getInt(path + "default-max-mana", 100);
		int defaultStartingMana = config.getInt(path + "default-starting-mana", defaultMaxMana);
		int defaultRegenAmount = config.getInt(path + "default-regen-amount", 5);
		int defaultRegenInterval = config.getInt(path + "default-regen-interval", TimeUtil.TICKS_PER_SECOND);

		showManaOnUse = config.getBoolean(path + "show-mana-on-use", false);
		showManaOnRegen = config.getBoolean(path + "show-mana-on-regen", false);
		showManaOnHungerBar = config.getBoolean(path + "show-mana-on-hunger-bar", false);
		showManaOnActionBar = config.getBoolean(path + "show-mana-on-action-bar", false);
		showManaOnExperienceBar = config.getBoolean(path + "show-mana-on-experience-bar", true);

		modifierList = config.getStringList(path + "modifiers", null);
		defaultRank = new ManaRank("default", defaultSymbol, defaultBarSize, defaultBarFormat, defaultMaxMana, defaultStartingMana, defaultRegenAmount, defaultRegenInterval);

		regenerators = new HashSet<>();
		ranks = new ArrayList<>();
		manaBars = new HashMap<>();

		Set<String> rankKeys = config.getKeys("mana.ranks");
		if (rankKeys != null) {
			for (String key : rankKeys) {
				String keyPath = "mana.ranks." + key + ".";

				String barFormat = config.getString(keyPath + "bar-format", legacyFormat ? null : defaultBarFormat);
				if (barFormat == null) {
					String legacyBarPrefix = Util.getStrictString(config.getString(keyPath + "prefix", null));
					if (legacyBarPrefix == null) legacyBarPrefix = legacyDefaultBarPrefix;

					NamedTextColor legacyColorFull = Util.getLegacyColor(config.getString(keyPath + "color-full", null), legacyDefaultColorFull);
					NamedTextColor legacyColorEmpty = Util.getLegacyColor(config.getString(keyPath + "color-empty", null), legacyDefaultColorEmpty);

					barFormat = "<" + MagicSpells.getLegacyTextColor() + ">"
						+ legacyBarPrefix
						+ " {<" + legacyColorFull + "><full_segment></" + legacyColorFull + ">"
						+ "<" + legacyColorEmpty + "><empty_segment></" + legacyColorEmpty + ">} [<mana>/<max_mana>]"
						+ "</" + MagicSpells.getLegacyTextColor() + ">";
				}

				ManaRank r = new ManaRank();
				r.setName(key);

				r.setSymbol(config.getString(keyPath + "symbol", defaultSymbol + "").charAt(0));
				r.setBarSize(config.getInt(keyPath + "size", defaultBarSize));
				r.setBarFormat(barFormat);

				r.setMaxMana(config.getInt(keyPath + "max-mana", defaultMaxMana));
				r.setRegenAmount(config.getInt(keyPath + "regen-amount", defaultRegenAmount));
				r.setStartingMana(config.getInt(keyPath + "starting-mana", defaultStartingMana));
				r.setRegenInterval(config.getInt(keyPath + "regen-interval", defaultRegenAmount));

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
			modifiers = new ModifierSet(modifierList, true);
			modifierList = null;
		}
	}

	// DEBUG INFO: level 1, creating mana bar for player playerName with rank rankName
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

	// DEBUG INFO: level 1, updating mana bar for player playerName with rank rankName
	@Override
	public void createManaBar(final Player player) {
		boolean update = manaBars.containsKey(player.getUniqueId());
		ManaBar bar = getManaBar(player);
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

	// DEBUG INFO: level 3, fetching mana rank for playerName
	// DEBUG INFO: level 3, checking rank rankName
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
		return bar.getMaxMana();
	}

	@Override
	public void setMaxMana(Player player, int amount) {
		ManaBar bar = getManaBar(player);
		bar.setMaxMana(amount);
	}

	@Override
	public int getRegenAmount(Player player) {
		ManaBar bar = getManaBar(player);
		return bar.getRegenAmount();
	}

	@Override
	public void setRegenAmount(Player player, int amount) {
		ManaBar bar = getManaBar(player);
		bar.setRegenAmount(amount);
	}

	@Override
	public int getMana(Player player) {
		ManaBar bar = getManaBar(player);
		return bar.getMana();
	}

	@Override
	public boolean hasMana(Player player, int amount) {
		ManaBar bar = getManaBar(player);
		return bar.has(amount);
	}

	@Override
	public boolean addMana(Player player, int amount, ManaChangeReason reason) {
		ManaBar bar = getManaBar(player);
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
		boolean r = bar.setMana(amount, reason);
		if (r) showMana(player, showManaOnUse);
		return r;
	}

	@Override
	public void showMana(Player player, boolean showInChat) {
		ManaBar bar = getManaBar(player);
		if (showInChat) showManaInChat(player, bar);
		if (showManaOnHungerBar) showManaOnHungerBar(player, bar);
		if (showManaOnActionBar) showManaOnActionBar(player, bar);
		if (showManaOnExperienceBar) showManaOnExperienceBar(player, bar);
	}

	@Override
	public ModifierSet getModifiers() {
		return modifiers;
	}

	private Component getManaMessage(ManaBar bar) {
		ManaRank rank = bar.getManaRank();

		int mana = bar.getMana();
		int maxMana = bar.getMaxMana();
		int barSize = rank.getBarSize();
		String symbol = String.valueOf(rank.getSymbol());

		double progress = (double) mana / maxMana;
		int segments = (int) (progress * barSize);

		return MiniMessage.miniMessage().deserialize(
			bar.getBarFormat(),
			Placeholder.component("full_segment", Component.text(symbol.repeat(segments))),
			Placeholder.component("empty_segment", Component.text(symbol.repeat(barSize - segments))),
			Placeholder.component("mana", Component.text(mana)),
			Placeholder.component("max_mana", Component.text(maxMana))
		);
	}

	private void showManaInChat(Player player, ManaBar bar) {
		player.sendMessage(getManaMessage(bar));
	}

	private void showManaOnHungerBar(Player player, ManaBar bar) {
		int food = Math.round(((float) bar.getMana() / (float) bar.getMaxMana()) * 20);

		double health = player.isHealthScaled() ?
				player.getHealth() / Util.getMaxHealth(player) * player.getHealthScale() :
				player.getHealth();

		player.sendHealthUpdate(health, food, player.getSaturation());
	}

	private void showManaOnActionBar(Player player, ManaBar bar) {
		player.sendActionBar(getManaMessage(bar));
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
	public void disable() {
		ranks.clear();
		manaBars.clear();

		for (Regenerator regenerator : regenerators) {
			MagicSpells.cancelTask(regenerator.taskId);
		}
		regenerators.clear();
	}

	private class Regenerator implements Runnable {

		private final ManaRank rank;

		private final int taskId;

		private Regenerator(ManaRank rank, int regenInterval) {
			this.rank = rank;
			taskId = MagicSpells.scheduleRepeatingTask(this, regenInterval, regenInterval);
		}

		@Override
		public void run() {
			Iterator<ManaBar> manaBarIterator = manaBars.values().iterator();
			ManaBar manaBar;
			Player player;
			while (manaBarIterator.hasNext()) {
				manaBar = manaBarIterator.next();
				if (!manaBar.getManaRank().equals(rank)) continue;

				if (!manaBar.regenerate()) continue;

				player = manaBar.getPlayer();
				if (player == null) continue;

				showMana(player, showManaOnRegen);
			}
		}

	}

}
