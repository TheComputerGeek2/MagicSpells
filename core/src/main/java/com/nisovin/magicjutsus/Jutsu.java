package com.nisovin.magicjutsus;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.LinkedHashMap;

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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.IntMap;
import com.nisovin.magicjutsus.util.TxtUtil;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.CastItem;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.mana.ManaHandler;
import com.nisovin.magicjutsus.util.VariableMod;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.LocationUtil;
import com.nisovin.magicjutsus.util.InventoryUtil;
import com.nisovin.magicjutsus.util.JutsuReagents;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;
import com.nisovin.magicjutsus.util.ExperienceUtils;
import com.nisovin.magicjutsus.util.ValidTargetList;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.mana.ManaChangeReason;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.events.JutsuCastedEvent;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.util.ValidTargetChecker;
import com.nisovin.magicjutsus.jutsueffects.JutsuEffect;
import com.nisovin.magicjutsus.variables.VariableManager;
import com.nisovin.magicjutsus.castmodifiers.ModifierSet;
import com.nisovin.magicjutsus.util.handlers.MoneyHandler;
import com.nisovin.magicjutsus.jutsueffects.EffectTracker;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.events.MagicJutsusEntityDamageByEntityEvent;

import de.slikey.effectlib.Effect;

import com.google.common.collect.Multimap;
import com.google.common.collect.ArrayListMultimap;

public abstract class Jutsu implements Comparable<Jutsu>, Listener {

	protected MagicConfig config;

	protected Map<UUID, Long> nextCast;
	protected Map<String, Integer> xpGranted;
	protected Map<String, Integer> xpRequired;
	protected Map<Jutsu, Float> sharedCooldowns;
	protected Map<String, Map<EffectPosition, List<Runnable>>> callbacks;

	protected Multimap<String, VariableMod> variableModsCast;
	protected Multimap<String, VariableMod> variableModsCasted;
	protected Multimap<String, VariableMod> variableModsTarget;

	protected IntMap<UUID> chargesConsumed;

	protected EnumMap<EffectPosition, List<JutsuEffect>> effects;

	protected Set<String> tags;
	protected Set<CastItem> bindableItems;
	protected Set<Material> losTransparentBlocks;
	protected Set<EffectTracker> effectTrackerSet;

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
	protected boolean helperJutsu;
	protected boolean alwaysGranted;
	protected boolean interruptOnMove;
	protected boolean interruptOnCast;
	protected boolean interruptOnDamage;
	protected boolean castWithLeftClick;
	protected boolean castWithRightClick;
	protected boolean interruptOnTeleport;
	protected boolean ignoreGlobalCooldown;
	protected boolean jutsuPowerAffectsRange;
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
	protected String jutsuNameOnInterrupt;

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

	protected Jutsu jutsuOnInterrupt;

	protected JutsuReagents reagents;

	protected ItemStack jutsuIcon;

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

	public Jutsu(MagicConfig config, String jutsuName) {
		this.config = config;
		this.internalName = jutsuName;

		callbacks = new HashMap<>();
		loadConfigData(config, jutsuName, "jutsus");
	}

