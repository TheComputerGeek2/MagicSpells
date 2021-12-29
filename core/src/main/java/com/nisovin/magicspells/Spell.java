package com.nisovin.magicspells;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.util.Pair;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.util.BlockIterator;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import net.kyori.adventure.text.Component;

import de.slikey.exp4j.Expression;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.IntMap;
import com.nisovin.magicspells.util.TxtUtil;
import com.nisovin.magicspells.util.config.*;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.spelleffects.*;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.util.VariableMod;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.util.InventoryUtil;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.ExperienceUtils;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.handlers.MoneyHandler;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.ValidTargetChecker;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.effecttypes.*;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.managers.VariableManager;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;
import com.nisovin.magicspells.spelleffects.trackers.EffectTracker;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;
import com.nisovin.magicspells.spelleffects.trackers.AsyncEffectTracker;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

import de.slikey.effectlib.Effect;

import com.google.common.collect.Multimap;
import com.google.common.collect.LinkedListMultimap;

public abstract class Spell implements Comparable<Spell>, Listener {

	protected Random random = ThreadLocalRandom.current();

	protected MagicConfig config;

	protected Map<UUID, Long> nextCast;
	protected Map<String, Integer> xpGranted;
	protected Map<String, Integer> xpRequired;
	protected Map<Spell, Float> sharedCooldowns;
	protected Map<String, Map<EffectPosition, List<Runnable>>> callbacks;

	protected Multimap<String, VariableMod> variableModsCast;
	protected Multimap<String, VariableMod> variableModsCasted;
	protected Multimap<String, VariableMod> variableModsTarget;

	protected IntMap<UUID> chargesConsumed;

	protected EnumMap<EffectPosition, List<SpellEffect>> effects;

	protected Set<String> tags;
	protected Set<CastItem> bindableItems;
	protected Set<Material> losTransparentBlocks;
	protected Set<EffectTracker> effectTrackerSet;
	protected Set<AsyncEffectTracker> asyncEffectTrackerSet;

	protected List<String> replaces;
	protected List<String> precludes;
	protected List<String> incantations;
	protected List<String> prerequisites;
	protected List<String> modifierStrings;
	protected List<String> worldRestrictions;
	protected List<String> rawSharedCooldowns;
	protected List<String> targetModifierStrings;
	protected List<String> locationModifierStrings;

	protected List<String> varModsCast;
	protected List<String> varModsCasted;
	protected List<String> varModsTarget;

	protected boolean debug;
	protected boolean obeyLos;
	protected boolean bindable;
	protected boolean beneficial;
	protected boolean helperSpell;
	protected boolean alwaysGranted;
	protected boolean interruptOnMove;
	protected boolean interruptOnCast;
	protected boolean interruptOnDamage;
	protected boolean castWithLeftClick;
	protected boolean castWithRightClick;
	protected boolean usePreciseCooldowns;
	protected boolean interruptOnTeleport;
	protected boolean ignoreGlobalCooldown;
	protected boolean spellPowerAffectsRange;
	protected boolean requireCastItemOnCommand;

	protected CastItem[] castItems;
	protected CastItem[] consumeCastItems;
	protected CastItem[] leftClickCastItems;
	protected CastItem[] rightClickCastItems;

	protected String[] aliases;

	protected String name;
	protected String permName;
	protected String description;
	protected String internalName;
	protected String profilingKey;
	protected String rechargeSound;
	protected String soundOnCooldown;
	protected String danceCastSequence;
	protected String soundMissingReagents;
	protected String spellNameOnInterrupt;

	protected String strCost;
	protected String strCastSelf;
	protected String strCantCast;
	protected String strCantBind;
	protected String strCastStart;
	protected String strCastOthers;
	protected String strOnCooldown;
	protected String strWrongWorld;
	protected String strInterrupted;
	protected String strXpAutoLearned;
	protected String strWrongCastItem;
	protected String strModifierFailed;
	protected String strMissingReagents;

	protected ModifierSet modifiers;
	protected ModifierSet targetModifiers;
	protected ModifierSet locationModifiers;

	protected Subspell spellOnInterrupt;

	protected SpellReagents reagents;

	protected ItemStack spellIcon;

	protected DamageCause targetDamageCause;

	protected ValidTargetList validTargetList;

	protected long nextCastServer;

	protected double targetDamageAmount;

	protected ConfigData<Integer> range;
	protected ConfigData<Integer> minRange;

	protected int charges;
	protected int castTime;
	protected int experience;
	protected int broadcastRange;

	protected float cooldown;
	protected float serverCooldown;

	protected float minCooldown = -1F;
	protected float maxCooldown = -1F;

	public Spell(MagicConfig config, String spellName) {
		this.config = config;
		this.internalName = spellName;

		callbacks = new HashMap<>();
		loadConfigData(config, spellName, "spells");
	}

