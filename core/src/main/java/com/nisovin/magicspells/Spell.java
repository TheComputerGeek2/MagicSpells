package com.nisovin.magicspells;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import com.nisovin.magicspells.util.*;
import de.slikey.effectlib.Effect;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.variables.VariableManager;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public abstract class Spell implements Comparable<Spell>, Listener {

	protected MagicConfig config;

	protected Map<String, Long> nextCast;
	protected Map<String, Integer> xpGranted;
	protected Map<String, Integer> xpRequired;
	protected Map<Spell, Float> sharedCooldowns;
	protected Map<String, VariableMod> variableModsCast;
	protected Map<String, VariableMod> variableModsCasted;
	protected Map<String, VariableMod> variableModsTarget;
	protected Map<String, Map<EffectPosition, List<Runnable>>> callbacks;

	protected IntMap<String> chargesConsumed;

	protected EnumMap<EffectPosition, List<SpellEffect>> effects;

	protected Set<String> tags;
	protected Set<CastItem> bindableItems;
	protected Set<Material> losTransparentBlocks;

	protected List<String> replaces;
	protected List<String> precludes;
	protected List<String> incantations;
	protected List<String> prerequisites;
	protected List<String> modifierStrings;
	protected List<String> worldRestrictions;
	protected List<String> rawSharedCooldowns;
	protected List<String> targetModifierStrings;
	protected List<String> locationModifierStrings;

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
	protected boolean interruptOnTeleport;
	protected boolean ignoreGlobalCooldown;
	protected boolean spellPowerAffectsRange;
	protected boolean requireCastItemOnCommand;

	protected CastItem[] castItems;
	protected CastItem[] consumeCastItems;
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

	protected Spell spellOnInterrupt;

	protected SpellReagents reagents;

	protected ItemStack spellIcon;

	protected DamageCause targetDamageCause;

	protected ValidTargetList validTargetList;

	protected long nextCastServer;

	protected double targetDamageAmount;

	protected int range;
	protected int charges;
	protected int minRange;
	protected int castTime;
	protected int experience;
	protected int broadcastRange;

	protected float cooldown;
	protected float serverCooldown;

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
			String[] sItems = config.getString(path + "cast-item", "-5").trim().replace(" ", "").split(",");
			castItems = new CastItem[sItems.length];
			for (int i = 0; i < sItems.length; i++) {
				ItemStack is = Util.getItemStackFromString(sItems[i]);
				if (is == null) continue;
				castItems[i] = new CastItem(is);
			}
		} else if (config.contains(path + "cast-items")) {
			List<String> sItems = config.getStringList(path + "cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			castItems = new CastItem[sItems.size()];
			for (int i = 0; i < castItems.length; i++) {
				ItemStack is = Util.getItemStackFromString(sItems.get(i));
				if (is == null) continue;
				castItems[i] = new CastItem(is);
			}
		} else {
			castItems = new CastItem[0];
		}
		if (config.contains(path + "right-click-cast-item")) {
			String[] sItems = config.getString(path + "right-click-cast-item", "-5").trim().replace(" ", "").split(",");
			rightClickCastItems = new CastItem[sItems.length];
			for (int i = 0; i < sItems.length; i++) {
				ItemStack is = Util.getItemStackFromString(sItems[i]);
				if (is == null) continue;
				rightClickCastItems[i] = new CastItem(is);
			}
		} else if (config.contains(path + "right-click-cast-items")) {
			List<String> sItems = config.getStringList(path + "right-click-cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			rightClickCastItems = new CastItem[sItems.size()];
			for (int i = 0; i < rightClickCastItems.length; i++) {
				ItemStack is = Util.getItemStackFromString(sItems.get(i));
				if (is == null) continue;
				rightClickCastItems[i] = new CastItem(is);
			}
		} else {
			rightClickCastItems = new CastItem[0];
		}
		if (config.contains(path + "consume-cast-item")) {
			String[] sItems = config.getString(path + "consume-cast-item", "-5").trim().replace(" ", "").split(",");
			consumeCastItems = new CastItem[sItems.length];
			for (int i = 0; i < sItems.length; i++) {
				ItemStack is = Util.getItemStackFromString(sItems[i]);
				if (is == null) continue;
				consumeCastItems[i] = new CastItem(is);
			}
		} else if (config.contains(path + "consume-cast-items")) {
			List<String> sItems = config.getStringList(path + "consume-cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			consumeCastItems = new CastItem[sItems.size()];
			for (int i = 0; i < consumeCastItems.length; i++) {
				ItemStack is = Util.getItemStackFromString(sItems.get(i));
				if (is == null) continue;
				consumeCastItems[i] = new CastItem(is);
			}
		} else {
			consumeCastItems = new CastItem[0];
		}
		castWithLeftClick = config.getBoolean(path + "cast-with-left-click", MagicSpells.plugin.castWithLeftClick);
		castWithRightClick = config.getBoolean(path + "cast-with-right-click", MagicSpells.plugin.castWithRightClick);
		danceCastSequence = config.getString(path + "dance-cast-sequence", null);
		requireCastItemOnCommand = config.getBoolean(path + "require-cast-item-on-command", false);
		bindable = config.getBoolean(path + "bindable", true);
		List<String> bindables = config.getStringList(path + "bindable-items", null);
		if (bindables != null) {
			bindableItems = new HashSet<>();
			for (String s : bindables) {
				ItemStack is = Util.getItemStackFromString(s);
				if (is == null) continue;
				bindableItems.add(new CastItem(is));
			}
		}
		String icontemp = config.getString(path + "spell-icon", null);
		if (icontemp == null) {
			spellIcon = null;
		} else {
			spellIcon = Util.getItemStackFromString(icontemp);
			if (spellIcon != null && spellIcon.getType() != Material.AIR) {
				spellIcon.setAmount(0);
				if (!icontemp.contains("|")) {
					ItemMeta iconMeta = spellIcon.getItemMeta();
					iconMeta.setDisplayName(MagicSpells.getTextColor() + name);
					spellIcon.setItemMeta(iconMeta);
				}
			}
		}
		experience = config.getInt(path + "experience", 0);
		broadcastRange = config.getInt(path + "broadcast-range", MagicSpells.plugin.broadcastRange);

		// Cast time
		castTime = config.getInt(path + "cast-time", 0);
		interruptOnMove = config.getBoolean(path + "interrupt-on-move", true);
		interruptOnCast = config.getBoolean(path + "interrupt-on-cast", true);
		interruptOnDamage = config.getBoolean(path + "interrupt-on-damage", false);
		interruptOnTeleport = config.getBoolean(path + "interrupt-on-teleport", true);
		spellNameOnInterrupt = config.getString(path + "spell-on-interrupt", null);

		// Targeting
		minRange = config.getInt(path + "min-range", 0);
		range = config.getInt(path + "range", 20);
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

		// Graphical effects
		if (config.contains(path + "effects")) {
			effects = new EnumMap<>(EffectPosition.class);
			if (config.isList(path + "effects")) {
				List<String> effectsList = config.getStringList(path + "effects", null);
				if (effectsList != null) {
					for (String eff : effectsList) {
						String[] data = eff.split(" ", 3);
						EffectPosition pos = EffectPosition.getPositionFromString(data[0]);
						if (pos == null) continue;
						SpellEffect effect = SpellEffect.createNewEffectByName(data[1]);
						if (effect == null) continue;
						effect.loadFromString(data.length > 2 ? data[2] : null);
						List<SpellEffect> e = effects.computeIfAbsent(pos, p -> new ArrayList<>());
						e.add(effect);
					}
				}
			} else if (config.isSection(path + "effects")) {
				for (String key : config.getKeys(path + "effects")) {
					ConfigurationSection effConf = config.getSection(path + "effects." + key);
					EffectPosition pos = EffectPosition.getPositionFromString(effConf.getString("position", ""));
					if (pos == null) continue;
					SpellEffect effect = SpellEffect.createNewEffectByName(effConf.getString("effect", ""));
					if (effect == null) continue;
					effect.loadFromConfiguration(effConf);
					List<SpellEffect> e = effects.computeIfAbsent(pos, p -> new ArrayList<>());
					e.add(effect);
				}
			}
		}

		//TODO load the fast mapping for effects here

		// Cost
		reagents = getConfigReagents("cost");
		if (reagents == null) reagents = new SpellReagents();

		// Cooldowns
		cooldown = (float) config.getDouble(path + "cooldown", 0);
		serverCooldown = (float) config.getDouble(path + "server-cooldown", 0);
		rawSharedCooldowns = config.getStringList(path + "shared-cooldowns", null);
		ignoreGlobalCooldown = config.getBoolean(path + "ignore-global-cooldown", false);
		charges = config.getInt(path + "charges", 0);
		rechargeSound = config.getString(path + "recharge-sound", "");
		nextCast = new HashMap<>();
		chargesConsumed = new IntMap<>();
		nextCastServer = 0;

		// Modifiers
		modifierStrings = config.getStringList(path + "modifiers", null);
		targetModifierStrings = config.getStringList(path + "target-modifiers", null);
		locationModifierStrings = config.getStringList(path + "location-modifiers", null);

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

		// Variable options
		List<String> varModsCast = config.getStringList(path + "variable-mods-cast", null);
		if (varModsCast != null && !varModsCast.isEmpty()) {
			variableModsCast = new HashMap<>();
			for (String s : varModsCast) {
				try {
					String[] data = s.split(" ");
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					variableModsCast.put(var, varMod);
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-cast option for spell '" + spellName + "': " + s);
				}
			}
		}
		List<String> varModsCasted = config.getStringList(path + "variable-mods-casted", null);
		if (varModsCasted != null && !varModsCasted.isEmpty()) {
			variableModsCasted = new HashMap<>();
			for (String s : varModsCasted) {
				try {
					String[] data = s.split(" ");
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					variableModsCasted.put(var, varMod);
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-casted option for spell '" + spellName + "': " + s);
				}
			}
		}
		List<String> varModsTarget = config.getStringList(path + "variable-mods-target", null);
		if (varModsTarget != null && !varModsTarget.isEmpty()) {
			variableModsTarget = new HashMap<>();
			for (String s : varModsTarget) {
				try {
					String[] data = s.split(" ");
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					variableModsTarget.put(var, varMod);
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-target option for spell '" + spellName + "': " + s);
				}
			}
		}

		soundOnCooldown = config.getString(path + "sound-on-cooldown", MagicSpells.plugin.soundFailOnCooldown);
		soundMissingReagents = config.getString(path + "sound-missing-reagents", MagicSpells.plugin.soundFailMissingReagents);
		if (soundOnCooldown != null && soundOnCooldown.isEmpty()) soundOnCooldown = null;
		if (soundMissingReagents != null && soundMissingReagents.isEmpty()) soundMissingReagents = null;

		// Strings
		strCost = config.getString(path + "str-cost", null);
		strCantCast = config.getString(path + "str-cant-cast", MagicSpells.plugin.strCantCast);
		strCantBind = config.getString(path + "str-cant-bind", null);
		strCastSelf = config.getString(path + "str-cast-self", null);
		strCastStart = config.getString(path + "str-cast-start", null);
		strCastOthers = config.getString(path + "str-cast-others", null);
		strOnCooldown = config.getString(path + "str-on-cooldown", MagicSpells.plugin.strOnCooldown);
		strWrongWorld = config.getString(path + "str-wrong-world", MagicSpells.plugin.strWrongWorld);
		strInterrupted = config.getString(path + "str-interrupted", null);
		strXpAutoLearned = config.getString(path + "str-xp-auto-learned", MagicSpells.plugin.strXpAutoLearned);
		strWrongCastItem = config.getString(path + "str-wrong-cast-item", strCantCast);
		strModifierFailed = config.getString(path + "str-modifier-failed", null);
		strMissingReagents = config.getString(path + "str-missing-reagents", MagicSpells.plugin.strMissingReagents);
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
		SpellReagents reagents = null;
		List<String> costList = config.getStringList("spells." + internalName + '.' + option, null);
		if (costList != null && !costList.isEmpty()) {
			reagents = new SpellReagents();
			String[] data;
			for (String costVal : costList) {
				try {
					// Parse cost data
					data = costVal.split(" ");
					int amt = 1;
					float money = 1;
					if (data[0].equalsIgnoreCase("health")) {
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setHealth(amt);
					} else if (data[0].equalsIgnoreCase("mana")) {
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setMana(amt);
					} else if (data[0].equalsIgnoreCase("hunger")) {
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setHunger(amt);
					} else if (data[0].equalsIgnoreCase("experience")) {
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setExperience(amt);
					} else if (data[0].equalsIgnoreCase("levels")) {
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setLevels(amt);
					} else if (data[0].equalsIgnoreCase("durability")) {
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setDurability(amt);
					} else if (data[0].equalsIgnoreCase("money")) {
						if (data.length > 1) money = Float.parseFloat(data[1]);
						reagents.setMoney(money);
					} else if (data[0].equalsIgnoreCase("copper")) {
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						reagents.setCopper(amt);
					} else if (data[0].equalsIgnoreCase("variable")) {
						reagents.addVariable(data[1], Double.parseDouble(data[2]));
					} else {
						if (data.length > 1) amt = Integer.parseInt(data[1]);
						ItemStack is = Util.getItemStackFromString(data[0]);
						if (is != null) {
							is.setAmount(amt);
							reagents.addItem(is);
						} else {
							MagicSpells.error("Failed to process cost value for " + internalName + " spell: " + costVal);
						}
					}
				} catch (Exception e) {
					// FIXME this should not be a means of breaking
					MagicSpells.error("Failed to process cost value for " + internalName + " spell: " + costVal);
				}
			}
		}
		return reagents;
	}

	// DEBUG INFO: level 2, adding modifiers to internalname
	// DEBUG INFO: level 2, adding target modifiers to internalname
	/**
	 * This method is called immediately after all spells have been loaded.
	 */
	protected void initialize() {
		// Modifiers
		if (modifierStrings != null && !modifierStrings.isEmpty()) {
			debug(2, "Adding modifiers to " + internalName + " spell");
			modifiers = new ModifierSet(modifierStrings);
			modifierStrings = null;
		}
		if (targetModifierStrings != null && !targetModifierStrings.isEmpty()) {
			debug(2, "Adding target modifiers to " + internalName + " spell");
			targetModifiers = new ModifierSet(targetModifierStrings);
			targetModifierStrings = null;
		}
		if (locationModifierStrings != null && !locationModifierStrings.isEmpty()) {
			debug(2, "Adding location modifiers to " + internalName + " spell");
			locationModifiers = new ModifierSet(locationModifierStrings);
			locationModifierStrings = null;
		}

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
		if (spellNameOnInterrupt != null && !spellNameOnInterrupt.isEmpty()) spellOnInterrupt = MagicSpells.getSpellByInternalName(spellNameOnInterrupt);
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

	protected boolean isConfigString(String key) {
		return config.isString("spells." + internalName + '.' + key);
	}

	protected boolean isConfigSection(String key) {
		return config.isSection("spells." + internalName + '.' + key);
	}

	public final SpellCastResult cast(Player player) {
		return cast(player, 1.0F, null);
	}

	// TODO can this safely be made varargs?
	public final SpellCastResult cast(Player player, String[] args) {
		return cast(player, 1.0F, args);
	}

	// TODO can this safely be made varargs?
	public final SpellCastResult cast(Player player, float power, String[] args) {
		SpellCastEvent spellCast = preCast(player, power, args);
		if (spellCast == null) return new SpellCastResult(SpellCastState.CANT_CAST, PostCastAction.HANDLE_NORMALLY);
		PostCastAction action;
		int castTime = spellCast.getCastTime();
		if (castTime <= 0 || spellCast.getSpellCastState() != SpellCastState.NORMAL) action = handleCast(spellCast);
		else if (!preCastTimeCheck(player, args)) action = PostCastAction.ALREADY_HANDLED;
		else {
			action = PostCastAction.DELAYED;
			sendMessage(strCastStart, player, args);
			playSpellEffects(EffectPosition.START_CAST, player);
			if (MagicSpells.plugin.useExpBarAsCastTimeBar) new DelayedSpellCastWithBar(spellCast);
			else new DelayedSpellCast(spellCast);
		}
		return new SpellCastResult(spellCast.getSpellCastState(), action);
	}

	protected SpellCastState getCastState(Player player) {
		if (!MagicSpells.getSpellbook(player).canCast(this)) return SpellCastState.CANT_CAST;
		if (worldRestrictions != null && !worldRestrictions.contains(player.getWorld().getName())) return SpellCastState.WRONG_WORLD;
		if (MagicSpells.getNoMagicZoneManager() != null && MagicSpells.getNoMagicZoneManager().willFizzle(player, this)) return SpellCastState.NO_MAGIC_ZONE;
		if (onCooldown(player)) return SpellCastState.ON_COOLDOWN;
		if (!hasReagents(player)) return SpellCastState.MISSING_REAGENTS;
		return SpellCastState.NORMAL;
	}

	// TODO can this safely be made varargs?
	// DEBUG INFO: level 2, spell cast state
	// DEBUG INFO: level 2, spell canceled
	// DEBUG INFO: level 2, spell cast state changed
	protected SpellCastEvent preCast(Player player, float power, String[] args) {
		// Get spell state
		SpellCastState state = getCastState(player);
		debug(2, "    Spell cast state: " + state);

		// Call events
		SpellCastEvent event = new SpellCastEvent(this, player, state, power, args, cooldown, reagents.clone(), castTime);
		EventUtil.call(event);
		if (event.isCancelled()) {
			debug(2, "    Spell canceled");
			return null;
		}

		if (event.haveReagentsChanged()) {
			boolean hasReagents = hasReagents(player, event.getReagents());
			if (!hasReagents && state != SpellCastState.MISSING_REAGENTS) {
				event.setSpellCastState(SpellCastState.MISSING_REAGENTS);
				debug(2, "    Spell cast state changed: " + state);
			} else if (hasReagents && state == SpellCastState.MISSING_REAGENTS) {
				event.setSpellCastState(state = SpellCastState.NORMAL);
				debug(2, "    Spell cast state changed: " + state);
			}
		}

		if (event.hasSpellCastStateChanged()) debug(2, "    Spell cast state changed: " + state);
		if (Perm.NOCASTTIME.has(player)) event.setCastTime(0);
		return event;
	}

	// DEBUG INFO: level 3, power #
	// DEBUG INFO: level 3, cooldown #
	// DEBUG INFO: level 3, args argsvalue
	PostCastAction handleCast(SpellCastEvent spellCast) {
		long start = System.nanoTime();
		Player player = spellCast.getCaster();
		SpellCastState state = spellCast.getSpellCastState();
		String[] args = spellCast.getSpellArgs();
		float power = spellCast.getPower();
		debug(3, "    Power: " + power);
		debug(3, "    Cooldown: " + cooldown);
		if (MagicSpells.plugin.debug && args != null && args.length > 0) debug(3, "    Args: {" + Util.arrayJoin(args, ',') + '}');
		PostCastAction action = castSpell(player, state, power, args);
		if (MagicSpells.plugin.enableProfiling) {
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
		Player player = spellCast.getCaster();
		SpellCastState state = spellCast.getSpellCastState();
		if (action != null && action != PostCastAction.ALREADY_HANDLED) {
			if (state == SpellCastState.NORMAL) {
				if (action.setCooldown()) setCooldown(player, spellCast.getCooldown());
				if (action.chargeReagents()) removeReagents(player, spellCast.getReagents());
				if (action.sendMessages()) sendMessages(player, spellCast.getSpellArgs());
				if (experience > 0) player.giveExp(experience);
			} else if (state == SpellCastState.ON_COOLDOWN) {
				MagicSpells.sendMessage(formatMessage(strOnCooldown, "%c", Math.round(getCooldown(player)) + ""), player, spellCast.getSpellArgs());
				if (soundOnCooldown != null) MagicSpells.getVolatileCodeHandler().playSound(player, soundOnCooldown, 1F, 1F);
			} else if (state == SpellCastState.MISSING_REAGENTS) {
				MagicSpells.sendMessage(strMissingReagents, player, spellCast.getSpellArgs());
				if (MagicSpells.plugin.showStrCostOnMissingReagents && strCost != null && !strCost.isEmpty()) MagicSpells.sendMessage("    (" + strCost + ')', player, spellCast.getSpellArgs());
				if (soundMissingReagents != null) MagicSpells.getVolatileCodeHandler().playSound(player, soundMissingReagents, 1F, 1F);
			} else if (state == SpellCastState.CANT_CAST) {
				MagicSpells.sendMessage(strCantCast, player, spellCast.getSpellArgs());
			} else if (state == SpellCastState.NO_MAGIC_ZONE) {
				MagicSpells.getNoMagicZoneManager().sendNoMagicMessage(player, this);
			} else if (state == SpellCastState.WRONG_WORLD) {
				MagicSpells.sendMessage(strWrongWorld, player, spellCast.getSpellArgs());
			}
		}
		SpellCastedEvent event = new SpellCastedEvent(this, player, state, spellCast.getPower(), spellCast.getSpellArgs(), cooldown, reagents, action);
		EventUtil.call(event);
	}

	// TODO can this safely be made varargs?
	public void sendMessages(Player player, String[] args) {
		sendMessage(formatMessage(strCastSelf, "%a", player.getName()), player, args);
		sendMessageNear(player, formatMessage(strCastOthers, "%a", player.getName()));
	}

	// TODO can this safely be made varargs?
	protected boolean preCastTimeCheck(Player player, String[] args) {
		return true;
	}

	// TODO can this safely be made varargs?
	/**
	 * This method is called when a player casts a spell, either by command, with a wand item, or otherwise.
	 * @param player the player casting the spell
	 * @param state the state of the spell cast (normal, on cooldown, missing reagents, etc)
	 * @param power the power multiplier the spell should be cast with (1.0 is normal)
	 * @param args the spell arguments, if cast by command
	 * @return the action to take after the spell is processed
	 */
	public abstract PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args);

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
			if (sender.isOp() || !(sender instanceof Player) || ((Player) sender).canSee(p)) matches.add(name);
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

	public boolean isAlwaysGranted() {
		return alwaysGranted;
	}

	public boolean isValidItemForCastCommand(ItemStack item) {
		if (!requireCastItemOnCommand || castItems == null) return true;
		if (item == null && castItems.length == 1 && BlockUtils.isAir(castItems[0].getItemType())) return true;
		for (CastItem castItem : castItems) {
			if (castItem.equals(item)) return true;
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
	 * @param player The player to check
	 * @return whether the spell is on cooldown
	 */
	public boolean onCooldown(Player player) {
		if (Perm.NOCOOLDOWN.has(player)) return false;
		if (charges > 0) return chargesConsumed.get(player.getName()) >= charges;
		if (serverCooldown > 0 && nextCastServer > System.currentTimeMillis()) return true;

		Long next = nextCast.get(player.getName());
		if (next != null && next > System.currentTimeMillis()) return true;
		return false;
	}

	public float getCooldown() {
		return cooldown;
	}

	/**
	 * Get how many seconds remain on the cooldown of this spell for the specified player
	 * @param player The player to check
	 * @return The number of seconds remaining in the cooldown
	 */
	public float getCooldown(Player player) {
		if (charges > 0) return -1;

		float cd = 0;

		Long next = nextCast.get(player.getName());
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
	 * @param player The player to set the cooldown for
	 */
	public void setCooldown(Player player, float cooldown) {
		setCooldown(player, cooldown, true);
	}

	/**
	 * Begins the cooldown for the spell for the specified player
	 * @param player The player to set the cooldown for
	 */
	public void setCooldown(final Player player, float cooldown, boolean activateSharedCooldowns) {
		if (cooldown > 0) {
			if (charges <= 0) {
				nextCast.put(player.getName(), System.currentTimeMillis() + (long) (cooldown * TimeUtil.MILLISECONDS_PER_SECOND));
			} else {
				final String name = player.getName();
				chargesConsumed.increment(name);
				MagicSpells.scheduleDelayedTask(() -> {
					chargesConsumed.decrement(name);
					if (rechargeSound == null) return;
					if (rechargeSound.isEmpty()) return;
					MagicSpells.getVolatileCodeHandler().playSound(player, rechargeSound, 1.0F, 1.0F);
				}, Math.round(TimeUtil.TICKS_PER_SECOND * cooldown));
			}
		} else {
			if (charges <= 0) nextCast.remove(player.getName());
			else chargesConsumed.remove(player.getName());
		}
		if (serverCooldown > 0) nextCastServer = System.currentTimeMillis() + (long) (serverCooldown * TimeUtil.MILLISECONDS_PER_SECOND);
		if (activateSharedCooldowns && sharedCooldowns != null) {
			for (Map.Entry<Spell, Float> scd : sharedCooldowns.entrySet()) {
				scd.getKey().setCooldown(player, scd.getValue(), false);
			}
		}
	}

	/**
	 * Checks if a player has the reagents required to cast this spell
	 * @param player the player to check
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(Player player) {
		return hasReagents(player, reagents);
	}

	// FIXME this doesn't seem strictly tied to Spell logic, could probably be moved
	/**
	 * Checks if a player has the reagents required to cast this spell
	 * @param player the player to check
	 * @param reagents the reagents to check for
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(Player player, SpellReagents reagents) {
		if (reagents == null) return true;
		return hasReagents(player, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels(), reagents.getDurability(), reagents.getMoney(), reagents.getCopper(), reagents.getVariables());
	}

	/**
	 * Checks if a player has the specified reagents, including health and mana
	 * @param player the player to check
	 * @param reagents the inventory item reagents to look for
	 * @param healthCost the health cost, in half-hearts
	 * @param manaCost the mana cost
	 * @return true if the player has all the reagents, false otherwise
	 */
	private boolean hasReagents(Player player, ItemStack[] reagents, int healthCost, int manaCost, int hungerCost, int experienceCost, int levelsCost, int durabilityCost, float moneyCost, int copperCost, Map<String, Double> variables) {
		// Is the player exempt from reagent costs?
		if (Perm.NOREAGENTS.has(player)) return true;

		// Health costs
		if (healthCost > 0 && player.getHealth() <= healthCost) return false;

		// Mana costs
		if (manaCost > 0 && (MagicSpells.getManaHandler() == null || !MagicSpells.getManaHandler().hasMana(player, manaCost))) return false;

		// Hunger costs
		if (hungerCost > 0 && player.getFoodLevel() < hungerCost) return false;

		// Experience costs
		if (experienceCost > 0 && !ExperienceUtils.hasExp(player, experienceCost)) return false;

		// Level costs
		if (levelsCost > 0 && player.getLevel() < levelsCost) return false;

		// Durabilty costs
		if (durabilityCost > 0) {
			// Durability cost is charged from the main hand item
			ItemStack inHand = player.getEquipment().getItemInMainHand();
			if (inHand == null || inHand.getDurability() >= inHand.getType().getMaxDurability()) {
				return false;
			}
		}

		// Money costs
		if (moneyCost > 0) {
			MoneyHandler moneyHandler = MagicSpells.getMoneyHandler();
			if (moneyHandler == null || !moneyHandler.hasMoney(player, moneyCost)) {
				return false;
			}
		}

		// Copper costs
		if (copperCost > 0) {
			GoldHandler goldHandler = MagicSpells.getGoldHandler();
			if (goldHandler == null || !(goldHandler.getTotalGold(player) >= copperCost)) {
				return false;
			}
		}

		// Item costs
		if (reagents != null) {
			Inventory playerInventory = player.getInventory();
			for (ItemStack item : reagents) {
				if (item != null && !inventoryContains(playerInventory, item)) {
					return false;
				}
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

		return true;
	}

	/**
	 * Removes the reagent cost of this spell from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param player the player to remove reagents from
	 */
	protected void removeReagents(Player player) {
		removeReagents(player, reagents);
	}

	// TODO can this safely be made varargs?
	/**
	 * Removes the specified reagents from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param player the player to remove the reagents from
	 * @param reagents the inventory item reagents to remove
	 */
	protected void removeReagents(Player player, ItemStack[] reagents) {
		removeReagents(player, reagents, 0, 0, 0, 0, 0, 0, 0, 0, null);
	}

	protected void removeReagents(Player player, SpellReagents reagents) {
		removeReagents(player, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels(), reagents.getDurability(), reagents.getMoney(), reagents.getCopper(), reagents.getVariables());
	}

	/**
	 * Removes the specified reagents, including health and mana, from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param player the player to remove the reagents from
	 * @param reagents the inventory item reagents to remove
	 * @param healthCost the health to remove
	 * @param manaCost the mana to remove
	 */
	private void removeReagents(Player player, ItemStack[] reagents, int healthCost, int manaCost, int hungerCost, int experienceCost, int levelsCost, int durabilityCost, float moneyCost, int copperCost, Map<String, Double> variables) {
		if (Perm.NOREAGENTS.has(player)) return;

		if (reagents != null) {
			for (ItemStack item : reagents) {
				if (item != null) Util.removeFromInventory(player.getInventory(), item);
			}
		}

		if (healthCost != 0) {
			double h = player.getHealth() - healthCost;
			if (h < 0) h = 0;
			if (h > Util.getMaxHealth(player)) h = Util.getMaxHealth(player);
			player.setHealth(h);
		}

		if (manaCost != 0) MagicSpells.getManaHandler().addMana(player, -manaCost, ManaChangeReason.SPELL_COST);

		if (hungerCost != 0) {
			int f = player.getFoodLevel() - hungerCost;
			if (f < 0) f = 0;
			if (f > 20) f = 20;
			player.setFoodLevel(f);
		}

		if (experienceCost != 0) ExperienceUtils.changeExp(player, -experienceCost);

		if (durabilityCost != 0) {
			ItemStack inHand = player.getEquipment().getItemInMainHand();
			if (inHand != null && inHand.getType().getMaxDurability() > 0) {
				short newDura = (short) (inHand.getDurability() + durabilityCost);
				if (newDura < 0) newDura = 0;
				if (newDura >= inHand.getType().getMaxDurability()) {
					player.getEquipment().setItemInMainHand(null);
				} else {
					inHand.setDurability(newDura);
					player.getEquipment().setItemInMainHand(inHand);
				}
			}
		}

		if (moneyCost != 0) {
			MoneyHandler moneyHandler = MagicSpells.getMoneyHandler();
			if (moneyHandler != null) {
				if (moneyCost > 0) moneyHandler.removeMoney(player, moneyCost);
				else moneyHandler.addMoney(player, moneyCost);
			}
		}

		if (copperCost != 0) {
			GoldHandler goldHandler = MagicSpells.getGoldHandler();

			if (goldHandler != null) {
				if (copperCost > 0) goldHandler.takeGold(copperCost, player);
				else goldHandler.giveGold(copperCost, player);
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
					varMan.modify(var.getKey(), player, -var.getValue());
				}
			}
		}
	}

	private boolean inventoryContains(Inventory inventory, ItemStack item) {
		if (inventory == null) return false;
		int count = 0;
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < 36; i++) {
			if (items[i] != null && item.isSimilar(items[i])) count += items[i].getAmount();
			if (count >= item.getAmount()) return true;
		}
		return false;
	}

	/*private void removeFromInventory(Inventory inventory, ItemStack item) {
		int amt = item.getAmount();
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && item.isSimilar(items[i])) {
				if (items[i].getAmount() > amt) {
					items[i].setAmount(items[i].getAmount() - amt);
					break;
				} else if (items[i].getAmount() == amt) {
					items[i] = null;
					break;
				} else {
					amt -= items[i].getAmount();
					items[i] = null;
				}
			}
		}
		inventory.setContents(items);
	}*/

	protected int getRange(float power) {
		return spellPowerAffectsRange ? Math.round(range * power) : range;
	}

	/**
	 * Gets the player a player is currently looking at, ignoring other living entities
	 * @param player the player to get the target for
	 * @param range the maximum range to check
	 * @param checkLos whether to obey line-of-sight restrictions
	 * @return the targeted Player, or null if none was found
	 */
	protected TargetInfo<Player> getTargetedPlayer(Player player, float power) {
		TargetInfo<LivingEntity> target = getTargetedEntity(player, power, true, null);
		if (target == null) return null;
		if (!(target.getTarget() instanceof Player)) return null;
		return new TargetInfo<>((Player) target.getTarget(), target.getPower());
	}

	protected TargetInfo<Player> getTargetPlayer(Player player, float power) {
		return getTargetedPlayer(player, power);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(Player player, float power) {
		return getTargetedEntity(player, power, false, null);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(Player player, float power, ValidTargetChecker checker) {
		return getTargetedEntity(player, power, false, checker);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(Player player, float power, boolean forceTargetPlayers, ValidTargetChecker checker) {
		// Get nearby entities
		// TODO rename to avoid hiding
		int range = getRange(power);
		List<Entity> ne = player.getNearbyEntities(range, range, range);

		// Get valid targets
		List<LivingEntity> entities;
		if (MagicSpells.plugin.checkWorldPvpFlag && validTargetList.canTargetPlayers() && !isBeneficial() && !player.getWorld().getPVP()) {
			entities = validTargetList.filterTargetListCastingAsLivingEntities(player, ne, false);
		} else if (forceTargetPlayers) {
			entities = validTargetList.filterTargetListCastingAsLivingEntities(player, ne, true);
		} else {
			entities = validTargetList.filterTargetListCastingAsLivingEntities(player, ne);
		}

		// Find target
		LivingEntity target = null;
		BlockIterator bi;
		try {
			bi = new BlockIterator(player, range);
		} catch (IllegalStateException e) {
			DebugHandler.debugIllegalState(e);
			return null;
		}
		Block b;
		Location l;
		int bx;
		int by;
		int bz;
		double ex;
		double ey;
		double ez;
		// How far can a target be from the line of sight along the x, y, and z directions
		double xTolLower = 0.75;
		double xTolUpper = 1.75;
		double yTolLower = 1;
		double yTolUpper = 2.5;
		double zTolLower = 0.75;
		double zTolUpper = 1.75;
		// Do min range
		for (int i = 0; i < minRange && bi.hasNext(); i++) {
			bi.next();
		}
		// Loop through player's line of sight
		while (bi.hasNext()) {
			b = bi.next();
			bx = b.getX();
			by = b.getY();
			bz = b.getZ();
			if (obeyLos && !BlockUtils.isTransparent(this, b)) {
				// Line of sight is broken, stop without target
				break;
			}
			// Check for entities near this block in the line of sight
			for (LivingEntity e : entities) {
				l = e.getLocation();
				ex = l.getX();
				ey = l.getY();
				ez = l.getZ();

				if (!(bx - xTolLower <= ex && ex <= bx + xTolUpper)) continue;
				if (!(bz - zTolLower <= ez && ez <= bz + zTolUpper)) continue;
				if (!(by - yTolLower <= ey && ey <= by + yTolUpper)) continue;

				// Entity is close enough, set target and stop
				target = e;

				// Check for invalid target
				if (target instanceof Player && (((Player) target).getGameMode() == GameMode.CREATIVE || ((Player) target).getGameMode() == GameMode.SPECTATOR)) {
					target = null;
					continue;
				}

				// Check for anti-magic-zone
				if (target != null && MagicSpells.getNoMagicZoneManager() != null && MagicSpells.getNoMagicZoneManager().willFizzle(target.getLocation(), this)) {
					target = null;
					continue;
				}

				// Check for teams
				if (target instanceof Player && MagicSpells.plugin.checkScoreboardTeams) {
					Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
					Team playerTeam = scoreboard.getPlayerTeam(player);
					Team targetTeam = scoreboard.getPlayerTeam((Player)target);
					if (playerTeam != null && targetTeam != null) {
						if (playerTeam.equals(targetTeam)) {
							if (!playerTeam.allowFriendlyFire() && !isBeneficial()) {
								target = null;
								continue;
							}
						} else {
							if (isBeneficial()) {
								target = null;
								continue;
							}
						}
					}
				}

				// Call event listeners
				if (target != null) {
					SpellTargetEvent event = new SpellTargetEvent(this, player, target, power);
					EventUtil.call(event);
					if (event.isCancelled()) {
						target = null;
						continue;
					} else {
						target = event.getTarget();
						power = event.getPower();
					}
				}

				// Call damage event
				if (targetDamageCause != null) {
					EntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(player, target, targetDamageCause, targetDamageAmount);
					EventUtil.call(event);
					if (event.isCancelled()) {
						target = null;
						continue;
					}
				}

				// Run checker
				if (target != null && checker != null) {
					if (!checker.isValidTarget(target)) {
						target = null;
						continue;
					}
				}
				return new TargetInfo<>(target, power);
			}

		}
		return null;
	}

	protected Block getTargetedBlock(LivingEntity entity, float power) {
		return BlockUtils.getTargetBlock(this, entity, spellPowerAffectsRange ? Math.round(range * power) : range);
	}

	protected List<Block> getLastTwoTargetedBlocks(LivingEntity entity, float power) {
		return BlockUtils.getLastTwoTargetBlock(this, entity, spellPowerAffectsRange ? Math.round(range * power) : range);
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
			if (!(entity instanceof Player)) continue;
			Player p = (Player) entity;
			Map<EffectPosition, List<Runnable>> runnablesMap = callbacks.get(p.getUniqueId().toString());
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
			if (c instanceof Effect) {
				Effect eff = (Effect)c;
				eff.cancel();
			} else {
				c.run();
			}
			cancelers.remove(c);
		}
	}

	public void cancelEffectForAllPlayers(EffectPosition pos) {
		for (String key: callbacks.keySet()) {
			cancelEffects(pos, key);
		}
	}

	protected void playSpellEffectsBuff(Entity entity, SpellEffect.SpellEffectActiveChecker checker) {
		if (effects == null) return;
		List<SpellEffect> effectsList = effects.get(EffectPosition.BUFF);
		if (effectsList != null) {
			for (SpellEffect effect : effectsList) {
				effect.playEffectWhileActiveOnEntity(entity, checker);
			}
		}
		effectsList = effects.get(EffectPosition.ORBIT);
		if (effectsList != null) {
			for (SpellEffect effect : effectsList) {
				effect.playEffectWhileActiveOrbit(entity, checker);
			}
		}
	}

	protected void registerEvents() {
		registerEvents(this);
	}

	protected void registerEvents(Listener listener) {
		MagicSpells.registerEvents(listener);
	}

	protected void unregisterEvents() {
		unregisterEvents(this);
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
	 * @param player the player to send the message to
	 * @param message the message to send
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(Player player, String message, String... replacements) {
		sendMessage(formatMessage(message, replacements), player, null);
	}

	protected void sendMessage(Player player, String message) {
		sendMessage(message, player, null);
	}

	/**
	 * Sends a message to a player. This method also does color replacement and has multi-line functionality.
	 * @param player the player to send the message to
	 * @param message the message to send
	 */
	protected void sendMessage(String message, Player player, String[] args) {
		MagicSpells.sendMessage(message, player, args);
	}

	/**
	 * Sends a message to all players near the specified player, within the configured broadcast range.
	 * @param player the "center" player used to find nearby players
	 * @param message the message to send
	 */
	protected void sendMessageNear(Player player, String message) {
		sendMessageNear(player, null, message, broadcastRange, MagicSpells.NULL_ARGS);
	}

	// TODO can this safely be made varargs?
	/**
	 * Sends a message to all players near the specified player, within the specified broadcast range.
	 * @param player the "center" player used to find nearby players
	 * @param message the message to send
	 * @param range the broadcast range
	 */
	protected void sendMessageNear(Player player, Player ignore, String message, int range, String[] args) {
		if (message == null) return;
		if (message.isEmpty()) return;
		if (Perm.SILENT.has(player)) return;

		// FIXME extract the regexp to a pattern
		String [] msgs = message.replaceAll("&([0-9a-f])", "\u00A7$1").split("\n");
		int rangeDoubled = range << 1;
		List<Entity> entities = player.getNearbyEntities(rangeDoubled, rangeDoubled, rangeDoubled);
		for (Entity entity : entities) {
			if (!(entity instanceof Player)) continue;
			if (entity == player) continue;
			if (entity == ignore) continue;
			for (String msg : msgs) {
				if (msg.isEmpty()) continue;
				entity.sendMessage(MagicSpells.plugin.textColor + msg);
			}
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
		return MagicSpells.plugin.strConsoleName;
	}

	public String getStrWrongCastItem() {
		return strWrongCastItem;
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

	public Map<String, Long> getCooldowns() {
		return nextCast;
	}

	public Map<String, VariableMod> getVariableModsCast() {
		return variableModsCast;
	}

	public Map<String, VariableMod> getVariableModsCasted() {
		return variableModsCasted;
	}

	public Map<String, VariableMod> getVariableModsTarget() {
		return variableModsTarget;
	}

	void setCooldownManually(String name, long nextCast) {
		this.nextCast.put(name, nextCast);
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

		private Player player;
		private Location prevLoc;
		private Spell spell;
		private SpellCastEvent spellCast;
		private int taskId;
		private boolean cancelled = false;
		private double motionToleranceX = 0.2;
		private double motionToleranceY = 0.2;
		private double motionToleranceZ = 0.2;

		public DelayedSpellCast(SpellCastEvent spellCast) {
			this.spellCast = spellCast;

			spell = spellCast.getSpell();
			player = spellCast.getCaster();
			prevLoc = player.getLocation().clone();
			taskId = scheduleDelayedTask(this, spellCast.getCastTime());
			registerEvents(this);
		}

		@Override
		public void run() {
			if (!cancelled && player.isOnline() && !player.isDead()) {
				Location currLoc = player.getLocation();
				if (!interruptOnMove || (Math.abs(currLoc.getX() - prevLoc.getX()) < motionToleranceX && Math.abs(currLoc.getY() - prevLoc.getY()) < motionToleranceY && Math.abs(currLoc.getZ() - prevLoc.getZ()) < motionToleranceZ)) {
					if (!spell.hasReagents(player, reagents)) spellCast.setSpellCastState(SpellCastState.MISSING_REAGENTS);
					spell.handleCast(spellCast);
				} else interrupt();
			}
			unregisterEvents(this);
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onDamage(EntityDamageEvent event) {
			if (!interruptOnDamage) return;
			if (cancelled) return;
			if (!event.getEntity().equals(player)) return;
			cancelled = true;
			MagicSpells.cancelTask(taskId);
			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellCast(SpellCastEvent event) {
			if (!interruptOnCast) return;
			if (cancelled) return;
			if (event.getSpell() instanceof PassiveSpell) return;
			if (!event.getCaster().equals(player)) return;
			cancelled = true;
			MagicSpells.cancelTask(taskId);
			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onTeleport(PlayerTeleportEvent event) {
			if (!interruptOnTeleport) return;
			if (cancelled) return;
			if (!event.getPlayer().equals(player)) return;
			cancelled = true;
			MagicSpells.cancelTask(taskId);
			interrupt();
		}

		private void interrupt() {
			sendMessage(strInterrupted, player, null);
			if (spellOnInterrupt != null) spellOnInterrupt.castSpell(player, SpellCastState.NORMAL, spellCast.getPower(), null);
		}
	}

	public class DelayedSpellCastWithBar implements Runnable, Listener {

		private Player player;
		private Location prevLoc;
		private Spell spell;
		private SpellCastEvent spellCast;
		private int castTime;
		private int taskId;
		private boolean cancelled = false;

		private int interval = 5;
		private int elapsed = 0;

		private double motionToleranceX = 0.2;
		private double motionToleranceY = 0.2;
		private double motionToleranceZ = 0.2;

		public DelayedSpellCastWithBar(SpellCastEvent spellCast) {
			this.spellCast = spellCast;

			player = spellCast.getCaster();
			spell = spellCast.getSpell();
			prevLoc = player.getLocation().clone();
			castTime = spellCast.getCastTime();

			MagicSpells.getExpBarManager().lock(player, this);
			taskId = scheduleRepeatingTask(this, interval, interval);
			registerEvents(this);
		}

		@Override
		public void run() {
			if (!cancelled && player.isOnline() && !player.isDead()) {
				elapsed += interval;
				Location currLoc = player.getLocation();
				if (!interruptOnMove || (Math.abs(currLoc.getX() - prevLoc.getX()) < motionToleranceX && Math.abs(currLoc.getY() - prevLoc.getY()) < motionToleranceY && Math.abs(currLoc.getZ() - prevLoc.getZ()) < motionToleranceZ)) {
					if (elapsed >= castTime) {
						if (!spell.hasReagents(player, reagents)) spellCast.setSpellCastState(SpellCastState.MISSING_REAGENTS);
						spell.handleCast(spellCast);
						cancelled = true;
					}
					MagicSpells.getExpBarManager().update(player, 0, (float) elapsed / (float) castTime, this);
				} else interrupt();
			} else end();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onDamage(EntityDamageEvent event) {
			if (!interruptOnDamage) return;
			if (cancelled) return;
			if (!event.getEntity().equals(player)) return;
			cancelled = true;
			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellCast(SpellCastEvent event) {
			Player caster = event.getCaster();
			if (!interruptOnCast) return;
			if (cancelled) return;
			if (event.getSpell() instanceof PassiveSpell) return;
			if (caster == null) return;
			if (!caster.equals(player)) return;
			cancelled = true;
			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onTeleport(PlayerTeleportEvent event) {
			if (interruptOnTeleport && !cancelled && event.getPlayer().equals(player)) {
				cancelled = true;
				interrupt();
			}
		}

		private void interrupt() {
			sendMessage(strInterrupted, player, null);
			end();
			if (spellOnInterrupt != null) spellOnInterrupt.castSpell(player, SpellCastState.NORMAL, spellCast.getPower(), null);
		}

		private void end() {
			cancelled = true;
			Bukkit.getScheduler().cancelTask(taskId);
			unregisterEvents(this);
			MagicSpells.getExpBarManager().unlock(player, this);
			MagicSpells.getExpBarManager().update(player, player.getLevel(), player.getExp());
			ManaHandler mana = MagicSpells.getManaHandler();
			if (mana != null) mana.showMana(player);
		}
	}

	public ValidTargetChecker getValidTargetChecker() {
		return null;
	}

}
