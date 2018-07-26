package com.nisovin.magicspells.mana;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.nisovin.magicspells.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.util.MagicConfig;

public class ManaSystem extends ManaHandler {

	private String manaBarPrefix;
	private int manaBarSize;
	private ChatColor manaBarColorFull;
	private ChatColor manaBarColorEmpty;
	private int manaBarToolSlot;
	
	private int regenInterval;
	private int defaultStartingMana;
	private int defaultMaxMana;
	private int defaultRegenAmount;
	
	private boolean showManaOnUse;
	boolean showManaOnRegen;
	private boolean showManaOnWoodTool;
	private boolean showManaOnHungerBar;
	private boolean showManaOnExperienceBar;
	
	private List<String> modifierList;
	private ModifierSet modifiers;
	
	private ManaRank defaultRank;
	private ArrayList<ManaRank> ranks;
	
	HashMap<String, ManaBar> manaBars;
	
	private int taskId = -1;

	public ManaSystem(MagicConfig config) {
		this.manaBarPrefix = config.getString("mana.mana-bar-prefix", "Mana:");
		this.manaBarSize = config.getInt("mana.mana-bar-size", 35);
		this.manaBarColorFull = ChatColor.getByChar(config.getString("mana.color-full", ChatColor.GREEN.getChar() + ""));
		this.manaBarColorEmpty = ChatColor.getByChar(config.getString("mana.color-empty", ChatColor.BLACK.getChar() + ""));
		this.manaBarToolSlot = config.getInt("mana.tool-slot", 8);
		
		this.regenInterval = config.getInt("mana.regen-interval", TimeUtil.TICKS_PER_SECOND);
		this.defaultMaxMana = config.getInt("mana.default-max-mana", 100);
		this.defaultStartingMana = config.getInt("mana.default-starting-mana", this.defaultMaxMana);
		this.defaultRegenAmount = config.getInt("mana.default-regen-amount", 5);
		
		showManaOnUse = config.getBoolean("mana.show-mana-on-use", false);
		showManaOnRegen = config.getBoolean("mana.show-mana-on-regen", false);
		showManaOnWoodTool = config.getBoolean("mana.show-mana-on-wood-tool", true);
		showManaOnHungerBar = config.getBoolean("mana.show-mana-on-hunger-bar", false);
		showManaOnExperienceBar = config.getBoolean("mana.show-mana-on-experience-bar", false);

		modifierList = config.getStringList("mana.modifiers", null);
		
		this.defaultRank = new ManaRank();
		this.defaultRank.name = "default";
		this.defaultRank.startingMana = this.defaultStartingMana;
		this.defaultRank.maxMana = this.defaultMaxMana;
		this.defaultRank.regenAmount = this.defaultRegenAmount;
		this.defaultRank.prefix = this.manaBarPrefix;
		this.defaultRank.colorFull = this.manaBarColorFull;
		this.defaultRank.colorEmpty = this.manaBarColorEmpty;
		
		this.ranks = new ArrayList<>();
		Set<String> rankKeys = config.getKeys("mana.ranks");
		if (rankKeys != null) {
			for (String key : rankKeys) {
				ManaRank r = new ManaRank();
				r.name = key;
				r.maxMana = config.getInt("mana.ranks." + key + ".max-mana", this.defaultMaxMana);
				r.startingMana = config.getInt("mana.ranks." + key + ".starting-mana", this.defaultStartingMana);
				r.regenAmount = config.getInt("mana.ranks." + key + ".regen-amount", this.defaultRegenAmount);
				r.prefix = config.getString("mana.ranks." + key + ".prefix", this.manaBarPrefix);
				r.colorFull = ChatColor.getByChar(config.getString("mana.ranks." + key + ".color-full", this.manaBarColorFull.getChar() + ""));
				r.colorEmpty = ChatColor.getByChar(config.getString("mana.ranks." + key + ".color-empty", this.manaBarColorEmpty.getChar() + ""));
				this.ranks.add(r);
			}
		}
		
		this.manaBars = new HashMap<>();
		this.taskId = MagicSpells.scheduleRepeatingTask(new Regenerator(), this.regenInterval, this.regenInterval);
	}
	
	// DEBUG INFO: level 2, adding mana modifiers
	@Override
	public void initialize() {
		if (this.modifierList != null && !this.modifierList.isEmpty()) {
			MagicSpells.debug(2, "Adding mana modifiers");
			this.modifiers = new ModifierSet(this.modifierList);
			this.modifierList = null;
		}
	}
	
	// DEBUG INFO: level 1, creating mana bar for player playername with rank rankname
	private ManaBar getManaBar(Player player) {
		ManaBar bar = this.manaBars.get(player.getName().toLowerCase());
		if (bar == null) {
			// Create the mana bar
			ManaRank rank = getRank(player);
			bar = new ManaBar(player, rank);
			MagicSpells.debug(1, "Creating mana bar for player " + player.getName() + " with rank " + rank.name);
			this.manaBars.put(player.getName().toLowerCase(), bar);
		}
		return bar;
	}
	