	private void loadConfigData(MagicConfig config, String spellName, String section) {
		String path = section + '.' + spellName + '.';
		debug = config.getBoolean(path + "debug", false);
		name = config.getString(path + "name", spellName);
		profilingKey = "Spell:" + getClass().getName().replace("com.nisovin.magicspells.spells.", "") + '-' + spellName;
		List<String> temp = config.getStringList(path + "aliases", null);
		if (temp != null) {
			aliases = new String[temp.size()];
			aliases = temp.toArray(aliases);
		}
		helperSpell = config.getBoolean(path + "helper-spell", false);
		alwaysGranted = config.getBoolean(path + "always-granted", false);
		permName = config.getString(path + "permission-name", spellName);
		incantations = config.getStringList(path + "incantations", null);

		// General options
		description = config.getString(path + "description", "");
		if (config.contains(path + "cast-item")) {
			String[] sItems = config.getString(path + "cast-item", "-5").trim().replace(" ", "").split(MagicItemDataParser.DATA_REGEX);
			castItems = setupCastItems(sItems, "Spell '" + internalName + "' has an invalid cast item specified: %i");
		} else if (config.contains(path + "cast-items")) {
			List<String> sItems = config.getStringList(path + "cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			castItems = setupCastItems(sItems.toArray(new String[0]), "Spell '" + internalName + "' has an invalid cast item specified: %i");
		} else castItems = new CastItem[0];

		if (config.contains(path + "left-click-cast-item")) {
			String[] sItems = config.getString(path + "left-click-cast-item", "-5").trim().replace(" ", "").split(MagicItemDataParser.DATA_REGEX);
			leftClickCastItems = setupCastItems(sItems, "Spell '" + internalName + "' has an invalid left click cast item specified: %i");
		} else if (config.contains(path + "left-click-cast-items")) {
			List<String> sItems = config.getStringList(path + "left-click-cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			leftClickCastItems = setupCastItems(sItems.toArray(new String[0]), "Spell '" + internalName + "' has an invalid left click cast item listed: %i");
		} else leftClickCastItems = new CastItem[0];

		if (config.contains(path + "right-click-cast-item")) {
			String[] sItems = config.getString(path + "right-click-cast-item", "-5").trim().replace(" ", "").split(MagicItemDataParser.DATA_REGEX);
			rightClickCastItems = setupCastItems(sItems, "Spell '" + internalName + "' has an invalid right click cast item specified: %i");
		} else if (config.contains(path + "right-click-cast-items")) {
			List<String> sItems = config.getStringList(path + "right-click-cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			rightClickCastItems = setupCastItems(sItems.toArray(new String[0]), "Spell '" + internalName + "' has an invalid right click cast item listed: %i");
		} else rightClickCastItems = new CastItem[0];

		if (config.contains(path + "consume-cast-item")) {
			String[] sItems = config.getString(path + "consume-cast-item", "-5").trim().replace(" ", "").split(MagicItemDataParser.DATA_REGEX);
			consumeCastItems = setupCastItems(sItems, "Spell '" + internalName + "' has an invalid consume cast item specified: %i");
		} else if (config.contains(path + "consume-cast-items")) {
			List<String> sItems = config.getStringList(path + "consume-cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			consumeCastItems = setupCastItems(sItems.toArray(new String[0]), "Spell '" + internalName + "' has an invalid consume cast item listed: %i");
		} else consumeCastItems = new CastItem[0];

		castWithLeftClick = config.getBoolean(path + "cast-with-left-click", MagicSpells.canCastWithLeftClick());
		castWithRightClick = config.getBoolean(path + "cast-with-right-click", MagicSpells.canCastWithRightClick());

		usePreciseCooldowns = config.getBoolean(path + "use-precise-cooldowns", false);

		danceCastSequence = config.getString(path + "dance-cast-sequence", null);
		requireCastItemOnCommand = config.getBoolean(path + "require-cast-item-on-command", false);
		bindable = config.getBoolean(path + "bindable", true);
		List<String> bindables = config.getStringList(path + "bindable-items", null);
		if (bindables != null) {
			bindableItems = new HashSet<>();
			for (String str : bindables) {
				MagicItem magicItem = MagicItems.getMagicItemFromString(str);
				if (magicItem == null) {
					MagicSpells.error("Spell '" + internalName + "' has an invalid bindable cast item specified: " + str);
					continue;
				}

				ItemStack item = magicItem.getItemStack();
				if (item == null) {
					MagicSpells.error("Spell '" + internalName + "' has an invalid bindable cast item specified: " + str);
					continue;
				}
				bindableItems.add(new CastItem(item));
			}
		}
		String iconStr = config.getString(path + "spell-icon", null);
		if (iconStr != null) {
			MagicItem magicItem = MagicItems.getMagicItemFromString(iconStr);
			if (magicItem != null) {
				spellIcon = magicItem.getItemStack();
				if (spellIcon != null && !BlockUtils.isAir(spellIcon.getType())) {
					spellIcon.setAmount(0);
					if (!iconStr.contains("|")) {
						ItemMeta iconMeta = spellIcon.getItemMeta();
						iconMeta.displayName(Component.text(MagicSpells.getTextColor() + name));
						spellIcon.setItemMeta(iconMeta);
					}
				}
			}
		} else spellIcon = null;

		experience = config.getInt(path + "experience", 0);
		broadcastRange = config.getInt(path + "broadcast-range", MagicSpells.getBroadcastRange());

		// Cast time
		castTime = config.getInt(path + "cast-time", 0);
		interruptOnMove = config.getBoolean(path + "interrupt-on-move", true);
		interruptOnCast = config.getBoolean(path + "interrupt-on-cast", true);
		interruptOnDamage = config.getBoolean(path + "interrupt-on-damage", false);
		interruptOnTeleport = config.getBoolean(path + "interrupt-on-teleport", true);
		spellNameOnInterrupt = config.getString(path + "spell-on-interrupt", null);

		// Targeting
		minRange = getConfigDataInt("min-range", 0);
		range = getConfigDataInt("range", 20);
		spellPowerAffectsRange = config.getBoolean(path + "spell-power-affects-range", false);
		obeyLos = config.getBoolean(path + "obey-los", true);
		if (config.contains(path + "can-target")) {
			if (config.isList(path + "can-target")) validTargetList = new ValidTargetList(this, config.getStringList(path + "can-target", null));
			else validTargetList = new ValidTargetList(this, config.getString(path + "can-target", ""));
		} else {
			boolean targetPlayers = config.getBoolean(path + "target-players", true);
			boolean targetNonPlayers = config.getBoolean(path + "target-non-players", true);
			validTargetList = new ValidTargetList(targetPlayers, targetNonPlayers);
		}
		beneficial = config.getBoolean(path + "beneficial", isBeneficialDefault());
		targetDamageCause = null;
		String causeStr = config.getString(path + "target-damage-cause", null);
		if (causeStr != null) {
			for (DamageCause cause : DamageCause.values()) {
				if (!cause.name().equalsIgnoreCase(causeStr)) continue;
				targetDamageCause = cause;
				break;
			}
		}
		targetDamageAmount = config.getDouble(path + "target-damage-amount", 0);
		losTransparentBlocks = MagicSpells.getTransparentBlocks();
		if (config.contains(path + "los-transparent-blocks")) {
			losTransparentBlocks = Util.getMaterialList(config.getStringList(path + "los-transparent-blocks", Collections.emptyList()), HashSet::new);
			losTransparentBlocks.add(Material.AIR);
			losTransparentBlocks.add(Material.CAVE_AIR);
			losTransparentBlocks.add(Material.VOID_AIR);
		}

		// Cooldowns
		String cooldownRange = config.getString(path + "cooldown", "0");
		try {
			cooldown = Float.parseFloat(cooldownRange);
		} catch (NumberFormatException e) {

			// parse min max cooldowns
			String[] cdRange = cooldownRange.split("-");

			try {
				float min = Float.parseFloat(cdRange[0]);
				float max = Float.parseFloat(cdRange[1]);
				if (min > max) {
					minCooldown = max;
					maxCooldown = min;
				} else {
					minCooldown = min;
					maxCooldown = max;
				}
			} catch (NumberFormatException ignored) {}
		}

		serverCooldown = (float) config.getDouble(path + "server-cooldown", 0);
		rawSharedCooldowns = config.getStringList(path + "shared-cooldowns", null);
		ignoreGlobalCooldown = config.getBoolean(path + "ignore-global-cooldown", false);
		charges = config.getInt(path + "charges", 0);
		rechargeSound = config.getString(path + "recharge-sound", "");
		nextCast = new WeakHashMap<>();
		chargesConsumed = new IntMap<>();
		nextCastServer = 0;

		// Modifiers
		modifierStrings = config.getStringList(path + "modifiers", null);
		targetModifierStrings = config.getStringList(path + "target-modifiers", null);
		locationModifierStrings = config.getStringList(path + "location-modifiers", null);

		// Variables
		varModsCast = config.getStringList(path + "variable-mods-cast", null);
		varModsCasted = config.getStringList(path + "variable-mods-casted", null);
		varModsTarget = config.getStringList(path + "variable-mods-target", null);

		// Hierarchy options
		prerequisites = config.getStringList(path + "prerequisites", null);
		replaces = config.getStringList(path + "replaces", null);
		precludes = config.getStringList(path + "precludes", null);
		worldRestrictions = config.getStringList(path + "restrict-to-worlds", null);
		List<String> sXpGranted = config.getStringList(path + "xp-granted", null);
		List<String> sXpRequired = config.getStringList(path + "xp-required", null);
		if (sXpGranted != null) {
			xpGranted = new LinkedHashMap<>();
			for (String s : sXpGranted) {
				String[] split = s.split(" ");
				try {
					int amt = Integer.parseInt(split[1]);
					xpGranted.put(split[0], amt);
				} catch (NumberFormatException e) {
					MagicSpells.error("Error in xp-granted entry for spell '" + internalName + "': " + s);
				}
			}
		}
		if (sXpRequired != null) {
			xpRequired = new LinkedHashMap<>();
			for (String s : sXpRequired) {
				String[] split = s.split(" ");
				try {
					int amt = Integer.parseInt(split[1]);
					xpRequired.put(split[0], amt);
				} catch (NumberFormatException e) {
					MagicSpells.error("Error in xp-required entry for spell '" + internalName + "': " + s);
				}
			}
		}

		soundOnCooldown = config.getString(path + "sound-on-cooldown", MagicSpells.getCooldownSound());
		soundMissingReagents = config.getString(path + "sound-missing-reagents", MagicSpells.getMissingReagentsSound());
		if (soundOnCooldown != null && soundOnCooldown.isEmpty()) soundOnCooldown = null;
		if (soundMissingReagents != null && soundMissingReagents.isEmpty()) soundMissingReagents = null;

		// Strings
		strCost = config.getString(path + "str-cost", null);
		strCantCast = config.getString(path + "str-cant-cast", MagicSpells.getCantCastMessage());
		strCantBind = config.getString(path + "str-cant-bind", null);
		strCastSelf = config.getString(path + "str-cast-self", null);
		strCastStart = config.getString(path + "str-cast-start", null);
		strCastOthers = config.getString(path + "str-cast-others", null);
		strOnCooldown = config.getString(path + "str-on-cooldown", MagicSpells.getOnCooldownMessage());
		strWrongWorld = config.getString(path + "str-wrong-world", MagicSpells.getWrongWorldMessage());
		strInterrupted = config.getString(path + "str-interrupted", null);
		strXpAutoLearned = config.getString(path + "str-xp-auto-learned", MagicSpells.getXpAutoLearnedMessage());
		strWrongCastItem = config.getString(path + "str-wrong-cast-item", strCantCast);
		strModifierFailed = config.getString(path + "str-modifier-failed", null);
		strMissingReagents = config.getString(path + "str-missing-reagents", MagicSpells.getMissingReagentsMessage());
		if (strXpAutoLearned != null) strXpAutoLearned = strXpAutoLearned.replace("%s", name);

		tags = new HashSet<>(config.getStringList(path + "tags", new ArrayList<>()));
		tags.add("spell-class:" + getClass().getCanonicalName());
		tags.add("spell-package:" + getClass().getPackage().getName());
	}

	public Set<String> getTags() {
		return Collections.unmodifiableSet(tags);
	}

	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}

	public String getLoggingSpellPrefix() {
		return '[' + internalName + ']';
	}

	protected SpellReagents getConfigReagents(String option) {
		List<String> costList = config.getStringList("spells." + internalName + '.' + option, null);
		if (costList == null || costList.isEmpty()) return null;

		SpellReagents reagents = new SpellReagents();
		String[] data;

		for (String costVal : costList) {
			try {
				// Parse cost data
				data = costVal.split(" ");

				switch (data[0].toLowerCase()) {
					case "health" -> {
						if (data.length > 1) reagents.setHealth(Double.parseDouble(data[1]));
					}
					case "mana" -> {
						if (data.length > 1) reagents.setMana(Integer.parseInt(data[1]));
					}
					case "hunger" -> {
						if (data.length > 1) reagents.setHunger(Integer.parseInt(data[1]));
					}
					case "experience" -> {
						if (data.length > 1) reagents.setExperience(Integer.parseInt(data[1]));
					}
					case "levels" -> {
						if (data.length > 1) reagents.setLevels(Integer.parseInt(data[1]));
					}
					case "durability" -> {
						if (data.length > 1) reagents.setDurability(Integer.parseInt(data[1]));
					}
					case "money" -> {
						if (data.length > 1) reagents.setMoney(Float.parseFloat(data[1]));
					}
					case "variable" -> {
						if (data.length > 2) reagents.addVariable(data[1], Double.parseDouble(data[2]));
					}

					default -> {
						int amount = 1;
						if (data.length > 1) amount = Integer.parseInt(data[1]);
						MagicItemData itemData = MagicItems.getMagicItemDataFromString(data[0]);
						if (itemData == null) {
							MagicSpells.error("Failed to process cost value for " + internalName + " spell: " + costVal);
							continue;
						}
						reagents.addItem(new SpellReagents.ReagentItem(itemData, amount));
					}
				}
			} catch (Exception e) {
				MagicSpells.error("Failed to process cost value for " + internalName + " spell: " + costVal);
			}
		}

		return reagents;
	}

	protected void initializeVariables() {
		// Variable options
		if (varModsCast != null && !varModsCast.isEmpty()) {
			variableModsCast = LinkedListMultimap.create();
			for (String s : varModsCast) {
				try {
					String[] data = s.split(" ", 2);
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					variableModsCast.put(var, varMod);
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-cast option for spell '" + internalName + "': " + s);
				}
			}
		}
		if (varModsCasted != null && !varModsCasted.isEmpty()) {
			variableModsCasted = LinkedListMultimap.create();
			for (String s : varModsCasted) {
				try {
					String[] data = s.split(" ", 2);
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					variableModsCasted.put(var, varMod);
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-casted option for spell '" + internalName + "': " + s);
				}
			}
		}
		if (varModsTarget != null && !varModsTarget.isEmpty()) {
			variableModsTarget = LinkedListMultimap.create();
			for (String s : varModsTarget) {
				try {
					String[] data = s.split(" ", 2);
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					variableModsTarget.put(var, varMod);
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-target option for spell '" + internalName + "': " + s);
				}
			}
		}

		// Cost
		reagents = getConfigReagents("cost");
		if (reagents == null) reagents = new SpellReagents();
	}

	protected void initializeSpellEffects() {
		// Graphical effects
		String path = "spells" + '.' + internalName + '.';
		effectTrackerSet = new HashSet<>();
		asyncEffectTrackerSet = new HashSet<>();
		if (!config.contains(path + "effects")) return;

		effects = new EnumMap<>(EffectPosition.class);
		if (config.isList(path + "effects")) {
			MagicSpells.error("Warning: Single line effects are deprecated. Usage encountered on spell: " + internalName);
			return;
		}

		if (!config.isSection(path + "effects")) return;
		for (String key : config.getKeys(path + "effects")) {
			ConfigurationSection section = config.getSection(path + "effects." + key);
			if (section == null) {
				MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not contain a configuration section.");
				continue;
			}

			String positionName = section.getString("position", "");
			if (positionName.isEmpty()) {
				MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not contain a 'position' value.");
				continue;
			}

			EffectPosition position = EffectPosition.getPositionFromString(positionName);
			if (position == null) {
				MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not have a valid 'position' defined: " + positionName);
				continue;
			}

			String effectType = section.getString("effect", "");
			if (effectType.isEmpty()) {
				MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not contain an 'effect' value.");
				continue;
			}

			SpellEffect effect = MagicSpells.getSpellEffectManager().getSpellEffectByName(effectType);
			if (effect == null) {
				MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not have a valid 'effect' defined: " + effectType);
				continue;
			}

			effect.loadFromConfiguration(section);

			List<SpellEffect> effectList = effects.computeIfAbsent(position, p -> new ArrayList<>());
			effectList.add(effect);
		}
	}

	// DEBUG INFO: level 2, adding modifiers to internalname
	// DEBUG INFO: level 2, adding target modifiers to internalname
	protected void initializeModifiers() {
		// Modifiers
		if (modifierStrings != null && !modifierStrings.isEmpty()) {
			debug(2, "Adding modifiers to " + internalName + " spell");
			modifiers = new ModifierSet(modifierStrings, this);
			modifierStrings = null;
		}
		if (targetModifierStrings != null && !targetModifierStrings.isEmpty()) {
			debug(2, "Adding target modifiers to " + internalName + " spell");
			targetModifiers = new ModifierSet(targetModifierStrings, this);
			targetModifierStrings = null;
		}
		if (locationModifierStrings != null && !locationModifierStrings.isEmpty()) {
			debug(2, "Adding location modifiers to " + internalName + " spell");
			locationModifiers = new ModifierSet(locationModifierStrings, this);
			locationModifierStrings = null;
		}

		if (effects != null && !effects.isEmpty()) {
			for (EffectPosition position : effects.keySet()) {
				if (position == null) continue;

				List<SpellEffect> spellEffects = effects.get(position);
				if (spellEffects == null || spellEffects.isEmpty()) continue;
				spellEffects.forEach(spellEffect -> spellEffect.initializeModifiers(this));
			}
		}
	}

	/**
	 * This method is called immediately after all spells have been loaded.
	 */
	protected void initialize() {
		// Process shared cooldowns
		if (rawSharedCooldowns != null) {
			sharedCooldowns = new HashMap<>();
			for (String s : rawSharedCooldowns) {
				String[] data = s.split(" ");
				Spell spell = MagicSpells.getSpellByInternalName(data[0]);
				float cd = Float.parseFloat(data[1]);
				if (spell != null) sharedCooldowns.put(spell, cd);
			}
			rawSharedCooldowns.clear();
			rawSharedCooldowns = null;
		}

		// Register events
		registerEvents();

		// Other processing
		if (spellNameOnInterrupt != null && !spellNameOnInterrupt.isEmpty()) {
			spellOnInterrupt = initSubspell(spellNameOnInterrupt, "Spell '" + internalName + "' has an invalid spell-on-interrupt defined!");
		}
	}

	protected boolean configKeyExists(String key) {
		return config.contains("spells." + internalName + '.' + key);
	}

	/**
	 * Access an integer config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected int getConfigInt(String key, int defaultValue) {
		return config.getInt("spells." + internalName + '.' + key, defaultValue);
	}

	/**
	 * Access a long config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected long getConfigLong(String key, long defaultValue) {
		return config.getLong("spells." + internalName + '.' + key, defaultValue);
	}

	/**
	 * Access a boolean config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected boolean getConfigBoolean(String key, boolean defaultValue) {
		return config.getBoolean("spells." + internalName + '.' + key, defaultValue);
	}

	/**
	 * Access a String config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected String getConfigString(String key, String defaultValue) {
		return config.getString("spells." + internalName + '.' + key, defaultValue);
	}

	/**
	 * Access a Vector config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected Vector getConfigVector(String key, String defaultValue) {
		String[] vecStrings = getConfigString(key, defaultValue).split(",");
		return new Vector(Double.parseDouble(vecStrings[0]), Double.parseDouble(vecStrings[1]), Double.parseDouble(vecStrings[2]));
	}

	/**
	 * Access a float config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected float getConfigFloat(String key, float defaultValue) {
		return (float) config.getDouble("spells." + internalName + '.' + key, defaultValue);
	}

	/**
	 * Access a double config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected double getConfigDouble(String key, double defaultValue) {
		return config.getDouble("spells." + internalName + '.' + key, defaultValue);
	}

	protected List<Integer> getConfigIntList(String key, List<Integer> defaultValue) {
		return config.getIntList("spells." + internalName + '.' + key, defaultValue);
	}

	protected List<String> getConfigStringList(String key, List<String> defaultValue) {
		return config.getStringList("spells." + internalName + '.' + key, defaultValue);
	}

	protected Set<String> getConfigKeys(String key) {
		return config.getKeys("spells." + internalName + '.' + key);
	}

	protected ConfigurationSection getConfigSection(String key) {
		return config.getSection("spells." + internalName + '.' + key);
	}

	protected ConfigData<Integer> getConfigDataInt(String key, int def) {
		String path = "spells." + internalName + '.' + key;

		if (config.isInt(path)) {
			int value = config.getInt(path, def);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			Pair<Expression, Boolean> ex = FunctionData.buildExpression(config.getString(path, ""));
			if (ex == null) return (caster, target, power, args) -> def;

			return new FunctionData.IntegerData(ex.getFirst(), def, ex.getSecond());
		}

		return (caster, target, power, args) -> def;
	}

	protected ConfigData<Integer> getConfigDataInt(String key, ConfigData<Integer> def) {
		String path = "spells." + internalName + '.' + key;

		if (config.isInt(path)) {
			int value = config.getInt(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			Pair<Expression, Boolean> ex = FunctionData.buildExpression(config.getString(path, ""));
			if (ex == null) return def;

			return new FunctionData.IntegerData(ex.getFirst(), def, ex.getSecond());
		}

		return def;
	}

	protected ConfigData<Double> getConfigDataDouble(String key, double def) {
		String path = "spells." + internalName + '.' + key;

		if (config.isDouble(path)) {
			double value = config.getDouble(path, def);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			Pair<Expression, Boolean> ex = FunctionData.buildExpression(config.getString(path, ""));
			if (ex == null) return (caster, target, power, args) -> def;

			return new FunctionData.DoubleData(ex.getFirst(), def, ex.getSecond());
		}

		return (caster, target, power, args) -> def;
	}

	protected ConfigData<Double> getConfigDataDouble(String key, ConfigData<Double> def) {
		String path = "spells." + internalName + '.' + key;

		if (config.isDouble(path)) {
			double value = config.getDouble(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			Pair<Expression, Boolean> ex = FunctionData.buildExpression(config.getString(path, ""));
			if (ex == null) return def;

			return new FunctionData.DoubleData(ex.getFirst(), def, ex.getSecond());
		}

		return def;
	}

	protected ConfigData<Float> getConfigDataFloat(String key, float def) {
		String path = "spells." + internalName + '.' + key;

		if (config.isDouble(path)) {
			float value = (float) config.getDouble(path, def);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			Pair<Expression, Boolean> ex = FunctionData.buildExpression(config.getString(path, ""));
			if (ex == null) return (caster, target, power, args) -> def;

			return new FunctionData.FloatData(ex.getFirst(), def, ex.getSecond());
		}

		return (caster, target, power, args) -> def;
	}

	protected ConfigData<Float> getConfigDataFloat(String key, ConfigData<Float> def) {
		String path = "spells." + internalName + '.' + key;

		if (config.isDouble(path)) {
			float value = (float) config.getDouble(path);
			return (caster, target, power, args) -> value;
		}

		if (config.isString(path)) {
			Pair<Expression, Boolean> ex = FunctionData.buildExpression(config.getString(path, ""));
			if (ex == null) return def;

			return new FunctionData.FloatData(ex.getFirst(), def, ex.getSecond());
		}

		return def;
	}

	protected ConfigData<String> getConfigDataString(String key, String def) {
		String value = config.getString("spells." + internalName + '.' + key, def);

		boolean argReplacement = value.contains("%arg");
		boolean varReplacement = value.contains("%var") || value.contains("%playervar");

		boolean targeted = value.contains("%targetvar");
		boolean targetedReplacement = targeted || value.contains("%castervar");

		if (argReplacement || varReplacement || targetedReplacement)
			return new StringData(value, varReplacement, targetedReplacement, argReplacement, targeted);

		return (caster, target, power, args) -> value;
	}

	protected boolean isConfigString(String key) {
		return config.isString("spells." + internalName + '.' + key);
	}

	protected boolean isConfigSection(String key) {
		return config.isSection("spells." + internalName + '.' + key);
	}

	public final SpellCastResult cast(LivingEntity livingEntity) {
		return cast(livingEntity, 1.0F, null);
	}

	// TODO can this safely be made varargs?
	public final SpellCastResult cast(LivingEntity livingEntity, String[] args) {
		return cast(livingEntity, 1.0F, args);
	}

	// TODO can this safely be made varargs?
	public final SpellCastResult cast(LivingEntity livingEntity, float power, String[] args) {
		SpellCastEvent spellCast = preCast(livingEntity, power, args);
		if (spellCast == null) return new SpellCastResult(SpellCastState.CANT_CAST, PostCastAction.HANDLE_NORMALLY);
		PostCastAction action;
		int castTime = spellCast.getCastTime();
		if (castTime <= 0 || spellCast.getSpellCastState() != SpellCastState.NORMAL) action = handleCast(spellCast);
		else if (!preCastTimeCheck(livingEntity, args)) action = PostCastAction.ALREADY_HANDLED;
		else {
			action = PostCastAction.DELAYED;
			sendMessage(strCastStart, livingEntity, args);
			playSpellEffects(EffectPosition.START_CAST, livingEntity);
			if (MagicSpells.useExpBarAsCastTimeBar()) new DelayedSpellCastWithBar(spellCast);
			else new DelayedSpellCast(spellCast);
		}
		return new SpellCastResult(spellCast.getSpellCastState(), action);
	}

	protected SpellCastState getCastState(LivingEntity livingEntity) {
		if (livingEntity instanceof Player && !MagicSpells.getSpellbook((Player) livingEntity).canCast(this)) return SpellCastState.CANT_CAST;
		if (worldRestrictions != null && !worldRestrictions.contains(livingEntity.getWorld().getName())) return SpellCastState.WRONG_WORLD;
		if (MagicSpells.getNoMagicZoneManager() != null && MagicSpells.getNoMagicZoneManager().willFizzle(livingEntity, this)) return SpellCastState.NO_MAGIC_ZONE;
		if (onCooldown(livingEntity)) return SpellCastState.ON_COOLDOWN;
		if (!hasReagents(livingEntity)) return SpellCastState.MISSING_REAGENTS;
		return SpellCastState.NORMAL;
	}

	// TODO can this safely be made varargs?
	// DEBUG INFO: level 2, spell cast state
	// DEBUG INFO: level 2, spell canceled
	// DEBUG INFO: level 2, spell cast state changed
	protected SpellCastEvent preCast(LivingEntity livingEntity, float power, String[] args) {
		// Get spell state
		SpellCastState state = getCastState(livingEntity);
		debug(2, "    Spell cast state: " + state);

		// Call events
		SpellCastEvent event = new SpellCastEvent(this, livingEntity, state, power, args, cooldown, reagents.clone(), castTime);
		EventUtil.call(event);
		if (event.isCancelled()) {
			debug(2, "    Spell cancelled");
			return null;
		}

		if (event.haveReagentsChanged()) {
			boolean hasReagents = hasReagents(livingEntity, event.getReagents());
			if (!hasReagents && state != SpellCastState.MISSING_REAGENTS) {
				event.setSpellCastState(SpellCastState.MISSING_REAGENTS);
				debug(2, "    Spell cast state changed: " + state);
			} else if (hasReagents && state == SpellCastState.MISSING_REAGENTS) {
				event.setSpellCastState(state = SpellCastState.NORMAL);
				debug(2, "    Spell cast state changed: " + state);
			}
		}

		if (event.hasSpellCastStateChanged()) debug(2, "    Spell cast state changed: " + state);
		if (Perm.NO_CAST_TIME.has(livingEntity)) event.setCastTime(0);
		return event;
	}

	// DEBUG INFO: level 3, power #
	// DEBUG INFO: level 3, cooldown #
	// DEBUG INFO: level 3, args argsvalue
	PostCastAction handleCast(SpellCastEvent spellCast) {
		long start = System.nanoTime();
		LivingEntity caster = spellCast.getCaster();
		SpellCastState state = spellCast.getSpellCastState();
		String[] args = spellCast.getSpellArgs();
		float power = spellCast.getPower();
		debug(3, "    Power: " + power);
		debug(3, "    Cooldown: " + cooldown);
		if (MagicSpells.isDebug() && args != null && args.length > 0) debug(3, "    Args: {" + Util.arrayJoin(args, ',') + '}');
		PostCastAction action = castSpell(caster, state, power, args);
		if (MagicSpells.hasProfilingEnabled()) {
			Long total = MagicSpells.getProfilingTotalTime().get(profilingKey);
			if (total == null) total = (long) 0;
			total += System.nanoTime() - start;
			MagicSpells.getProfilingTotalTime().put(profilingKey, total);
			Integer runs = MagicSpells.getProfilingRuns().get(profilingKey);
			if (runs == null) runs = 0;
			runs += 1;
			MagicSpells.getProfilingRuns().put(profilingKey, runs);
		}
		postCast(spellCast, action);
		return action;
	}

	// FIXME save the results of the redundant calculations or be cleaner about it
	// DEBUG INFO: level 3, post cast action actionName
	protected void postCast(SpellCastEvent spellCast, PostCastAction action) {
		debug(3, "    Post-cast action: " + action);
		LivingEntity caster = spellCast.getCaster();
		SpellCastState state = spellCast.getSpellCastState();
		if (action != null && action != PostCastAction.ALREADY_HANDLED) {
			if (state == SpellCastState.NORMAL) {
				if (action.setCooldown()) setCooldown(caster, spellCast.getCooldown());
				if (action.chargeReagents()) removeReagents(caster, spellCast.getReagents());
				if (action.sendMessages()) sendMessages(caster, spellCast.getSpellArgs());
				if (experience > 0 && caster instanceof Player player) player.giveExp(experience);
			} else if (state == SpellCastState.ON_COOLDOWN) {
				MagicSpells.sendMessageAndFormat(strOnCooldown, caster, spellCast.getSpellArgs(),
					"%c", Math.round(getCooldown(caster)) + "", "%s", spellCast.getSpell().getName());
				playSpellEffects(EffectPosition.COOLDOWN, caster);
				if (soundOnCooldown != null && caster instanceof Player player) player.playSound(caster.getLocation(), soundOnCooldown, 1F, 1F);
			} else if (state == SpellCastState.MISSING_REAGENTS) {
				MagicSpells.sendMessage(strMissingReagents, caster, spellCast.getSpellArgs());
				playSpellEffects(EffectPosition.MISSING_REAGENTS, caster);
				if (MagicSpells.showStrCostOnMissingReagents() && strCost != null && !strCost.isEmpty()) MagicSpells.sendMessage("    (" + strCost + ')', caster, spellCast.getSpellArgs());
				if (soundMissingReagents != null && caster instanceof Player player) player.playSound(caster.getLocation(), soundMissingReagents, 1F, 1F);
			} else if (state == SpellCastState.CANT_CAST) {
				MagicSpells.sendMessage(strCantCast, caster, spellCast.getSpellArgs());
			} else if (state == SpellCastState.NO_MAGIC_ZONE) {
				MagicSpells.getNoMagicZoneManager().sendNoMagicMessage(caster, this);
			} else if (state == SpellCastState.WRONG_WORLD) {
				MagicSpells.sendMessage(strWrongWorld, caster, spellCast.getSpellArgs());
			}
		}
		SpellCastedEvent event = new SpellCastedEvent(this, caster, state, spellCast.getPower(), spellCast.getSpellArgs(), cooldown, reagents, action);
		EventUtil.call(event);
	}

	// TODO can this safely be made varargs?
	public void sendMessages(LivingEntity livingEntity, String[] args) {
		sendMessage(strCastSelf, livingEntity, args, "%a", livingEntity.getName());
		sendMessageNear(livingEntity, formatMessage(strCastOthers, "%a", livingEntity.getName()));
	}

	// TODO can this safely be made varargs?
	protected boolean preCastTimeCheck(LivingEntity livingEntity, String[] args) {
		return true;
	}

	// TODO can this safely be made varargs?
	/**
	 * This method is called when a player casts a spell, either by command, with a wand item, or otherwise.
	 * @param caster the living entity casting the spell
	 * @param state the state of the spell cast (normal, on cooldown, missing reagents, etc)
	 * @param power the power multiplier the spell should be cast with (1.0 is normal)
	 * @param args the spell arguments, if cast by command
	 * @return the action to take after the spell is processed
	 */
	public abstract PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args);

	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}

	protected List<String> tabCompletePlayerName(CommandSender sender, String partial) {
		List<String> matches = new ArrayList<>();
		partial = partial.toLowerCase();
		// TODO stream this
		for (Player p : Bukkit.getOnlinePlayers()) {
			String name = p.getName();
			if (!name.toLowerCase().startsWith(partial)) continue;
			if (sender.isOp() || !(sender instanceof Player player) || player.canSee(p)) matches.add(name);
		}
		if (!matches.isEmpty()) return matches;
		return null;
	}

	protected List<String> tabCompleteSpellName(CommandSender sender, String partial) {
		return TxtUtil.tabCompleteSpellName(sender, partial);
	}

	// TODO can this safely be made varargs?
	/**
	 * This method is called when the spell is cast from the console.
	 * @param sender the console sender.
	 * @param args the command arguments
	 * @return true if the spell was handled, false otherwise
	 */
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

	public abstract boolean canCastWithItem();

	public abstract boolean canCastByCommand();

	public boolean canCastWithLeftClick() {
		return castWithLeftClick;
	}

	public boolean canCastWithRightClick() {
		return castWithRightClick;
	}

	public boolean hasPreciseCooldowns() {
		return usePreciseCooldowns;
	}

	public boolean isAlwaysGranted() {
		return alwaysGranted;
	}

	public boolean isIgnoringGlobalCooldown() {
		return ignoreGlobalCooldown;
	}

	public boolean isValidItemForCastCommand(ItemStack item) {
		if (!requireCastItemOnCommand || castItems == null) return true;
		if (item == null && castItems.length == 1 && BlockUtils.isAir(castItems[0].getType())) return true;
		for (CastItem castItem : castItems) {
			if (castItem.equals(new CastItem(item))) return true;
		}
		return false;
	}

	public boolean canBind(CastItem item) {
		if (!bindable) return false;
		if (bindableItems == null) return true;
		return bindableItems.contains(item);
	}

	public ItemStack getSpellIcon() {
		return spellIcon;
	}

	public String getCostStr() {
		if (strCost == null || strCost.isEmpty()) return null;
		return strCost;
	}

	/**
	 * Check whether this spell is currently on cooldown for the specified player
	 * @param livingEntity The living entity to check
	 * @return whether the spell is on cooldown
	 */
	public boolean onCooldown(LivingEntity livingEntity) {
		if (Perm.NO_COOLDOWN.has(livingEntity)) return false;
		if (charges > 0) return chargesConsumed.get(livingEntity.getUniqueId()) >= charges;
		if (serverCooldown > 0 && nextCastServer > System.currentTimeMillis()) return true;

		Long next = nextCast.get(livingEntity.getUniqueId());
		return next != null && next > System.currentTimeMillis();
	}

	public float getCooldown() {
		return cooldown;
	}

	/**
	 * Get how many seconds remain on the cooldown of this spell for the specified player
	 * @param livingEntity The living entity to check
	 * @return The number of seconds remaining in the cooldown
	 */
	public float getCooldown(LivingEntity livingEntity) {
		if (charges > 0) return -1;

		float cd = 0;

		Long next = nextCast.get(livingEntity.getUniqueId());
		if (next != null) {
			float c = (next - System.currentTimeMillis()) / ((float) TimeUtil.MILLISECONDS_PER_SECOND);
			cd =  c > 0 ? c : 0;
		}

		if (serverCooldown > 0 && nextCastServer > System.currentTimeMillis()) {
			float c = (nextCastServer - System.currentTimeMillis()) / ((float) TimeUtil.MILLISECONDS_PER_SECOND);
			if (c > cd) cd = c;
		}

		return cd;
	}

	/**
	 * Begins the cooldown for the spell for the specified player
	 * @param livingEntity The living entity to set the cooldown for
	 */
	public void setCooldown(LivingEntity livingEntity, float cooldown) {
		setCooldown(livingEntity, cooldown, true);
	}

	/**
	 * Begins the cooldown for the spell for the specified player
	 * @param livingEntity The living entity to set the cooldown for
	 */
	public void setCooldown(final LivingEntity livingEntity, float cooldown, boolean activateSharedCooldowns) {
		if (cooldown > 0 || minCooldown > 0) {
			float cd = cooldown;
			// calculate random cooldown
			if (minCooldown != -1F) {
				if (usePreciseCooldowns) cd = minCooldown + (maxCooldown - minCooldown) * random.nextFloat();
				else cd = minCooldown + random.nextInt((int) maxCooldown - (int) minCooldown + 1);
			}

			if (charges <= 0) {
				nextCast.put(livingEntity.getUniqueId(), System.currentTimeMillis() + (long) (cd * TimeUtil.MILLISECONDS_PER_SECOND));
			} else {
				final UUID uuid = livingEntity.getUniqueId();
				chargesConsumed.increment(uuid);
				MagicSpells.scheduleDelayedTask(() -> {
					chargesConsumed.decrement(uuid);
					playSpellEffects(EffectPosition.CHARGE_USE, livingEntity);
					if (rechargeSound == null) return;
					if (rechargeSound.isEmpty()) return;
					if (livingEntity instanceof Player player) player.playSound(livingEntity.getLocation(), rechargeSound, 1.0F, 1.0F);
				}, Math.round(TimeUtil.TICKS_PER_SECOND * cd));
			}

		} else {
			if (charges <= 0) nextCast.remove(livingEntity.getUniqueId());
			else chargesConsumed.remove(livingEntity.getUniqueId());
		}
		if (serverCooldown > 0) nextCastServer = System.currentTimeMillis() + (long) (serverCooldown * TimeUtil.MILLISECONDS_PER_SECOND);
		if (activateSharedCooldowns && sharedCooldowns != null) {
			for (Map.Entry<Spell, Float> scd : sharedCooldowns.entrySet()) {
				scd.getKey().setCooldown(livingEntity, scd.getValue(), false);
			}
		}
	}

	/**
	 * Checks if a player has the reagents required to cast this spell
	 * @param livingEntity the living entity to check
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(LivingEntity livingEntity) {
		return hasReagents(livingEntity, reagents);
	}

	// FIXME this doesn't seem strictly tied to Spell logic, could probably be moved
	/**
	 * Checks if a player has the reagents required to cast this spell
	 * @param livingEntity the living entity to check
	 * @param reagents the reagents to check for
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(LivingEntity livingEntity, SpellReagents reagents) {
		if (reagents == null) return true;
		return hasReagents(livingEntity, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels(), reagents.getDurability(), reagents.getMoney(), reagents.getVariables());
	}

	/**
	 * Checks if a player has the specified reagents, including health and mana
	 * @param livingEntity the living entity to check
	 * @param reagents the inventory item reagents to look for
	 * @param healthCost the health cost, in half-hearts
	 * @param manaCost the mana cost
	 * @return true if the player has all the reagents, false otherwise
	 */
	private boolean hasReagents(LivingEntity livingEntity, SpellReagents.ReagentItem[] reagents, double healthCost, int manaCost, int hungerCost, int experienceCost, int levelsCost, int durabilityCost, float moneyCost, Map<String, Double> variables) {
		// Is the livingEntity exempt from reagent costs?
		if (Perm.NO_REAGENTS.has(livingEntity)) return true;

		// player reagents
		if (livingEntity instanceof Player player) {
			// Mana costs
			if (manaCost > 0 && (MagicSpells.getManaHandler() == null || !MagicSpells.getManaHandler().hasMana(player, manaCost))) return false;

			// Hunger costs
			if (hungerCost > 0 && player.getFoodLevel() < hungerCost) return false;

			// Experience costs
			if (experienceCost > 0 && !ExperienceUtils.hasExp(player, experienceCost)) return false;

			// Level costs
			if (levelsCost > 0 && player.getLevel() < levelsCost) return false;

			// Money costs
			if (moneyCost > 0) {
				MoneyHandler moneyHandler = MagicSpells.getMoneyHandler();
				if (moneyHandler == null || !moneyHandler.hasMoney(player, moneyCost)) {
					return false;
				}
			}

			// Variable costs
			if (variables != null) {
				VariableManager varMan = MagicSpells.getVariableManager();
				if (varMan == null) return false;
				for (Map.Entry<String, Double> var : variables.entrySet()) {
					double val = var.getValue();
					if (val > 0 && varMan.getValue(var.getKey(), player) < val) return false;
				}
			}
		}

		// Health costs
		if (healthCost > 0 && livingEntity.getHealth() <= healthCost) return false;

		// Durabilty costs
		if (durabilityCost > 0) {
			// Durability cost is charged from the main hand item
			EntityEquipment equipment = livingEntity.getEquipment();
			if (equipment == null) return false;
			ItemStack inHand = equipment.getItemInMainHand();
			if (!(inHand.getItemMeta() instanceof Damageable damageable)) return false;
			if (damageable.getDamage() >= inHand.getType().getMaxDurability()) return false;
		}

		// Item costs
		if (reagents != null) {
			if (livingEntity instanceof Player player) {
				Inventory inventory = player.getInventory();
				for (SpellReagents.ReagentItem item : reagents) {
					if (item == null) continue;
					if (InventoryUtil.inventoryContains(inventory, item)) continue;
					return false;
				}
			} else {
				EntityEquipment entityEquipment = livingEntity.getEquipment();
				for (SpellReagents.ReagentItem item : reagents) {
					if (item == null) continue;
					if (InventoryUtil.inventoryContains(entityEquipment, item)) continue;
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Removes the reagent cost of this spell from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param livingEntity the living entity to remove reagents from
	 */
	protected void removeReagents(LivingEntity livingEntity) {
		removeReagents(livingEntity, reagents);
	}

	// TODO can this safely be made varargs?
	/**
	 * Removes the specified reagents from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param livingEntity the living entity to remove the reagents from
	 * @param reagents the inventory item reagents to remove
	 */
	protected void removeReagents(LivingEntity livingEntity, SpellReagents.ReagentItem[] reagents) {
		removeReagents(livingEntity, reagents, 0, 0, 0, 0, 0, 0, 0, null);
	}

	protected void removeReagents(LivingEntity livingEntity, SpellReagents reagents) {
		removeReagents(livingEntity, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels(), reagents.getDurability(), reagents.getMoney(), reagents.getVariables());
	}

	/**
	 * Removes the specified reagents, including health and mana, from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param livingEntity the living entity to remove the reagents from
	 * @param reagents the inventory item reagents to remove
	 * @param healthCost the health to remove
	 * @param manaCost the mana to remove
	 */
	private void removeReagents(LivingEntity livingEntity, SpellReagents.ReagentItem[] reagents, double healthCost, int manaCost, int hungerCost, int experienceCost, int levelsCost, int durabilityCost, float moneyCost, Map<String, Double> variables) {
		if (Perm.NO_REAGENTS.has(livingEntity)) return;

		if (reagents != null) {
			for (SpellReagents.ReagentItem item : reagents) {
				if (item == null) continue;
				if (livingEntity instanceof Player player) Util.removeFromInventory(player.getInventory(), item);
				else if (livingEntity.getEquipment() != null) Util.removeFromInventory(livingEntity.getEquipment(), item);
			}
		}

		if (livingEntity instanceof Player player) {
			if (manaCost != 0) MagicSpells.getManaHandler().addMana(player, -manaCost, ManaChangeReason.SPELL_COST);

			if (hungerCost != 0) {
				int f = player.getFoodLevel() - hungerCost;
				if (f < 0) f = 0;
				if (f > 20) f = 20;
				player.setFoodLevel(f);
			}

			if (experienceCost != 0) ExperienceUtils.changeExp(player, -experienceCost);

			if (moneyCost != 0) {
				MoneyHandler moneyHandler = MagicSpells.getMoneyHandler();
				if (moneyHandler != null) {
					if (moneyCost > 0) moneyHandler.removeMoney(player, moneyCost);
					else moneyHandler.addMoney(player, moneyCost);
				}
			}

			if (levelsCost != 0) {
				int lvl = player.getLevel() - levelsCost;
				if (lvl < 0) lvl = 0;
				player.setLevel(lvl);
			}

			if (variables != null) {
				VariableManager varMan = MagicSpells.getVariableManager();
				if (varMan != null) {
					for (Map.Entry<String, Double> var : variables.entrySet()) {
						varMan.set(var.getKey(), player, varMan.getValue(var.getKey(), player) - var.getValue());
					}
				}
			}
		}

		if (healthCost != 0) {
			double h = livingEntity.getHealth() - healthCost;
			if (h < 0) h = 0;
			if (h > Util.getMaxHealth(livingEntity)) h = Util.getMaxHealth(livingEntity);
			livingEntity.setHealth(h);
		}

		if (durabilityCost != 0) {
			EntityEquipment eq = livingEntity.getEquipment();

			if (eq != null) {
				ItemStack item =  eq.getItemInMainHand();
				ItemMeta meta = item.getItemMeta();

				int maxDurability = item.getType().getMaxDurability();
				if (maxDurability > 0 && meta instanceof Damageable damageable) {
					int damage = damageable.getDamage() + durabilityCost;
					damage = Math.max(Math.min(damage, maxDurability), 0);

					damageable.setDamage(damage);
					item.setItemMeta(meta);
				}
			}
		}
	}

	public EnumMap<EffectPosition, List<SpellEffect>> getEffects() {
		return effects;
	}

	protected int getRange(float power) {
		return getRange(null, power, null);
	}

	protected int getRange(LivingEntity caster, float power, String[] args) {
		int range = this.range.get(caster, null, power, args);
		return spellPowerAffectsRange ? Math.round(range * power) : range;
	}

	public int getCharges() {
		return charges;
	}

	/**
	 * Get how many charges the specified living entity has consumed.
	 * @param livingEntity The living entity to check
	 * @return The number of charges consumed
	 */
	public int getCharges(LivingEntity livingEntity) {
		return chargesConsumed.get(livingEntity.getUniqueId());
	}

	/**
	 * Gets the player a player is currently looking at, ignoring other living entities
	 * @param livingEntity the living entity to get the target for
	 * @return the targeted Player, or null if none was found
	 */
	protected TargetInfo<Player> getTargetedPlayer(LivingEntity livingEntity, float power) {
		return getTargetedPlayer(livingEntity, power, null);
	}

	/**
	 * Gets the player a player is currently looking at, ignoring other living entities
	 * @param livingEntity the living entity to get the target for
	 * @return the targeted Player, or null if none was found
	 */
	protected TargetInfo<Player> getTargetedPlayer(LivingEntity livingEntity, float power, String[] args) {
		TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power, true, null, args);
		if (target == null) return null;
		if (!(target.getTarget() instanceof Player)) return null;
		return new TargetInfo<>((Player) target.getTarget(), target.getPower());
	}

	protected TargetInfo<Player> getTargetPlayer(LivingEntity caster, float power) {
		return getTargetedPlayer(caster, power, null);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power) {
		return getTargetedEntity(caster, power, false, null, null);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power, String[] args) {
		return getTargetedEntity(caster, power, false, null, args);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power, ValidTargetChecker checker) {
		return getTargetedEntity(caster, power, false, checker, null);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power, ValidTargetChecker checker, String[] args) {
		return getTargetedEntity(caster, power, false, checker, args);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power, boolean forceTargetPlayers, ValidTargetChecker checker) {
		return getTargetedEntity(caster, power, forceTargetPlayers, checker, null);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power, boolean forceTargetPlayers, ValidTargetChecker checker, String[] args) {
		int currentRange = getRange(power);
		List<Entity> nearbyEntities = caster.getNearbyEntities(currentRange, currentRange, currentRange);

		// Get valid targets
		List<LivingEntity> entities;
		if (MagicSpells.checkWorldPvpFlag() && validTargetList.canTargetPlayers() && !isBeneficial() && !caster.getWorld().getPVP()) {
			entities = validTargetList.filterTargetListCastingAsLivingEntities(caster, nearbyEntities, false);
		} else if (forceTargetPlayers) {
			entities = validTargetList.filterTargetListCastingAsLivingEntities(caster, nearbyEntities, true);
		} else {
			entities = validTargetList.filterTargetListCastingAsLivingEntities(caster, nearbyEntities);
		}

		// Find target
		BlockIterator blockIterator;
		try {
			blockIterator = new BlockIterator(caster, currentRange);
		} catch (IllegalStateException e) {
			DebugHandler.debugIllegalState(e);
			return null;
		}

		Block block;
		Location location;

		int blockX;
		int blockY;
		int blockZ;

		double entityX;
		double entityY;
		double entityZ;

		// How far can a target be from the line of sight along the x, y, and z directions
		double xLower = 0.75;
		double xUpper = 1.75;
		double yLower = 1;
		double yUpper = 2.5;
		double zLower = 0.75;
		double zUpper = 1.75;

		// Do min range
		for (int i = 0; i < minRange.get(caster, null, power, args) && blockIterator.hasNext(); i++) {
			blockIterator.next();
		}

		Set<Entity> blacklistedEntities = new HashSet<>();

		// Loop through player's line of sight
		while (blockIterator.hasNext()) {
			block = blockIterator.next();
			blockX = block.getX();
			blockY = block.getY();
			blockZ = block.getZ();

			// Line of sight is broken, stop without target
			if (obeyLos && !BlockUtils.isTransparent(this, block)) break;

			// Check for entities near this block in the line of sight
			for (LivingEntity target : entities) {
				if (blacklistedEntities.contains(target)) continue;
				location = target.getLocation();
				entityX = location.getX();
				entityY = location.getY();
				entityZ = location.getZ();

				if (!(blockX - xLower <= entityX && entityX <= blockX + xUpper)) continue;
				if (!(blockY - yLower <= entityY && entityY <= blockY + yUpper)) continue;
				if (!(blockZ - zLower <= entityZ && entityZ <= blockZ + zUpper)) continue;

				// Check for invalid target
				if (target instanceof Player && (((Player) target).getGameMode() == GameMode.CREATIVE || ((Player) target).getGameMode() == GameMode.SPECTATOR)) {
					blacklistedEntities.add(target);
					continue;
				}

				// Check for no-magic-zone
				if (MagicSpells.getNoMagicZoneManager() != null && MagicSpells.getNoMagicZoneManager().willFizzle(target.getLocation(), this)) {
					blacklistedEntities.add(target);
					continue;
				}

				// Check for teams
				if (target instanceof Player && MagicSpells.checkScoreboardTeams()) {
					Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

					Team playerTeam = null;
					if (caster instanceof Player) playerTeam = scoreboard.getEntryTeam(caster.getName());
					Team targetTeam = scoreboard.getEntryTeam(target.getName());

					if (playerTeam != null && targetTeam != null) {
						if (playerTeam.equals(targetTeam)) {
							if (!playerTeam.allowFriendlyFire() && !isBeneficial()) {
								blacklistedEntities.add(target);
								continue;
							}
						} else if (isBeneficial()) {
							blacklistedEntities.add(target);
							continue;
						}
					}
				}

				// Call event listeners
				SpellTargetEvent spellTargetEvent = new SpellTargetEvent(this, caster, target, power, args);
				EventUtil.call(spellTargetEvent);
				if (spellTargetEvent.isCancelled()) {
					blacklistedEntities.add(target);
					continue;
				} else {
					target = spellTargetEvent.getTarget();
					power = spellTargetEvent.getPower();
				}

				// Call damage event
				if (targetDamageCause != null) {
					EntityDamageByEntityEvent entityDamageEvent = new MagicSpellsEntityDamageByEntityEvent(caster, target, targetDamageCause, targetDamageAmount, this);
					EventUtil.call(entityDamageEvent);
					if (entityDamageEvent.isCancelled()) {
						blacklistedEntities.add(target);
						continue;

					}
				}

				// Run checker
				if (target != null && checker != null && !checker.isValidTarget(target)) {
					blacklistedEntities.add(target);
					continue;
				}

				return new TargetInfo<>(target, power);
			}

		}
		return null;
	}

	protected Block getTargetedBlock(LivingEntity entity, float power) {
		return BlockUtils.getTargetBlock(this, entity, getRange(entity, power, null));
	}

	protected Block getTargetedBlock(LivingEntity entity, float power, String[] args) {
		return BlockUtils.getTargetBlock(this, entity, getRange(entity, power, args));
	}

	protected List<Block> getLastTwoTargetedBlocks(LivingEntity entity, float power) {
		return BlockUtils.getLastTwoTargetBlock(this, entity, getRange(entity, power, null));
	}

	protected List<Block> getLastTwoTargetedBlocks(LivingEntity entity, float power, String[] args) {
		return BlockUtils.getLastTwoTargetBlock(this, entity, getRange(entity, power, args));
	}

	public Set<Material> getLosTransparentBlocks() {
		return losTransparentBlocks;
	}

	public boolean isTransparent(Block block) {
		return losTransparentBlocks.contains(block.getType());
	}

	protected void playSpellEffects(Entity pos1, Entity pos2) {
		playSpellEffects(EffectPosition.CASTER, pos1);
		playSpellEffects(EffectPosition.TARGET, pos2);
		playSpellEffectsTrail(pos1.getLocation(), pos2.getLocation());
	}

	protected void playSpellEffects(Entity pos1, Location pos2) {
		playSpellEffects(EffectPosition.CASTER, pos1);
		playSpellEffects(EffectPosition.TARGET, pos2);
		playSpellEffectsTrail(pos1.getLocation(), pos2);
	}

	protected void playSpellEffects(Location pos1, Entity pos2) {
		playSpellEffects(EffectPosition.CASTER, pos1);
		playSpellEffects(EffectPosition.TARGET, pos2);
		playSpellEffectsTrail(pos1, pos2.getLocation());
	}

	protected void playSpellEffects(Location pos1, Location pos2) {
		playSpellEffects(EffectPosition.CASTER, pos1);
		playSpellEffects(EffectPosition.TARGET, pos2);
		playSpellEffectsTrail(pos1, pos2);
	}

	protected void playSpellEffects(EffectPosition pos, Entity entity) {
		if (effects == null) return;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return;
		for (SpellEffect effect : effectsList) {
			Runnable canceler = effect.playEffect(entity);
			if (canceler == null) continue;
			if (!(entity instanceof Player player)) continue;
			Map<EffectPosition, List<Runnable>> runnablesMap = callbacks.get(player.getUniqueId().toString());
			if (runnablesMap == null) continue;
			List<Runnable> runnables = runnablesMap.get(pos);
			if (runnables == null) continue;
			runnables.add(canceler);
		}
	}

	protected void playSpellEffects(EffectPosition pos, Location location) {
		if (effects == null) return;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return;
		for (SpellEffect effect : effectsList) {
			effect.playEffect(location);
		}
	}

	protected Set<EffectlibSpellEffect> playSpellEffectLibEffects(EffectPosition pos, Location location) {
		if (effects == null) return null;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return null;
		Set<EffectlibSpellEffect> spellEffects = new HashSet<>();
		for (SpellEffect effect : effectsList) {
			if (!(effect instanceof EffectLibEffect)) continue;
			spellEffects.add(new EffectlibSpellEffect(effect.playEffectLib(location), (EffectLibEffect) effect));
		}
		return spellEffects;
	}

	protected Set<Entity> playSpellEntityEffects(EffectPosition pos, Location location) {
		if (effects == null) return null;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return null;
		Set<Entity> entities = new HashSet<>();
		for (SpellEffect effect : effectsList) {
			if (!(effect instanceof EntityEffect)) continue;
			entities.add(effect.playEntityEffect(location));
		}
		return entities;
	}

	protected Set<ArmorStand> playSpellArmorStandEffects(EffectPosition pos, Location location) {
		if (effects == null) return null;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return null;
		Set<ArmorStand> armorStands = new HashSet<>();
		for (SpellEffect effect : effectsList) {
			if (!(effect instanceof ArmorStandEffect)) continue;
			armorStands.add(effect.playArmorStandEffect(location));
		}
		return armorStands;
	}

	protected void playSpellEffectsTrail(Location loc1, Location loc2) {
		if (effects == null) return;
		if (!LocationUtil.isSameWorld(loc1, loc2)) return;
		List<SpellEffect> effectsList = effects.get(EffectPosition.TRAIL);
		if (effectsList != null) {
			for (SpellEffect effect : effectsList) {
				effect.playEffect(loc1, loc2);
			}
		}
		List<SpellEffect> rTrailEffects = effects.get(EffectPosition.REVERSE_LINE);
		if (rTrailEffects != null) {
			for (SpellEffect effect: rTrailEffects) {
				effect.playEffect(loc2, loc1);
			}
		}
	}

	public void playTrackingLinePatterns(EffectPosition pos, Location origin, Location target, Entity originEntity, Entity targetEntity) {
		if (effects == null) return;
		List<SpellEffect> spellEffects = effects.get(pos);
		if (spellEffects == null) return;
		for (SpellEffect e: spellEffects) {
			e.playTrackingLinePatterns(origin, target, originEntity, targetEntity);
		}
	}

	public void initializePlayerEffectTracker(Player p) {
		if (callbacks == null) return;
		String key = p.getUniqueId().toString();
		Map<EffectPosition, List<Runnable>> entry = new EnumMap<>(EffectPosition.class);
		for (EffectPosition pos: EffectPosition.values()) {
			List<Runnable> runnables = new ArrayList<>();
			entry.put(pos, runnables);
		}
		callbacks.put(key, entry);
	}

	public void unloadPlayerEffectTracker(Player p) {
		String uuid = p.getUniqueId().toString();
		for (EffectPosition pos: EffectPosition.values()) {
			cancelEffects(pos, uuid);
		}
		callbacks.remove(uuid);
	}

	public void cancelEffects(EffectPosition pos, String uuid) {
		if (callbacks == null) return;
		if (callbacks.get(uuid) == null) return;
		List<Runnable> cancelers = callbacks.get(uuid).get(pos);
		while (!cancelers.isEmpty()) {
			Runnable c = cancelers.iterator().next();
			if (c instanceof Effect eff) eff.cancel();
			else c.run();
			cancelers.remove(c);
		}
	}

	public void cancelEffectForAllPlayers(EffectPosition pos) {
		for (String key: callbacks.keySet()) {
			cancelEffects(pos, key);
		}
	}

	public Set<EffectTracker> getEffectTrackers() {
		return effectTrackerSet;
	}

	public Set<AsyncEffectTracker> getAsyncEffectTrackers() {
		return asyncEffectTrackerSet;
	}

	protected void playSpellEffectsBuff(Entity entity, SpellEffect.SpellEffectActiveChecker checker) {
		if (effects == null) return;
		List<SpellEffect> effectList = effects.get(EffectPosition.BUFF);
		if (effectList != null) {
			for (SpellEffect effect : effectList) {
				EffectTracker tracker = effect.playEffectWhileActiveOnEntity(entity, checker);
				if (this instanceof BuffSpell) tracker.setBuffSpell((BuffSpell) this);
				effectTrackerSet.add(tracker);
			}
		}

		effectList = effects.get(EffectPosition.ORBIT);
		if (effectList != null) {
			for (SpellEffect effect : effectList) {
				EffectTracker tracker = effect.playEffectWhileActiveOrbit(entity, checker);
				if (this instanceof BuffSpell) tracker.setBuffSpell((BuffSpell) this);
				effectTrackerSet.add(tracker);
			}
		}

		effectList = effects.get(EffectPosition.BUFF_EFFECTLIB);
		if (effectList != null) {
			for (SpellEffect effect : effectList) {
				if (!(effect instanceof EffectLibEffect)) continue;
				AsyncEffectTracker tracker = effect.playEffectlibEffectWhileActiveOnEntity(entity, checker);
				if (this instanceof BuffSpell) tracker.setBuffSpell((BuffSpell) this);
				asyncEffectTrackerSet.add(tracker);
			}
		}

		// only normal effectlib effect is allowed
		effectList = effects.get(EffectPosition.ORBIT_EFFECTLIB);
		if (effectList != null) {
			for (SpellEffect effect : effectList) {
				if (!(effect instanceof EffectLibEffect)) continue;
				if (effect instanceof EffectLibLineEffect) continue;
				if (effect instanceof EffectLibEntityEffect) continue;

				AsyncEffectTracker tracker = effect.playEffectlibEffectWhileActiveOrbit(entity, checker);
				if (this instanceof BuffSpell) tracker.setBuffSpell((BuffSpell) this);
				asyncEffectTrackerSet.add(tracker);
			}
		}
	}

	protected void registerEvents() {
		registerEvents(this);
	}

	protected void registerEvents(Listener listener) {
		MagicSpells.registerEvents(listener);
	}

	protected void unregisterEvents(Listener listener) {
		HandlerList.unregisterAll(listener);
	}

	protected int scheduleDelayedTask(Runnable task, int delay) {
		return MagicSpells.scheduleDelayedTask(task, delay);
	}

	protected int scheduleRepeatingTask(Runnable task, int delay, int interval) {
		return MagicSpells.scheduleRepeatingTask(task, delay, interval);
	}

	protected CastItem[] setupCastItems(String[] items, String errorMessage) {
		CastItem[] castItems = new CastItem[items.length];
		for (int i = 0; i < items.length; i++) {
			MagicItem magicItem = MagicItems.getMagicItemFromString(items[i]);
			if (magicItem == null) {
				MagicSpells.error(errorMessage.replace("%i", items[i]));
				continue;
			}

			ItemStack item = magicItem.getItemStack();
			if (item == null) {
				MagicSpells.error(errorMessage.replace("%i", items[i]));
				continue;
			}
			castItems[i] = new CastItem(item);
		}
		return castItems;
	}

	protected Subspell initSubspell(String subspellName, String errorMessage) {
		if (subspellName.isEmpty()) return null;
		Subspell subspell = new Subspell(subspellName);
		if (!subspell.process()) {
			MagicSpells.error(errorMessage);
			subspell = null;
		}
		return subspell;
	}

	/**
	 * Formats a string by performing the specified replacements.
	 * @param message the string to format
	 * @param replacements the replacements to make, in pairs.
	 * @return the formatted string
	 */
	protected String formatMessage(String message, String... replacements) {
		return MagicSpells.formatMessage(message, replacements);
	}

	/**
	 * Sends a message to a player, first making the specified replacements. This method also does color replacement and has multi-line functionality.
	 * @param livingEntity the living entity to send the message to
	 * @param message the message to send
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(LivingEntity livingEntity, String message, String... replacements) {
		sendMessage(message, livingEntity, null, replacements);
	}

	protected void sendMessage(LivingEntity livingEntity, String message) {
		sendMessage(message, livingEntity, null);
	}

	/**
	 * Sends a message to a player. This method also does color replacement and has multi-line functionality.
	 * @param livingEntity the living entity to send the message to
	 * @param message the message to send
	 */
	protected void sendMessage(String message, LivingEntity livingEntity, String[] args) {
		MagicSpells.sendMessage(message, livingEntity, args);
	}

	protected void sendMessage(String message, LivingEntity livingEntity) {
		MagicSpells.sendMessage(message, livingEntity, null);
	}

	/**
	 * Sends a message to a player, first making the specified replacements.This method also does color replacement and has multi-line functionality.
	 * @param message the message to send
	 * @param livingEntity the player to send the message to
	 * @param args the arguments of associated spell cast
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(String message, LivingEntity livingEntity, String[] args, String... replacements) {
		MagicSpells.sendMessageAndFormat(message, livingEntity, args, replacements);
	}

	/**
	 * Sends a message to all players near the specified player, within the configured broadcast range.
	 * @param livingEntity the "center" living entity used to find nearby players
	 * @param message the message to send
	 */
	protected void sendMessageNear(LivingEntity livingEntity, String message) {
		sendMessageNear(livingEntity, null, message, broadcastRange, MagicSpells.NULL_ARGS);
	}

	/**
	 * Sends a message to all players near the specified player, within the specified broadcast range.
	 * @param livingEntity the "center" living entity used to find nearby players
	 * @param message the message to send
	 * @param range the broadcast range
	 */
	protected void sendMessageNear(LivingEntity livingEntity, Player ignore, String message, int range, String[] args) {
		sendMessageNear(livingEntity, ignore, message, range, args, (String[]) null);
	}

	/**
	 * Sends a message to all players near the specified player, within the specified broadcast range.
	 * @param livingEntity the "center" living entity used to find nearby players
	 * @param ignore player to ignore when sending messages
	 * @param message the message to send
	 * @param range the broadcast range
	 * @param args cast arguments
	 * @param replacements replacements to be done on message
	 */
	protected void sendMessageNear(LivingEntity livingEntity, Player ignore, String message, int range, String[] args, String... replacements) {
		if (message == null) return;
		if (message.isEmpty()) return;
		if (Perm.SILENT.has(livingEntity)) return;

		int rangeDoubled = range << 1;
		Collection<Player> players = livingEntity.getLocation().getNearbyPlayers(rangeDoubled);
		for (Player player : players) {
			if (player == livingEntity) continue;
			if (player == ignore) continue;

			sendMessage(message, player, args, replacements);
		}
	}

	public String getInternalName() {
		return internalName;
	}

	public String getName() {
		if (name != null && !name.isEmpty()) return name;
		return internalName;
	}

	public String getPermissionName() {
		return permName;
	}

	public boolean isHelperSpell() {
		return helperSpell;
	}

	public String getCantBindError() {
		return strCantBind;
	}

	public String[] getAliases() {
		return aliases;
	}

	public List<String> getIncantations() {
		return incantations;
	}

	public CastItem getCastItem() {
		if (castItems.length == 1) return castItems[0];
		return null;
	}

	public CastItem[] getCastItems() {
		return castItems;
	}

	public CastItem[] getLeftClickCastItems() {
		return leftClickCastItems;
	}

	public CastItem[] getRightClickCastItems() {
		return rightClickCastItems;
	}

	public CastItem[] getConsumeCastItems() {
		return consumeCastItems;
	}

	public String getDanceCastSequence() {
		return danceCastSequence;
	}

	public String getDescription() {
		return description;
	}

	public SpellReagents getReagents() {
		return reagents;
	}

	public String getConsoleName() {
		return MagicSpells.getConsoleName();
	}

	public String getStrWrongCastItem() {
		return strWrongCastItem;
	}

	public List<String> getPrecludes() {
		return precludes;
	}

	public List<String> getReplaces() {
		return replaces;
	}

	public List<String> getPrerequisites() {
		return prerequisites;
	}

	public final boolean isBeneficial() {
		return beneficial;
	}

	public boolean isBeneficialDefault() {
		return false;
	}

	public ModifierSet getModifiers() {
		return modifiers;
	}

	public ModifierSet getTargetModifiers() {
		return targetModifiers;
	}

	public ModifierSet getLocationModifiers() {
		return locationModifiers;
	}

	public String getStrModifierFailed() {
		return strModifierFailed;
	}

	public Map<String, Integer> getXpGranted() {
		return xpGranted;
	}

	public Map<String, Integer> getXpRequired() {
		return xpRequired;
	}

	public String getStrXpLearned() {
		return strXpAutoLearned;
	}

	public Map<UUID, Long> getCooldowns() {
		return nextCast;
	}

	public Multimap<String, VariableMod> getVariableModsCast() {
		return variableModsCast;
	}

	public Multimap<String, VariableMod> getVariableModsCasted() {
		return variableModsCasted;
	}

	public Multimap<String, VariableMod> getVariableModsTarget() {
		return variableModsTarget;
	}

	public ValidTargetList getValidTargetList() {
		return validTargetList;
	}

	public void setValidTargetList(ValidTargetList validTargetList) {
		this.validTargetList = validTargetList;
	}

	void setCooldownManually(UUID uuid, long nextCast) {
		this.nextCast.put(uuid, nextCast);
	}

	protected void debug(int level, String message) {
		if (debug) MagicSpells.debug(level, message);
	}

	/**
	 * This method is called when the plugin is being disabled, for any reason.
	 */
	protected void turnOff() {
		// No op
	}

	@Override
	public int compareTo(Spell spell) {
		return name.compareTo(spell.name);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Spell && ((Spell) o).internalName.equals(internalName);
	}

	@Override
	public int hashCode() {
		return internalName.hashCode();
	}

	// TODO move this to its own class
	public enum SpellCastState {

		NORMAL,
		ON_COOLDOWN,
		MISSING_REAGENTS,
		CANT_CAST,
		NO_MAGIC_ZONE,
		WRONG_WORLD

	}

	// TODO move this to its own class
	public enum PostCastAction {

		HANDLE_NORMALLY(true, true, true),
		ALREADY_HANDLED(false, false, false),
		NO_MESSAGES(true, true, false),
		NO_REAGENTS(true, false, true),
		NO_COOLDOWN(false, true, true),
		MESSAGES_ONLY(false, false, true),
		REAGENTS_ONLY(false, true, false),
		COOLDOWN_ONLY(true, false, false),
		DELAYED(false, false, false);

		private boolean cooldown;
		private boolean reagents;
		private boolean messages;

		PostCastAction(boolean cooldown, boolean reagents, boolean messages) {
			this.cooldown = cooldown;
			this.reagents = reagents;
			this.messages = messages;
		}

		public boolean setCooldown() {
			return cooldown;
		}

		public boolean chargeReagents() {
			return reagents;
		}

		public boolean sendMessages() {
			return messages;
		}

	}

	public static class SpellCastResult {

		public SpellCastState state;
		public PostCastAction action;

		public SpellCastResult(SpellCastState state, PostCastAction action) {
			this.state = state;
			this.action = action;
		}

	}

	public class DelayedSpellCast implements Runnable, Listener {

		private static final double motionTolerance = 0.2;

		private final SpellCastEvent spellCast;
		private final LivingEntity caster;
		private final Location from;
		private final int taskId;

		public DelayedSpellCast(SpellCastEvent spellCast) {
			this.spellCast = spellCast;

			taskId = scheduleDelayedTask(this, spellCast.getCastTime());
			caster = spellCast.getCaster();
			from = caster.getLocation();

			registerEvents(this);
		}

		@Override
		public void run() {
			if (caster.isValid() && !caster.isDead()) {
				if (!interruptOnMove || inBounds(caster.getLocation())) {
					unregisterEvents(this);

					spellCast.setSpellCastState(getCastState(caster));
					spellCast.getSpell().handleCast(spellCast);
				} else interrupt();

				return;
			}

			unregisterEvents(this);
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onMove(PlayerMoveEvent event) {
			if (!interruptOnMove) return;
			if (!event.getPlayer().equals(caster)) return;
			if (inBounds(event.getTo())) return;

			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onDamage(EntityDamageEvent event) {
			if (!interruptOnDamage) return;
			if (!event.getEntity().equals(caster)) return;

			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellCast(SpellCastEvent event) {
			if (!interruptOnCast) return;
			if (event.getSpell() instanceof PassiveSpell) return;
			if (!event.getCaster().equals(caster)) return;

			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onTeleport(PlayerTeleportEvent event) {
			if (!interruptOnTeleport) return;
			if (!event.getPlayer().equals(caster)) return;

			interrupt();
		}

		private boolean inBounds(Location to) {
			return Math.abs(from.getX() - to.getX()) < motionTolerance
				&& Math.abs(from.getY() - to.getY()) < motionTolerance
				&& Math.abs(from.getZ() - to.getZ()) < motionTolerance;
		}

		private void interrupt() {
			MagicSpells.cancelTask(taskId);
			unregisterEvents(this);

			sendMessage(strInterrupted, caster, null);
			if (spellOnInterrupt != null) {
				if (spellOnInterrupt.isTargetedLocationSpell()) spellOnInterrupt.castAtLocation(caster, caster.getLocation(), spellCast.getPower());
				else spellOnInterrupt.cast(caster, spellCast.getPower());
			}
		}

	}

	public class DelayedSpellCastWithBar implements Runnable, Listener {

		private static final double motionTolerance = 0.2;
		private static final int interval = 5;

		private final SpellCastEvent spellCast;
		private final LivingEntity caster;
		private final Location from;
		private final int castTime;
		private final int taskId;

		private int elapsed = 0;

		public DelayedSpellCastWithBar(SpellCastEvent spellCast) {
			this.spellCast = spellCast;

			castTime = spellCast.getCastTime();
			caster = spellCast.getCaster();
			from = caster.getLocation();

			if (caster instanceof Player) MagicSpells.getExpBarManager().lock((Player) caster, this);
			taskId = scheduleRepeatingTask(this, interval, interval);
			registerEvents(this);
		}

		@Override
		public void run() {
			if (caster.isValid() && !caster.isDead()) {
				elapsed += interval;

				if (!interruptOnMove || inBounds(caster.getLocation())) {
					if (elapsed >= castTime) {
						end();

						spellCast.setSpellCastState(getCastState(caster));
						spellCast.getSpell().handleCast(spellCast);
					}

					if (caster instanceof Player) MagicSpells.getExpBarManager().update((Player) caster, 0, (float) elapsed / (float) castTime, this);
				} else interrupt();
			} else end();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onMove(PlayerMoveEvent event) {
			if (!interruptOnMove) return;
			if (!event.getPlayer().equals(caster)) return;
			if (inBounds(event.getTo())) return;

			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onDamage(EntityDamageEvent event) {
			if (!interruptOnDamage) return;
			if (!event.getEntity().equals(caster)) return;

			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellCast(SpellCastEvent event) {
			if (!interruptOnCast) return;
			if (event.getSpell() instanceof PassiveSpell) return;
			if (!caster.equals(event.getCaster())) return;

			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onTeleport(PlayerTeleportEvent event) {
			if (!interruptOnTeleport) return;
			if (!event.getPlayer().equals(caster)) return;

			interrupt();
		}

		private boolean inBounds(Location to) {
			return Math.abs(from.getX() - to.getX()) < motionTolerance
				&& Math.abs(from.getY() - to.getY()) < motionTolerance
				&& Math.abs(from.getZ() - to.getZ()) < motionTolerance;
		}

		private void interrupt() {
			sendMessage(strInterrupted, caster, null);
			end();
			if (spellOnInterrupt != null) {
				if (spellOnInterrupt.isTargetedLocationSpell()) spellOnInterrupt.castAtLocation(caster, caster.getLocation(), spellCast.getPower());
				else spellOnInterrupt.cast(caster, spellCast.getPower());
			}
		}

		private void end() {
			MagicSpells.cancelTask(taskId);
			unregisterEvents(this);

			if (caster instanceof Player) {
				MagicSpells.getExpBarManager().unlock((Player) caster, this);
				MagicSpells.getExpBarManager().update((Player) caster, ((Player) caster).getLevel(), ((Player) caster).getExp());
				ManaHandler mana = MagicSpells.getManaHandler();
				if (mana != null) mana.showMana((Player) caster);
			}
		}

	}

	public ValidTargetChecker getValidTargetChecker() {
		return null;
	}

}