	private void loadConfigData(MagicConfig config, String jutsuName, String section) {
		String path = section + '.' + jutsuName + '.';
		debug = config.getBoolean(path + "debug", false);
		name = config.getString(path + "name", jutsuName);
		profilingKey = "Jutsu:" + getClass().getName().replace("com.nisovin.magicjutsus.jutsus.", "") + '-' + jutsuName;
		List<String> temp = config.getStringList(path + "aliases", null);
		if (temp != null) {
			aliases = new String[temp.size()];
			aliases = temp.toArray(aliases);
		}
		helperJutsu = config.getBoolean(path + "helper-jutsu", false);
		alwaysGranted = config.getBoolean(path + "always-granted", false);
		permName = config.getString(path + "permission-name", jutsuName);
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
		castWithLeftClick = config.getBoolean(path + "cast-with-left-click", MagicJutsus.plugin.castWithLeftClick);
		castWithRightClick = config.getBoolean(path + "cast-with-right-click", MagicJutsus.plugin.castWithRightClick);
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
		String icontemp = config.getString(path + "jutsu-icon", null);
		if (icontemp == null) {
			jutsuIcon = null;
		} else {
			jutsuIcon = Util.getItemStackFromString(icontemp);
			if (jutsuIcon != null && jutsuIcon.getType() != Material.AIR) {
				jutsuIcon.setAmount(0);
				if (!icontemp.contains("|")) {
					ItemMeta iconMeta = jutsuIcon.getItemMeta();
					iconMeta.setDisplayName(MagicJutsus.getTextColor() + name);
					jutsuIcon.setItemMeta(iconMeta);
				}
			}
		}
		experience = config.getInt(path + "experience", 0);
		broadcastRange = config.getInt(path + "broadcast-range", MagicJutsus.plugin.broadcastRange);

		// Cast time
		castTime = config.getInt(path + "cast-time", 0);
		interruptOnMove = config.getBoolean(path + "interrupt-on-move", true);
		interruptOnCast = config.getBoolean(path + "interrupt-on-cast", true);
		interruptOnDamage = config.getBoolean(path + "interrupt-on-damage", false);
		interruptOnTeleport = config.getBoolean(path + "interrupt-on-teleport", true);
		jutsuNameOnInterrupt = config.getString(path + "jutsu-on-interrupt", null);

		// Targeting
		minRange = config.getInt(path + "min-range", 0);
		range = config.getInt(path + "range", 20);
		jutsuPowerAffectsRange = config.getBoolean(path + "jutsu-power-affects-range", false);
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
		losTransparentBlocks = MagicJutsus.getTransparentBlocks();
		if (config.contains(path + "los-transparent-blocks")) {
			losTransparentBlocks = Util.getMaterialList(config.getStringList(path + "los-transparent-blocks", Collections.emptyList()), HashSet::new);
			losTransparentBlocks.add(Material.AIR);
			losTransparentBlocks.add(Material.CAVE_AIR);
			losTransparentBlocks.add(Material.VOID_AIR);
		}

		// Graphical effects
		effectTrackerSet = new HashSet<>();
		if (config.contains(path + "effects")) {
			effects = new EnumMap<>(EffectPosition.class);
			if (config.isList(path + "effects")) {
				List<String> effectsList = config.getStringList(path + "effects", null);
				if (effectsList != null) {
					for (String eff : effectsList) {
						String[] data = eff.split(" ", 3);
						EffectPosition pos = EffectPosition.getPositionFromString(data[0]);
						if (pos == null) continue;
						JutsuEffect effect = JutsuEffect.createNewEffectByName(data[1]);
						if (effect == null) continue;
						effect.loadFromString(data.length > 2 ? data[2] : null);
						List<JutsuEffect> e = effects.computeIfAbsent(pos, p -> new ArrayList<>());
						e.add(effect);
					}
				}
			} else if (config.isSection(path + "effects")) {
				for (String key : config.getKeys(path + "effects")) {
					ConfigurationSection effConf = config.getSection(path + "effects." + key);
					EffectPosition pos = EffectPosition.getPositionFromString(effConf.getString("position", ""));
					if (pos == null) continue;
					JutsuEffect effect = JutsuEffect.createNewEffectByName(effConf.getString("effect", ""));
					if (effect == null) continue;
					effect.loadFromConfiguration(effConf);
					List<JutsuEffect> e = effects.computeIfAbsent(pos, p -> new ArrayList<>());
					e.add(effect);
				}
			}
		}

		//TODO load the fast mapping for effects here

		// Cost
		reagents = getConfigReagents("cost");
		if (reagents == null) reagents = new JutsuReagents();

		// Cooldowns
		cooldown = (float) config.getDouble(path + "cooldown", 0);
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
					MagicJutsus.error("Error in xp-granted entry for jutsu '" + internalName + "': " + s);
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
					MagicJutsus.error("Error in xp-required entry for jutsu '" + internalName + "': " + s);
				}
			}
		}

		// Variable options
		List<String> varModsCast = config.getStringList(path + "variable-mods-cast", null);
		if (varModsCast != null && !varModsCast.isEmpty()) {
			variableModsCast = ArrayListMultimap.create();
			for (String s : varModsCast) {
				try {
					String[] data = s.split(" ");
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					variableModsCast.put(var, varMod);
				} catch (Exception e) {
					MagicJutsus.error("Invalid variable-mods-cast option for jutsu '" + jutsuName + "': " + s);
				}
			}
		}
		List<String> varModsCasted = config.getStringList(path + "variable-mods-casted", null);
		if (varModsCasted != null && !varModsCasted.isEmpty()) {
			variableModsCasted = ArrayListMultimap.create();
			for (String s : varModsCasted) {
				try {
					String[] data = s.split(" ");
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					variableModsCasted.put(var, varMod);
				} catch (Exception e) {
					MagicJutsus.error("Invalid variable-mods-casted option for jutsu '" + jutsuName + "': " + s);
				}
			}
		}
		List<String> varModsTarget = config.getStringList(path + "variable-mods-target", null);
		if (varModsTarget != null && !varModsTarget.isEmpty()) {
			variableModsTarget = ArrayListMultimap.create();
			for (String s : varModsTarget) {
				try {
					String[] data = s.split(" ");
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					variableModsTarget.put(var, varMod);
				} catch (Exception e) {
					MagicJutsus.error("Invalid variable-mods-target option for jutsu '" + jutsuName + "': " + s);
				}
			}
		}

		soundOnCooldown = config.getString(path + "sound-on-cooldown", MagicJutsus.plugin.soundFailOnCooldown);
		soundMissingReagents = config.getString(path + "sound-missing-reagents", MagicJutsus.plugin.soundFailMissingReagents);
		if (soundOnCooldown != null && soundOnCooldown.isEmpty()) soundOnCooldown = null;
		if (soundMissingReagents != null && soundMissingReagents.isEmpty()) soundMissingReagents = null;

		// Strings
		strCost = config.getString(path + "str-cost", null);
		strCantCast = config.getString(path + "str-cant-cast", MagicJutsus.plugin.strCantCast);
		strCantBind = config.getString(path + "str-cant-bind", null);
		strCastSelf = config.getString(path + "str-cast-self", null);
		strCastStart = config.getString(path + "str-cast-start", null);
		strCastOthers = config.getString(path + "str-cast-others", null);
		strOnCooldown = config.getString(path + "str-on-cooldown", MagicJutsus.plugin.strOnCooldown);
		strWrongWorld = config.getString(path + "str-wrong-world", MagicJutsus.plugin.strWrongWorld);
		strInterrupted = config.getString(path + "str-interrupted", null);
		strXpAutoLearned = config.getString(path + "str-xp-auto-learned", MagicJutsus.plugin.strXpAutoLearned);
		strWrongCastItem = config.getString(path + "str-wrong-cast-item", strCantCast);
		strModifierFailed = config.getString(path + "str-modifier-failed", null);
		strMissingReagents = config.getString(path + "str-missing-reagents", MagicJutsus.plugin.strMissingReagents);
		if (strXpAutoLearned != null) strXpAutoLearned = strXpAutoLearned.replace("%s", name);

		tags = new HashSet<>(config.getStringList(path + "tags", new ArrayList<>()));
		tags.add("jutsu-class:" + getClass().getCanonicalName());
		tags.add("jutsu-package:" + getClass().getPackage().getName());
	}

	public Set<String> getTags() {
		return Collections.unmodifiableSet(tags);
	}

	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}

	public String getLoggingJutsuPrefix() {
		return '[' + internalName + ']';
	}

	protected JutsuReagents getConfigReagents(String option) {
		JutsuReagents reagents = null;
		List<String> costList = config.getStringList("jutsus." + internalName + '.' + option, null);
		if (costList != null && !costList.isEmpty()) {
			reagents = new JutsuReagents();
			String[] data;
			for (String costVal : costList) {
				try {
					// Parse cost data
					data = costVal.split(" ");
					int amt = 1;
					float money = 1;

					switch (data[0].toLowerCase()) {
						case "health":
							if (data.length > 1) amt = Integer.parseInt(data[1]);
							reagents.setHealth(amt);
							break;
						case "mana":
							if (data.length > 1) amt = Integer.parseInt(data[1]);
							reagents.setMana(amt);
							break;
						case "hunger":
							if (data.length > 1) amt = Integer.parseInt(data[1]);
							reagents.setHunger(amt);
							break;
						case "experience":
							if (data.length > 1) amt = Integer.parseInt(data[1]);
							reagents.setExperience(amt);
							break;
						case "levels":
							if (data.length > 1) amt = Integer.parseInt(data[1]);
							reagents.setLevels(amt);
							break;
						case "durability":
							if (data.length > 1) amt = Integer.parseInt(data[1]);
							reagents.setDurability(amt);
							break;
						case "money":
							if (data.length > 1) money = Float.parseFloat(data[1]);
							reagents.setMoney(money);
							break;
						case "variable":
							reagents.addVariable(data[1], Double.parseDouble(data[2]));
							break;
						default:
							if (data.length > 1) amt = Integer.parseInt(data[1]);
							ItemStack is = Util.getItemStackFromString(data[0]);
							if (is != null) {
								is.setAmount(amt);
								reagents.addItem(is);
							} else MagicJutsus.error("Failed to process cost value for " + internalName + " jutsu: " + costVal);
							break;
					}
				} catch (Exception e) {
					// FIXME this should not be a means of breaking
					MagicJutsus.error("Failed to process cost value for " + internalName + " jutsu: " + costVal);
				}
			}
		}
		return reagents;
	}

	// DEBUG INFO: level 2, adding modifiers to internalname
	// DEBUG INFO: level 2, adding target modifiers to internalname
	/**
	 * This method is called immediately after all jutsus have been loaded.
	 */
	protected void initialize() {
		// Modifiers
		if (modifierStrings != null && !modifierStrings.isEmpty()) {
			debug(2, "Adding modifiers to " + internalName + " jutsu");
			modifiers = new ModifierSet(modifierStrings);
			modifierStrings = null;
		}
		if (targetModifierStrings != null && !targetModifierStrings.isEmpty()) {
			debug(2, "Adding target modifiers to " + internalName + " jutsu");
			targetModifiers = new ModifierSet(targetModifierStrings);
			targetModifierStrings = null;
		}
		if (locationModifierStrings != null && !locationModifierStrings.isEmpty()) {
			debug(2, "Adding location modifiers to " + internalName + " jutsu");
			locationModifiers = new ModifierSet(locationModifierStrings);
			locationModifierStrings = null;
		}

		// Process shared cooldowns
		if (rawSharedCooldowns != null) {
			sharedCooldowns = new HashMap<>();
			for (String s : rawSharedCooldowns) {
				String[] data = s.split(" ");
				Jutsu jutsu = MagicJutsus.getJutsuByInternalName(data[0]);
				float cd = Float.parseFloat(data[1]);
				if (jutsu != null) sharedCooldowns.put(jutsu, cd);
			}
			rawSharedCooldowns.clear();
			rawSharedCooldowns = null;
		}

		// Register events
		registerEvents();

		// Other processing
		if (jutsuNameOnInterrupt != null && !jutsuNameOnInterrupt.isEmpty()) jutsuOnInterrupt = MagicJutsus.getJutsuByInternalName(jutsuNameOnInterrupt);
	}

	protected boolean configKeyExists(String key) {
		return config.contains("jutsus." + internalName + '.' + key);
	}

	/**
	 * Access an integer config value for this jutsu.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected int getConfigInt(String key, int defaultValue) {
		return config.getInt("jutsus." + internalName + '.' + key, defaultValue);
	}

	/**
	 * Access a long config value for this jutsu.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected long getConfigLong(String key, long defaultValue) {
		return config.getLong("jutsus." + internalName + '.' + key, defaultValue);
	}

	/**
	 * Access a boolean config value for this jutsu.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected boolean getConfigBoolean(String key, boolean defaultValue) {
		return config.getBoolean("jutsus." + internalName + '.' + key, defaultValue);
	}

	/**
	 * Access a String config value for this jutsu.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected String getConfigString(String key, String defaultValue) {
		return config.getString("jutsus." + internalName + '.' + key, defaultValue);
	}

	/**
	 * Access a Vector config value for this jutsu.
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
	 * Access a float config value for this jutsu.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected float getConfigFloat(String key, float defaultValue) {
		return (float) config.getDouble("jutsus." + internalName + '.' + key, defaultValue);
	}

	/**
	 * Access a double config value for this jutsu.
	 *
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 *
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected double getConfigDouble(String key, double defaultValue) {
		return config.getDouble("jutsus." + internalName + '.' + key, defaultValue);
	}

	protected List<Integer> getConfigIntList(String key, List<Integer> defaultValue) {
		return config.getIntList("jutsus." + internalName + '.' + key, defaultValue);
	}

	protected List<String> getConfigStringList(String key, List<String> defaultValue) {
		return config.getStringList("jutsus." + internalName + '.' + key, defaultValue);
	}

	protected Set<String> getConfigKeys(String key) {
		return config.getKeys("jutsus." + internalName + '.' + key);
	}

	protected ConfigurationSection getConfigSection(String key) {
		return config.getSection("jutsus." + internalName + '.' + key);
	}

	protected boolean isConfigString(String key) {
		return config.isString("jutsus." + internalName + '.' + key);
	}

	protected boolean isConfigSection(String key) {
		return config.isSection("jutsus." + internalName + '.' + key);
	}

	public final JutsuCastResult cast(LivingEntity livingEntity) {
		return cast(livingEntity, 1.0F, null);
	}

	// TODO can this safely be made varargs?
	public final JutsuCastResult cast(LivingEntity livingEntity, String[] args) {
		return cast(livingEntity, 1.0F, args);
	}

	// TODO can this safely be made varargs?
	public final JutsuCastResult cast(LivingEntity livingEntity, float power, String[] args) {
		JutsuCastEvent jutsuCast = preCast(livingEntity, power, args);
		if (jutsuCast == null) return new JutsuCastResult(JutsuCastState.CANT_CAST, PostCastAction.HANDLE_NORMALLY);
		PostCastAction action;
		int castTime = jutsuCast.getCastTime();
		if (castTime <= 0 || jutsuCast.getJutsuCastState() != JutsuCastState.NORMAL) action = handleCast(jutsuCast);
		else if (!preCastTimeCheck(livingEntity, args)) action = PostCastAction.ALREADY_HANDLED;
		else {
			action = PostCastAction.DELAYED;
			sendMessage(strCastStart, livingEntity, args);
			playJutsuEffects(EffectPosition.START_CAST, livingEntity);
			if (MagicJutsus.plugin.useExpBarAsCastTimeBar) new DelayedJutsuCastWithBar(jutsuCast);
			else new DelayedJutsuCast(jutsuCast);
		}
		return new JutsuCastResult(jutsuCast.getJutsuCastState(), action);
	}

	protected JutsuCastState getCastState(LivingEntity livingEntity) {
		if (livingEntity instanceof Player && !MagicJutsus.getJutsubook((Player) livingEntity).canCast(this)) return JutsuCastState.CANT_CAST;
		if (worldRestrictions != null && !worldRestrictions.contains(livingEntity.getWorld().getName())) return JutsuCastState.WRONG_WORLD;
		if (MagicJutsus.getNoMagicZoneManager() != null && MagicJutsus.getNoMagicZoneManager().willFizzle(livingEntity, this)) return JutsuCastState.NO_MAGIC_ZONE;
		if (onCooldown(livingEntity)) return JutsuCastState.ON_COOLDOWN;
		if (!hasReagents(livingEntity)) return JutsuCastState.MISSING_REAGENTS;
		return JutsuCastState.NORMAL;
	}

	// TODO can this safely be made varargs?
	// DEBUG INFO: level 2, jutsu cast state
	// DEBUG INFO: level 2, jutsu canceled
	// DEBUG INFO: level 2, jutsu cast state changed
	protected JutsuCastEvent preCast(LivingEntity livingEntity, float power, String[] args) {
		// Get jutsu state
		JutsuCastState state = getCastState(livingEntity);
		debug(2, "    Jutsu cast state: " + state);

		// Call events
		JutsuCastEvent event = new JutsuCastEvent(this, livingEntity, state, power, args, cooldown, reagents.clone(), castTime);
		EventUtil.call(event);
		if (event.isCancelled()) {
			debug(2, "    Jutsu cancelled");
			return null;
		}

		if (event.haveReagentsChanged()) {
			boolean hasReagents = hasReagents(livingEntity, event.getReagents());
			if (!hasReagents && state != JutsuCastState.MISSING_REAGENTS) {
				event.setJutsuCastState(JutsuCastState.MISSING_REAGENTS);
				debug(2, "    Jutsu cast state changed: " + state);
			} else if (hasReagents && state == JutsuCastState.MISSING_REAGENTS) {
				event.setJutsuCastState(state = JutsuCastState.NORMAL);
				debug(2, "    Jutsu cast state changed: " + state);
			}
		}

		if (event.hasJutsuCastStateChanged()) debug(2, "    Jutsu cast state changed: " + state);
		if (Perm.NOCASTTIME.has(livingEntity)) event.setCastTime(0);
		return event;
	}

	// DEBUG INFO: level 3, power #
	// DEBUG INFO: level 3, cooldown #
	// DEBUG INFO: level 3, args argsvalue
	PostCastAction handleCast(JutsuCastEvent jutsuCast) {
		long start = System.nanoTime();
		LivingEntity livingEntity = jutsuCast.getCaster();
		JutsuCastState state = jutsuCast.getJutsuCastState();
		String[] args = jutsuCast.getJutsuArgs();
		float power = jutsuCast.getPower();
		debug(3, "    Power: " + power);
		debug(3, "    Cooldown: " + cooldown);
		if (MagicJutsus.plugin.debug && args != null && args.length > 0) debug(3, "    Args: {" + Util.arrayJoin(args, ',') + '}');
		PostCastAction action = castJutsu(livingEntity, state, power, args);
		if (MagicJutsus.plugin.enableProfiling) {
			Long total = MagicJutsus.getProfilingTotalTime().get(profilingKey);
			if (total == null) total = (long) 0;
			total += System.nanoTime() - start;
			MagicJutsus.getProfilingTotalTime().put(profilingKey, total);
			Integer runs = MagicJutsus.getProfilingRuns().get(profilingKey);
			if (runs == null) runs = 0;
			runs += 1;
			MagicJutsus.getProfilingRuns().put(profilingKey, runs);
		}
		postCast(jutsuCast, action);
		return action;
	}

	// FIXME save the results of the redundant calculations or be cleaner about it
	// DEBUG INFO: level 3, post cast action actionName
	protected void postCast(JutsuCastEvent jutsuCast, PostCastAction action) {
		debug(3, "    Post-cast action: " + action);
		LivingEntity livingEntity = jutsuCast.getCaster();
		JutsuCastState state = jutsuCast.getJutsuCastState();
		if (action != null && action != PostCastAction.ALREADY_HANDLED) {
			if (state == JutsuCastState.NORMAL) {
				if (action.setCooldown()) setCooldown(livingEntity, jutsuCast.getCooldown());
				if (action.chargeReagents()) removeReagents(livingEntity, jutsuCast.getReagents());
				if (action.sendMessages()) sendMessages(livingEntity, jutsuCast.getJutsuArgs());
				if (experience > 0 && livingEntity instanceof Player) ((Player) livingEntity).giveExp(experience);
			} else if (state == JutsuCastState.ON_COOLDOWN) {
				String message = formatMessage(strOnCooldown, "%c", Math.round(getCooldown(livingEntity)) + "");
				message = formatMessage(message, "%s", jutsuCast.getJutsu().getName());
				MagicJutsus.sendMessage(message, livingEntity, jutsuCast.getJutsuArgs());
				playJutsuEffects(EffectPosition.COOLDOWN, livingEntity);
				if (soundOnCooldown != null && livingEntity instanceof Player) ((Player) livingEntity).playSound(livingEntity.getLocation(), soundOnCooldown, 1F, 1F);
			} else if (state == JutsuCastState.MISSING_REAGENTS) {
				MagicJutsus.sendMessage(strMissingReagents, livingEntity, jutsuCast.getJutsuArgs());
				playJutsuEffects(EffectPosition.MISSING_REAGENTS, livingEntity);
				if (MagicJutsus.plugin.showStrCostOnMissingReagents && strCost != null && !strCost.isEmpty()) MagicJutsus.sendMessage("    (" + strCost + ')', livingEntity, jutsuCast.getJutsuArgs());
				if (soundMissingReagents != null && livingEntity instanceof Player) ((Player) livingEntity).playSound(livingEntity.getLocation(), soundMissingReagents, 1F, 1F);
			} else if (state == JutsuCastState.CANT_CAST) {
				MagicJutsus.sendMessage(strCantCast, livingEntity, jutsuCast.getJutsuArgs());
			} else if (state == JutsuCastState.NO_MAGIC_ZONE) {
				MagicJutsus.getNoMagicZoneManager().sendNoMagicMessage(livingEntity, this);
			} else if (state == JutsuCastState.WRONG_WORLD) {
				MagicJutsus.sendMessage(strWrongWorld, livingEntity, jutsuCast.getJutsuArgs());
			}
		}
		JutsuCastedEvent event = new JutsuCastedEvent(this, livingEntity, state, jutsuCast.getPower(), jutsuCast.getJutsuArgs(), cooldown, reagents, action);
		EventUtil.call(event);
	}

	// TODO can this safely be made varargs?
	public void sendMessages(LivingEntity livingEntity, String[] args) {
		sendMessage(formatMessage(strCastSelf, "%a", livingEntity.getName()), livingEntity, args);
		sendMessageNear(livingEntity, formatMessage(strCastOthers, "%a", livingEntity.getName()));
	}

	// TODO can this safely be made varargs?
	protected boolean preCastTimeCheck(LivingEntity livingEntity, String[] args) {
		return true;
	}

	// TODO can this safely be made varargs?
	/**
	 * This method is called when a player casts a jutsu, either by command, with a wand item, or otherwise.
	 * @param livingEntity the living entity casting the jutsu
	 * @param state the state of the jutsu cast (normal, on cooldown, missing reagents, etc)
	 * @param power the power multiplier the jutsu should be cast with (1.0 is normal)
	 * @param args the jutsu arguments, if cast by command
	 * @return the action to take after the jutsu is processed
	 */
	public abstract PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args);

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

	protected List<String> tabCompleteJutsuName(CommandSender sender, String partial) {
		return TxtUtil.tabCompleteJutsuName(sender, partial);
	}

	// TODO can this safely be made varargs?
	/**
	 * This method is called when the jutsu is cast from the console.
	 * @param sender the console sender.
	 * @param args the command arguments
	 * @return true if the jutsu was handled, false otherwise
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

	public ItemStack getJutsuIcon() {
		return jutsuIcon;
	}

	public String getCostStr() {
		if (strCost == null || strCost.isEmpty()) return null;
		return strCost;
	}

	/**
	 * Check whether this jutsu is currently on cooldown for the specified player
	 * @param livingEntity The living entity to check
	 * @return whether the jutsu is on cooldown
	 */
	public boolean onCooldown(LivingEntity livingEntity) {
		if (Perm.NOCOOLDOWN.has(livingEntity)) return false;
		if (charges > 0) return chargesConsumed.get(livingEntity.getUniqueId()) >= charges;
		if (serverCooldown > 0 && nextCastServer > System.currentTimeMillis()) return true;

		Long next = nextCast.get(livingEntity.getUniqueId());
		if (next != null && next > System.currentTimeMillis()) return true;
		return false;
	}

	public float getCooldown() {
		return cooldown;
	}

	/**
	 * Get how many seconds remain on the cooldown of this jutsu for the specified player
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
	 * Begins the cooldown for the jutsu for the specified player
	 * @param livingEntity The living entity to set the cooldown for
	 */
	public void setCooldown(LivingEntity livingEntity, float cooldown) {
		setCooldown(livingEntity, cooldown, true);
	}

	/**
	 * Begins the cooldown for the jutsu for the specified player
	 * @param livingEntity The living entity to set the cooldown for
	 */
	public void setCooldown(final LivingEntity livingEntity, float cooldown, boolean activateSharedCooldowns) {
		if (cooldown > 0) {
			if (charges <= 0) {
				nextCast.put(livingEntity.getUniqueId(), System.currentTimeMillis() + (long) (cooldown * TimeUtil.MILLISECONDS_PER_SECOND));
			} else {
				final UUID uuid = livingEntity.getUniqueId();
				chargesConsumed.increment(uuid);
				MagicJutsus.scheduleDelayedTask(() -> {
					chargesConsumed.decrement(uuid);
					playJutsuEffects(EffectPosition.CHARGE_USE, livingEntity);
					if (rechargeSound == null) return;
					if (rechargeSound.isEmpty()) return;
					if (livingEntity instanceof Player) ((Player) livingEntity).playSound(livingEntity.getLocation(), rechargeSound, 1.0F, 1.0F);
				}, Math.round(TimeUtil.TICKS_PER_SECOND * cooldown));
			}
		} else {
			if (charges <= 0) nextCast.remove(livingEntity.getUniqueId());
			else chargesConsumed.remove(livingEntity.getUniqueId());
		}
		if (serverCooldown > 0) nextCastServer = System.currentTimeMillis() + (long) (serverCooldown * TimeUtil.MILLISECONDS_PER_SECOND);
		if (activateSharedCooldowns && sharedCooldowns != null) {
			for (Map.Entry<Jutsu, Float> scd : sharedCooldowns.entrySet()) {
				scd.getKey().setCooldown(livingEntity, scd.getValue(), false);
			}
		}
	}

	/**
	 * Checks if a player has the reagents required to cast this jutsu
	 * @param livingEntity the living entity to check
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(LivingEntity livingEntity) {
		return hasReagents(livingEntity, reagents);
	}

	// FIXME this doesn't seem strictly tied to Jutsu logic, could probably be moved
	/**
	 * Checks if a player has the reagents required to cast this jutsu
	 * @param livingEntity the living entity to check
	 * @param reagents the reagents to check for
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(LivingEntity livingEntity, JutsuReagents reagents) {
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
	private boolean hasReagents(LivingEntity livingEntity, ItemStack[] reagents, int healthCost, int manaCost, int hungerCost, int experienceCost, int levelsCost, int durabilityCost, float moneyCost, Map<String, Double> variables) {
		// Is the livingEntity exempt from reagent costs?
		if (Perm.NOREAGENTS.has(livingEntity)) return true;

		// player reagents
		if (livingEntity instanceof Player) {
			// Mana costs
			if (manaCost > 0 && (MagicJutsus.getManaHandler() == null || !MagicJutsus.getManaHandler().hasMana((Player) livingEntity, manaCost))) return false;

			// Hunger costs
			if (hungerCost > 0 && ((Player) livingEntity).getFoodLevel() < hungerCost) return false;

			// Experience costs
			if (experienceCost > 0 && !ExperienceUtils.hasExp((Player) livingEntity, experienceCost)) return false;

			// Level costs
			if (levelsCost > 0 && ((Player) livingEntity).getLevel() < levelsCost) return false;

			// Money costs
			if (moneyCost > 0) {
				MoneyHandler moneyHandler = MagicJutsus.getMoneyHandler();
				if (moneyHandler == null || !moneyHandler.hasMoney((Player) livingEntity, moneyCost)) {
					return false;
				}
			}

			// Variable costs
			if (variables != null) {
				VariableManager varMan = MagicJutsus.getVariableManager();
				if (varMan == null) return false;
				for (Map.Entry<String, Double> var : variables.entrySet()) {
					double val = var.getValue();
					if (val > 0 && varMan.getValue(var.getKey(), (Player) livingEntity) < val) return false;
				}
			}
		}

		// Health costs
		if (healthCost > 0 && livingEntity.getHealth() <= healthCost) return false;

		// Durabilty costs
		if (durabilityCost > 0) {
			// Durability cost is charged from the main hand item
			ItemStack inHand = livingEntity.getEquipment().getItemInMainHand();
			if (inHand == null) return false;
			if (!(inHand.getItemMeta() instanceof Damageable)) return false;
			if (((Damageable) inHand.getItemMeta()).getDamage() >= inHand.getType().getMaxDurability()) return false;
		}

		// Item costs
		if (reagents != null) {
			if (livingEntity instanceof Player) {
				Inventory playerInventory = ((Player) livingEntity).getInventory();
				for (ItemStack item : reagents) {
					if (item == null) continue;
					if (InventoryUtil.inventoryContains(playerInventory, item)) continue;
					return false;
				}
			} else {
				EntityEquipment entityEquipment = livingEntity.getEquipment();
				for (ItemStack item : reagents) {
					if (item == null) continue;
					if (InventoryUtil.inventoryContains(entityEquipment, item)) continue;
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Removes the reagent cost of this jutsu from the player's inventory.
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
	protected void removeReagents(LivingEntity livingEntity, ItemStack[] reagents) {
		removeReagents(livingEntity, reagents, 0, 0, 0, 0, 0, 0, 0, null);
	}

	protected void removeReagents(LivingEntity livingEntity, JutsuReagents reagents) {
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
	private void removeReagents(LivingEntity livingEntity, ItemStack[] reagents, int healthCost, int manaCost, int hungerCost, int experienceCost, int levelsCost, int durabilityCost, float moneyCost, Map<String, Double> variables) {
		if (Perm.NOREAGENTS.has(livingEntity)) return;

		if (reagents != null) {
			for (ItemStack item : reagents) {
				if (item == null) continue;
				if (livingEntity instanceof Player) Util.removeFromInventory(((Player) livingEntity).getInventory(), item);
				else if (livingEntity.getEquipment() != null) Util.removeFromInventory(livingEntity.getEquipment(), item);
			}
		}

		if (livingEntity instanceof Player) {
			if (manaCost != 0) MagicJutsus.getManaHandler().addMana((Player) livingEntity, -manaCost, ManaChangeReason.JUTSU_COST);

			if (hungerCost != 0) {
				int f = ((Player) livingEntity).getFoodLevel() - hungerCost;
				if (f < 0) f = 0;
				if (f > 20) f = 20;
				((Player) livingEntity).setFoodLevel(f);
			}

			if (experienceCost != 0) ExperienceUtils.changeExp((Player) livingEntity, -experienceCost);

			if (moneyCost != 0) {
				MoneyHandler moneyHandler = MagicJutsus.getMoneyHandler();
				if (moneyHandler != null) {
					if (moneyCost > 0) moneyHandler.removeMoney((Player) livingEntity, moneyCost);
					else moneyHandler.addMoney((Player) livingEntity, moneyCost);
				}
			}

			if (levelsCost != 0) {
				int lvl = ((Player) livingEntity).getLevel() - levelsCost;
				if (lvl < 0) lvl = 0;
				((Player) livingEntity).setLevel(lvl);
			}

			if (variables != null) {
				VariableManager varMan = MagicJutsus.getVariableManager();
				if (varMan != null) {
					for (Map.Entry<String, Double> var : variables.entrySet()) {
						varMan.modify(var.getKey(), (Player) livingEntity, -var.getValue());
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
			ItemStack inHand = livingEntity.getEquipment().getItemInMainHand();
			if (inHand != null && inHand.getItemMeta() instanceof Damageable && inHand.getType().getMaxDurability() > 0) {
				short newDura = (short) (((Damageable) inHand.getItemMeta()).getDamage() + durabilityCost);
				if (newDura < 0) newDura = 0;
				if (newDura >= inHand.getType().getMaxDurability()) {
					livingEntity.getEquipment().setItemInMainHand(null);
				} else {
					((Damageable) inHand.getItemMeta()).setDamage(newDura);
					livingEntity.getEquipment().setItemInMainHand(inHand);
				}
			}
		}
	}

	protected int getRange(float power) {
		return jutsuPowerAffectsRange ? Math.round(range * power) : range;
	}

	/**
	 * Gets the player a player is currently looking at, ignoring other living entities
	 * @param livingEntity the living entity to get the target for
	 * @return the targeted Player, or null if none was found
	 */
	protected TargetInfo<Player> getTargetedPlayer(LivingEntity livingEntity, float power) {
		TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power, true, null);
		if (target == null) return null;
		if (!(target.getTarget() instanceof Player)) return null;
		return new TargetInfo<>((Player) target.getTarget(), target.getPower());
	}

	protected TargetInfo<Player> getTargetPlayer(LivingEntity livingEntity, float power) {
		return getTargetedPlayer(livingEntity, power);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity livingEntity, float power) {
		return getTargetedEntity(livingEntity, power, false, null);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity livingEntity, float power, ValidTargetChecker checker) {
		return getTargetedEntity(livingEntity, power, false, checker);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity livingEntity, float power, boolean forceTargetPlayers, ValidTargetChecker checker) {
		// Get nearby entities
		// TODO rename to avoid hiding
		int range = getRange(power);
		List<Entity> ne = livingEntity.getNearbyEntities(range, range, range);

		// Get valid targets
		List<LivingEntity> entities;
		if (MagicJutsus.plugin.checkWorldPvpFlag && validTargetList.canTargetPlayers() && !isBeneficial() && !livingEntity.getWorld().getPVP()) {
			entities = validTargetList.filterTargetListCastingAsLivingEntities(livingEntity, ne, false);
		} else if (forceTargetPlayers) {
			entities = validTargetList.filterTargetListCastingAsLivingEntities(livingEntity, ne, true);
		} else {
			entities = validTargetList.filterTargetListCastingAsLivingEntities(livingEntity, ne);
		}

		// Find target
		LivingEntity target = null;
		BlockIterator bi;
		try {
			bi = new BlockIterator(livingEntity, range);
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
				if (target != null && MagicJutsus.getNoMagicZoneManager() != null && MagicJutsus.getNoMagicZoneManager().willFizzle(target.getLocation(), this)) {
					target = null;
					continue;
				}

				// Check for teams
				if (target instanceof Player && MagicJutsus.plugin.checkScoreboardTeams) {
					Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
					Team playerTeam = null;
					if (livingEntity instanceof Player) scoreboard.getEntryTeam(livingEntity.getName());
					Team targetTeam = scoreboard.getEntryTeam(target.getName());
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
					JutsuTargetEvent event = new JutsuTargetEvent(this, livingEntity, target, power);
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
					EntityDamageByEntityEvent event = new MagicJutsusEntityDamageByEntityEvent(livingEntity, target, targetDamageCause, targetDamageAmount);
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
		return BlockUtils.getTargetBlock(this, entity, jutsuPowerAffectsRange ? Math.round(range * power) : range);
	}

	protected List<Block> getLastTwoTargetedBlocks(LivingEntity entity, float power) {
		return BlockUtils.getLastTwoTargetBlock(this, entity, jutsuPowerAffectsRange ? Math.round(range * power) : range);
	}

	public Set<Material> getLosTransparentBlocks() {
		return losTransparentBlocks;
	}

	public boolean isTransparent(Block block) {
		return losTransparentBlocks.contains(block.getType());
	}

	protected void playJutsuEffects(Entity pos1, Entity pos2) {
		playJutsuEffects(EffectPosition.CASTER, pos1);
		playJutsuEffects(EffectPosition.TARGET, pos2);
		playJutsuEffectsTrail(pos1.getLocation(), pos2.getLocation());
	}

	protected void playJutsuEffects(Entity pos1, Location pos2) {
		playJutsuEffects(EffectPosition.CASTER, pos1);
		playJutsuEffects(EffectPosition.TARGET, pos2);
		playJutsuEffectsTrail(pos1.getLocation(), pos2);
	}

	protected void playJutsuEffects(Location pos1, Entity pos2) {
		playJutsuEffects(EffectPosition.CASTER, pos1);
		playJutsuEffects(EffectPosition.TARGET, pos2);
		playJutsuEffectsTrail(pos1, pos2.getLocation());
	}

	protected void playJutsuEffects(Location pos1, Location pos2) {
		playJutsuEffects(EffectPosition.CASTER, pos1);
		playJutsuEffects(EffectPosition.TARGET, pos2);
		playJutsuEffectsTrail(pos1, pos2);
	}

	protected void playJutsuEffects(EffectPosition pos, Entity entity) {
		if (effects == null) return;
		List<JutsuEffect> effectsList = effects.get(pos);
		if (effectsList == null) return;
		for (JutsuEffect effect : effectsList) {
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

	protected void playJutsuEffects(EffectPosition pos, Location location) {
		if (effects == null) return;
		List<JutsuEffect> effectsList = effects.get(pos);
		if (effectsList == null) return;
		for (JutsuEffect effect : effectsList) {
			effect.playEffect(location);
		}
	}

	protected void playJutsuEffectsTrail(Location loc1, Location loc2) {
		if (effects == null) return;
		if (!LocationUtil.isSameWorld(loc1, loc2)) return;
		List<JutsuEffect> effectsList = effects.get(EffectPosition.TRAIL);
		if (effectsList != null) {
			for (JutsuEffect effect : effectsList) {
				effect.playEffect(loc1, loc2);
			}
		}
		List<JutsuEffect> rTrailEffects = effects.get(EffectPosition.REVERSE_LINE);
		if (rTrailEffects != null) {
			for (JutsuEffect effect: rTrailEffects) {
				effect.playEffect(loc2, loc1);
			}
		}
	}

	public void playTrackingLinePatterns(EffectPosition pos, Location origin, Location target, Entity originEntity, Entity targetEntity) {
		if (effects == null) return;
		List<JutsuEffect> jutsuEffects = effects.get(pos);
		if (jutsuEffects == null) return;
		for (JutsuEffect e: jutsuEffects) {
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

	public Set<EffectTracker> getEffectTrackers() {
		return effectTrackerSet;
	}

	protected void playJutsuEffectsBuff(Entity entity, JutsuEffect.JutsuEffectActiveChecker checker) {
		if (effects == null) return;
		List<JutsuEffect> effectsList = effects.get(EffectPosition.BUFF);
		if (effectsList != null) {
			for (JutsuEffect effect : effectsList) {
				effectTrackerSet.add(effect.playEffectWhileActiveOnEntity(entity, checker));
			}
		}
		effectsList = effects.get(EffectPosition.ORBIT);
		if (effectsList != null) {
			for (JutsuEffect effect : effectsList) {
				effectTrackerSet.add(effect.playEffectWhileActiveOrbit(entity, checker));
			}
		}
	}

	protected void registerEvents() {
		registerEvents(this);
	}

	protected void registerEvents(Listener listener) {
		MagicJutsus.registerEvents(listener);
	}

	protected void unregisterEvents() {
		unregisterEvents(this);
	}

	protected void unregisterEvents(Listener listener) {
		HandlerList.unregisterAll(listener);
	}

	protected int scheduleDelayedTask(Runnable task, int delay) {
		return MagicJutsus.scheduleDelayedTask(task, delay);
	}

	protected int scheduleRepeatingTask(Runnable task, int delay, int interval) {
		return MagicJutsus.scheduleRepeatingTask(task, delay, interval);
	}

	protected Ninjutsu initNinjutsu(String ninjutsuName, String errorMessage) {
		if (ninjutsuName.isEmpty()) return null;
		Ninjutsu ninjutsu = new Ninjutsu(ninjutsuName);
		if (!ninjutsu.process()) {
			MagicJutsus.error(errorMessage);
			ninjutsu = null;
		}
		return ninjutsu;
	}

	/**
	 * Formats a string by performing the specified replacements.
	 * @param message the string to format
	 * @param replacements the replacements to make, in pairs.
	 * @return the formatted string
	 */
	protected String formatMessage(String message, String... replacements) {
		return MagicJutsus.formatMessage(message, replacements);
	}

	/**
	 * Sends a message to a player, first making the specified replacements. This method also does color replacement and has multi-line functionality.
	 * @param livingEntity the living entity to send the message to
	 * @param message the message to send
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(LivingEntity livingEntity, String message, String... replacements) {
		sendMessage(formatMessage(message, replacements), livingEntity, null);
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
		MagicJutsus.sendMessage(message, livingEntity, args);
	}

	/**
	 * Sends a message to all players near the specified player, within the configured broadcast range.
	 * @param livingEntity the "center" living entity used to find nearby players
	 * @param message the message to send
	 */
	protected void sendMessageNear(LivingEntity livingEntity, String message) {
		sendMessageNear(livingEntity, null, message, broadcastRange, MagicJutsus.NULL_ARGS);
	}

	// TODO can this safely be made varargs?
	/**
	 * Sends a message to all players near the specified player, within the specified broadcast range.
	 * @param livingEntity the "center" living entity used to find nearby players
	 * @param message the message to send
	 * @param range the broadcast range
	 */
	protected void sendMessageNear(LivingEntity livingEntity, Player ignore, String message, int range, String[] args) {
		if (message == null) return;
		if (message.isEmpty()) return;
		if (Perm.SILENT.has(livingEntity)) return;

		// FIXME extract the regexp to a pattern
		String [] msgs = message.replaceAll("&([0-9a-f])", "\u00A7$1").split("\n");
		int rangeDoubled = range << 1;
		List<Entity> entities = livingEntity.getNearbyEntities(rangeDoubled, rangeDoubled, rangeDoubled);
		for (Entity entity : entities) {
			if (!(entity instanceof Player)) continue;
			if (entity == livingEntity) continue;
			if (entity == ignore) continue;
			for (String msg : msgs) {
				if (msg.isEmpty()) continue;
				entity.sendMessage(MagicJutsus.plugin.textColor + msg);
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

	public boolean isHelperJutsu() {
		return helperJutsu;
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

	public JutsuReagents getReagents() {
		return reagents;
	}

	public String getConsoleName() {
		return MagicJutsus.plugin.strConsoleName;
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

	void setCooldownManually(UUID uuid, long nextCast) {
		this.nextCast.put(uuid, nextCast);
	}

	protected void debug(int level, String message) {
		if (debug) MagicJutsus.debug(level, message);
	}

	/**
	 * This method is called when the plugin is being disabled, for any reason.
	 */
	protected void turnOff() {
		// No op
	}

	@Override
	public int compareTo(Jutsu jutsu) {
		return name.compareTo(jutsu.name);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Jutsu && ((Jutsu) o).internalName.equals(internalName);
	}

	@Override
	public int hashCode() {
		return internalName.hashCode();
	}

	// TODO move this to its own class
	public enum JutsuCastState {

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

	public static class JutsuCastResult {

		public JutsuCastState state;
		public PostCastAction action;

		public JutsuCastResult(JutsuCastState state, PostCastAction action) {
			this.state = state;
			this.action = action;
		}

	}

	public class DelayedJutsuCast implements Runnable, Listener {

		private LivingEntity livingEntity;
		private Location prevLoc;
		private Jutsu jutsu;
		private JutsuCastEvent jutsuCast;
		private int taskId;
		private boolean cancelled = false;
		private double motionToleranceX = 0.2;
		private double motionToleranceY = 0.2;
		private double motionToleranceZ = 0.2;

		public DelayedJutsuCast(JutsuCastEvent jutsuCast) {
			this.jutsuCast = jutsuCast;

			jutsu = jutsuCast.getJutsu();
			livingEntity = jutsuCast.getCaster();
			prevLoc = livingEntity.getLocation().clone();
			taskId = scheduleDelayedTask(this, jutsuCast.getCastTime());
			registerEvents(this);
		}

		@Override
		public void run() {
			if (!cancelled && livingEntity.isValid() && !livingEntity.isDead()) {
				Location currLoc = livingEntity.getLocation();
				if (!interruptOnMove || (Math.abs(currLoc.getX() - prevLoc.getX()) < motionToleranceX && Math.abs(currLoc.getY() - prevLoc.getY()) < motionToleranceY && Math.abs(currLoc.getZ() - prevLoc.getZ()) < motionToleranceZ)) {
					if (!jutsu.hasReagents(livingEntity, reagents)) jutsuCast.setJutsuCastState(JutsuCastState.MISSING_REAGENTS);
					jutsu.handleCast(jutsuCast);
				} else interrupt();
			}
			unregisterEvents(this);
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onDamage(EntityDamageEvent event) {
			if (!interruptOnDamage) return;
			if (cancelled) return;
			if (!event.getEntity().equals(livingEntity)) return;
			cancelled = true;
			MagicJutsus.cancelTask(taskId);
			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onJutsuCast(JutsuCastEvent event) {
			if (!interruptOnCast) return;
			if (cancelled) return;
			if (event.getJutsu() instanceof PassiveJutsu) return;
			if (!event.getCaster().equals(livingEntity)) return;
			cancelled = true;
			MagicJutsus.cancelTask(taskId);
			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onTeleport(PlayerTeleportEvent event) {
			if (!interruptOnTeleport) return;
			if (cancelled) return;
			if (!event.getPlayer().equals(livingEntity)) return;
			cancelled = true;
			MagicJutsus.cancelTask(taskId);
			interrupt();
		}

		private void interrupt() {
			sendMessage(strInterrupted, livingEntity, null);
			if (jutsuOnInterrupt != null) jutsuOnInterrupt.castJutsu(livingEntity, JutsuCastState.NORMAL, jutsuCast.getPower(), null);
		}
	}

	public class DelayedJutsuCastWithBar implements Runnable, Listener {

		private LivingEntity livingEntity;
		private Location prevLoc;
		private Jutsu jutsu;
		private JutsuCastEvent jutsuCast;
		private int castTime;
		private int taskId;
		private boolean cancelled = false;

		private int interval = 5;
		private int elapsed = 0;

		private double motionToleranceX = 0.2;
		private double motionToleranceY = 0.2;
		private double motionToleranceZ = 0.2;

		public DelayedJutsuCastWithBar(JutsuCastEvent jutsuCast) {
			this.jutsuCast = jutsuCast;

			livingEntity = jutsuCast.getCaster();
			jutsu = jutsuCast.getJutsu();
			prevLoc = livingEntity.getLocation().clone();
			castTime = jutsuCast.getCastTime();

			if (livingEntity instanceof Player) MagicJutsus.getExpBarManager().lock((Player) livingEntity, this);
			taskId = scheduleRepeatingTask(this, interval, interval);
			registerEvents(this);
		}

		@Override
		public void run() {
			if (!cancelled && livingEntity.isValid() && !livingEntity.isDead()) {
				elapsed += interval;
				Location currLoc = livingEntity.getLocation();
				if (!interruptOnMove || (Math.abs(currLoc.getX() - prevLoc.getX()) < motionToleranceX && Math.abs(currLoc.getY() - prevLoc.getY()) < motionToleranceY && Math.abs(currLoc.getZ() - prevLoc.getZ()) < motionToleranceZ)) {
					if (elapsed >= castTime) {
						if (!jutsu.hasReagents(livingEntity, reagents)) jutsuCast.setJutsuCastState(JutsuCastState.MISSING_REAGENTS);
						jutsu.handleCast(jutsuCast);
						cancelled = true;
					}
					if (livingEntity instanceof Player) MagicJutsus.getExpBarManager().update((Player) livingEntity, 0, (float) elapsed / (float) castTime, this);
				} else interrupt();
			} else end();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onDamage(EntityDamageEvent event) {
			if (!interruptOnDamage) return;
			if (cancelled) return;
			if (!event.getEntity().equals(livingEntity)) return;
			cancelled = true;
			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onJutsuCast(JutsuCastEvent event) {
			LivingEntity caster = event.getCaster();
			if (!interruptOnCast) return;
			if (cancelled) return;
			if (event.getJutsu() instanceof PassiveJutsu) return;
			if (caster == null) return;
			if (!caster.equals(livingEntity)) return;
			cancelled = true;
			interrupt();
		}

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onTeleport(PlayerTeleportEvent event) {
			if (interruptOnTeleport && !cancelled && event.getPlayer().equals(livingEntity)) {
				cancelled = true;
				interrupt();
			}
		}

		private void interrupt() {
			sendMessage(strInterrupted, livingEntity, null);
			end();
			if (jutsuOnInterrupt != null) jutsuOnInterrupt.castJutsu(livingEntity, JutsuCastState.NORMAL, jutsuCast.getPower(), null);
		}

		private void end() {
			cancelled = true;
			MagicJutsus.cancelTask(taskId);
			unregisterEvents(this);
			if (livingEntity instanceof Player) {
				MagicJutsus.getExpBarManager().unlock((Player) livingEntity, this);
				MagicJutsus.getExpBarManager().update((Player) livingEntity, ((Player) livingEntity).getLevel(), ((Player) livingEntity).getExp());
				ManaHandler mana = MagicJutsus.getManaHandler();
				if (mana != null) mana.showMana((Player) livingEntity);
			}
		}
	}

	public ValidTargetChecker getValidTargetChecker() {
		return null;
	}

}