	// DEBUG INFO: level 1, updating mana bar for player playername with rank rankname
	@Override
	public void createManaBar(final Player player) {
		boolean update = this.manaBars.containsKey(player.getName().toLowerCase());
		ManaBar bar = getManaBar(player);
		if (bar != null) {
			if (update) {
				ManaRank rank = getRank(player);
				if (rank != bar.getManaRank()) {
					MagicSpells.debug(1, "Updating mana bar for player " + player.getName() + " with rank " + rank.name);
					bar.setRank(rank);
				}
			}
			MagicSpells.scheduleDelayedTask(() -> showMana(player), 11);
		}
	}
	
	@Override
	public boolean updateManaRankIfNecessary(Player player) {
		if (this.manaBars.containsKey(player.getName().toLowerCase())) {
			ManaBar bar = getManaBar(player);
			ManaRank rank = getRank(player);
			if (bar.getManaRank() != rank) {
				bar.setRank(rank);
				return true;
			}
		} else {
			getManaBar(player);
		}
		return false;
	}
	
	// DEBUG INFO: level 3, fetching mana rank for playername
	// DEBUG INFO: level 3, checking rank rankname
	// DEBUG INFO: level 3, rank found
	// DEBUG INFO: level 3, no rank found
	private ManaRank getRank(Player player) {
		MagicSpells.debug(3, "Fetching mana rank for player " + player.getName() + "...");
		for (ManaRank rank : this.ranks) {
			MagicSpells.debug(3, "    checking rank " + rank.name);
			if (player.hasPermission("magicspells.rank." + rank.name)) {
				MagicSpells.debug(3, "    rank found");
				return rank;
			}
		}
		MagicSpells.debug(3, "    no rank found");
		return this.defaultRank;
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
		if (r) showMana(player, this.showManaOnUse);
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
		if (r) showMana(player, this.showManaOnUse);
		return r;
	}

	@Override
	public void showMana(Player player, boolean showInChat) {
		ManaBar bar = getManaBar(player);
		if (bar == null) return;
		if (showInChat) showManaInChat(player, bar);
		if (this.showManaOnWoodTool) showManaOnWoodTool(player, bar);
		if (this.showManaOnHungerBar) showManaOnHungerBar(player, bar);
		if (this.showManaOnExperienceBar) showManaOnExperienceBar(player, bar);
	}
	
	@Override
	public ModifierSet getModifiers() {
		return modifiers;
	}
	
	private void showManaInChat(Player player, ManaBar bar) {
		int segments = (int)(((double)bar.getMana()/(double)bar.getMaxMana()) * this.manaBarSize);
		String text = MagicSpells.getTextColor() + bar.getPrefix() + " {" + bar.getColorFull();
		int i = 0;
		for (; i < segments; i++) {
			text += "=";
		}
		text += bar.getColorEmpty();
		for (; i < this.manaBarSize; i++) {
			text += "=";
		}
		text += MagicSpells.getTextColor() + "} [" + bar.getMana() + '/' + bar.getMaxMana() + ']';
		player.sendMessage(text);
	}
	
	private void showManaOnWoodTool(Player player, ManaBar bar) {
		ItemStack item = player.getInventory().getItem(manaBarToolSlot);
		if (item == null) return;
		
		Material type = item.getType();
		if (type == Material.WOODEN_AXE || type == Material.WOODEN_HOE || type == Material.WOODEN_PICKAXE || type == Material.WOODEN_SHOVEL || type == Material.WOODEN_SWORD) {
			int dur = 60 - (int)(((double)bar.getMana()/(double)bar.getMaxMana()) * 60);
			if (dur == 60) {
				dur = 59;
			} else if (dur == 0) {
				dur = 1;
			}
			item.setDurability((short)dur);
			player.getInventory().setItem(this.manaBarToolSlot, item);
		}
	}
	
	private void showManaOnHungerBar(Player player, ManaBar bar) {
		player.setFoodLevel(Math.round(((float)bar.getMana()/(float)bar.getMaxMana()) * 20));
		player.setSaturation(20);
	}
	
	private void showManaOnExperienceBar(Player player, ManaBar bar) {
		MagicSpells.getExpBarManager().update(player, bar.getMana(), (float)bar.getMana()/(float)bar.getMaxMana());
	}
	
	public boolean usingExpBar() {
		return this.showManaOnExperienceBar;
	}

	@Override
	public void turnOff() {
		this.ranks.clear();
		this.manaBars.clear();
		if (this.taskId > 0) Bukkit.getScheduler().cancelTask(this.taskId);
	}
	
	public class Regenerator implements Runnable {
		
		@Override
		public void run() {
			for (ManaBar bar : manaBars.values()) {
				boolean r = bar.regenerate();
				if (!r) continue;
				Player p = bar.getPlayer();
				if (p == null) continue;
				showMana(p, showManaOnRegen);
			}
		}
		
	}

}
