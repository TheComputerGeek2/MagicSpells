package com.nisovin.magicspells;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.slikey.effectlib.Effect;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.function.Predicate;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.collect.*;
import com.google.common.base.Functions;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.VoxelShape;
import org.bukkit.util.BoundingBox;
import org.bukkit.damage.DamageType;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.damage.DamageSource;
import org.bukkit.util.RayTraceResult;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventPriority;
import org.bukkit.block.data.BlockData;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.block.fluid.FluidData;
import io.papermc.paper.registry.RegistryAccess;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.events.*;
import com.nisovin.magicspells.util.config.*;
import com.nisovin.magicspells.spelleffects.*;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.effecttypes.*;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;
import com.nisovin.magicspells.spelleffects.trackers.EffectTracker;
import com.nisovin.magicspells.spelleffects.effecttypes.EntityEffect;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;
import com.nisovin.magicspells.spelleffects.trackers.AsyncEffectTracker;

/**
 * Annotate this class with {@link DependsOn} if you require certain plugins to be enabled before this spell is.
 */
public abstract class Spell implements Comparable<Spell>, Listener {

	protected static final Random random = ThreadLocalRandom.current();

	protected MagicConfig config;

	protected Map<UUID, Long> nextCast;
	protected Map<String, Integer> xpGranted;
	protected Map<String, Integer> xpRequired;
	protected List<SharedCooldown> sharedCooldowns;
	protected Map<String, Map<EffectPosition, List<Runnable>>> callbacks;

	protected Multimap<String, VariableMod> variableModsCast;
	protected Multimap<String, VariableMod> variableModsCasted;
	protected Multimap<String, VariableMod> variableModsTarget;

	protected IntMap<UUID> chargesConsumed;

	protected ListMultimap<EffectPosition, SpellEffect> effects;

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
	protected boolean castWithLeftClick;
	protected boolean castWithRightClick;
	protected boolean usePreciseCooldowns;
	protected boolean ignoreGlobalCooldown;
	protected boolean requireCastItemOnCommand;

	protected ConfigData<Boolean> targetSelf;
	protected ConfigData<Boolean> alwaysActivate;
	protected ConfigData<Boolean> interruptOnMove;
	protected ConfigData<Boolean> interruptOnCast;
	protected ConfigData<Boolean> playFizzleSound;
	protected ConfigData<Boolean> interruptOnDamage;
	protected ConfigData<Boolean> interruptOnTeleport;
	protected ConfigData<Boolean> spellPowerAffectsRange;

	protected CastItem[] castItems;
	protected CastItem[] consumeCastItems;
	protected CastItem[] leftClickCastItems;
	protected CastItem[] rightClickCastItems;

	protected String[] aliases;

	protected String name;
	protected String permName;
	protected String description;
	protected String strNoTarget;
	protected String internalName;
	protected String profilingKey;
	protected String strCastTarget;
	protected String rechargeSound;
	protected String soundOnCooldown;
	protected String spellNameOnFail;
	protected String danceCastSequence;
	protected String soundMissingReagents;
	protected String spellNameOnInterrupt;

	protected String strCost;
	protected String strCastSelf;
	protected String strCantCast;
	protected String strCantBind;
	protected String strCastStart;
	protected String strCastOthers;
	protected String strCastCancelled;
	protected String strOnTeach;
	protected String strOnCooldown;
	protected String strWrongWorld;
	protected String strInterrupted;
	protected String strXpAutoLearned;
	protected String strWrongCastItem;
	protected String strModifierFailed;
	protected String strMissingReagents;
	protected String strCantCastByCommand;

	protected ModifierSet modifiers;
	protected ModifierSet targetModifiers;
	protected ModifierSet locationModifiers;

	private SpellFilter interruptFilter;

	protected Subspell spellOnFail;
	protected Subspell spellOnInterrupt;

	protected SpellReagents reagents;

	protected ItemStack spellIcon;

	protected DamageCause targetDamageCause;

	protected ValidTargetList validTargetList;

	protected ConfigData<Double> losRaySize;
	protected ConfigData<Boolean> losIgnorePassableBlocks;
	protected ConfigData<FluidCollisionMode> losFluidCollisionMode;

	protected long nextCastServer;

	protected double targetDamageAmount;

	protected ConfigData<Integer> range;
	protected ConfigData<Integer> minRange;

	protected int charges;
	protected ConfigData<Integer> castTime;
	protected ConfigData<Integer> experience;
	protected ConfigData<Integer> broadcastRange;

	protected ConfigData<Float> cooldown;
	protected float serverCooldown;

	protected final String internalKey;

	public Spell(MagicConfig config, String spellName) {
		this.config = config;
		this.internalName = spellName;

		internalKey = "spells." + internalName + '.';

		List<Class<?>> classes = new ArrayList<>();

		Class<?> currentClass = getClass();
		do {
			classes.add(currentClass);
			currentClass = currentClass.getSuperclass();
		} while (Spell.class.isAssignableFrom(currentClass));

		ConfigurationSection mainConfig = config.getMainConfig();
		for (Class<?> clazz : classes.reversed()) {
			ConfigurationSection defaults = config.getDefaults(clazz);
			if (defaults == null) continue;

			for (String key : defaults.getKeys(true)) {
				if (defaults.isConfigurationSection(key)) continue;

				String path = internalKey + key;
				if (!mainConfig.isSet(path)) mainConfig.set(path, defaults.get(key));
			}
		}

		callbacks = new HashMap<>();

		profilingKey = "Spell:" + getClass().getName().replace("com.nisovin.magicspells.spells.", "") + '-' + internalName;

		name = config.getString(internalKey + "name", internalName);
		debug = config.getBoolean(internalKey + "debug", false);
		helperSpell = config.getBoolean(internalKey + "helper-spell", false);
		alwaysGranted = config.getBoolean(internalKey + "always-granted", false);
		permName = config.getString(internalKey + "permission-name", internalName);
		incantations = config.getStringList(internalKey + "incantations", null);
		List<String> temp = config.getStringList(internalKey + "aliases", null);
		if (temp != null) aliases = temp.toArray(new String[0]);

		// General options
		description = config.getString(internalKey + "description", "");

		castItems = setupCastItems("cast-item", "cast-items", "cast item");
		leftClickCastItems = setupCastItems("left-click-cast-item", "left-click-cast-items", "left click cast item");
		rightClickCastItems = setupCastItems("right-click-cast-item", "right-click-cast-items", "right click cast item");
		consumeCastItems = setupCastItems("consume-cast-item", "consume-cast-items", "consume cast item");

		castWithLeftClick = config.getBoolean(internalKey + "cast-with-left-click", MagicSpells.canCastWithLeftClick());
		castWithRightClick = config.getBoolean(internalKey + "cast-with-right-click", MagicSpells.canCastWithRightClick());

		usePreciseCooldowns = config.getBoolean(internalKey + "use-precise-cooldowns", false);

		danceCastSequence = config.getString(internalKey + "dance-cast-sequence", null);
		requireCastItemOnCommand = config.getBoolean(internalKey + "require-cast-item-on-command", false);
		bindable = config.getBoolean(internalKey + "bindable", true);
		List<String> bindables = config.getStringList(internalKey + "bindable-items", null);
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
		String iconStr = config.getString(internalKey + "spell-icon", null);
		if (iconStr != null) {
			MagicItem magicItem = MagicItems.getMagicItemFromString(iconStr);
			if (magicItem != null) {
				spellIcon = magicItem.getItemStack();
				if (spellIcon != null && !spellIcon.getType().isAir()) {
					if (!magicItem.getMagicItemData().hasAttribute(MagicItemData.MagicItemAttribute.NAME)) {
						ItemMeta iconMeta = spellIcon.getItemMeta();
						iconMeta.displayName(Component.text(MagicSpells.getTextColor() + name));
						spellIcon.setItemMeta(iconMeta);
					}
				}
			}
		} else spellIcon = null;

		experience = getConfigDataInt("experience", 0);
		broadcastRange = getConfigDataInt("broadcast-range", MagicSpells.getBroadcastRange());

		// Cast time
		castTime = getConfigDataInt("cast-time", 0);
		interruptOnMove = getConfigDataBoolean("interrupt-on-move", true);
		interruptOnCast = getConfigDataBoolean("interrupt-on-cast", true);
		interruptOnDamage = getConfigDataBoolean("interrupt-on-damage", false);
		interruptOnTeleport = getConfigDataBoolean("interrupt-on-teleport", true);
		spellNameOnInterrupt = config.getString(internalKey + "spell-on-interrupt", null);

		// Targeting
		minRange = getConfigDataInt("min-range", 0);
		range = getConfigDataInt("range", 20);
		spellPowerAffectsRange = getConfigDataBoolean("spell-power-affects-range", false);
		obeyLos = config.getBoolean(internalKey + "obey-los", true);
		if (config.contains(internalKey + "can-target")) {
			if (config.isList(internalKey + "can-target"))
				validTargetList = new ValidTargetList(this, config.getStringList(internalKey + "can-target", null));
			else validTargetList = new ValidTargetList(this, config.getString(internalKey + "can-target", ""));
		} else {
			boolean targetPlayers = config.getBoolean(internalKey + "target-players", true);
			boolean targetNonPlayers = config.getBoolean(internalKey + "target-non-players", true);
			validTargetList = new ValidTargetList(targetPlayers, targetNonPlayers);
		}
		beneficial = config.getBoolean(internalKey + "beneficial", isBeneficialDefault());
		targetDamageCause = null;
		String causeStr = config.getString(internalKey + "target-damage-cause", null);
		if (causeStr != null) {
			for (DamageCause cause : DamageCause.values()) {
				if (!cause.name().equalsIgnoreCase(causeStr)) continue;
				targetDamageCause = cause;
				break;
			}
		}
		targetDamageAmount = config.getDouble(internalKey + "target-damage-amount", 0);
		losTransparentBlocks = MagicSpells.getTransparentBlocks();
		if (config.contains(internalKey + "los-transparent-blocks")) {
			losTransparentBlocks = Util.getMaterialList(config.getStringList(internalKey + "los-transparent-blocks", Collections.emptyList()), HashSet::new);
			losTransparentBlocks.add(Material.AIR);
			losTransparentBlocks.add(Material.CAVE_AIR);
			losTransparentBlocks.add(Material.VOID_AIR);
		}
		losRaySize = getConfigDataDouble("los-ray-size", MagicSpells.getLosRaySize());
		losIgnorePassableBlocks = getConfigDataBoolean("los-ignore-passable-blocks", MagicSpells.isIgnoringPassableBlocks());
		losFluidCollisionMode = getConfigDataEnum("los-fluid-collision-mode", FluidCollisionMode.class, MagicSpells.getFluidCollisionMode());
		targetSelf = getConfigDataBoolean("target-self", false);
		alwaysActivate = getConfigDataBoolean("always-activate", false);
		playFizzleSound = getConfigDataBoolean("play-fizzle-sound", false);
		strNoTarget = getConfigString("str-no-target", "");
		strCastTarget = getConfigString("str-cast-target", "");
		spellNameOnFail = getConfigString("spell-on-fail", "");

		// Cooldowns
		String cooldownString = config.getString(internalKey + "cooldown", null);
		String[] cooldownRange = cooldownString == null ? null : cooldownString.split("-", 2);
		if (cooldownRange != null && cooldownRange.length > 1) {
			try {
				float minCooldown = Float.parseFloat(cooldownRange[0]);
				float maxCooldown = Float.parseFloat(cooldownRange[1]);

				float min = Math.min(minCooldown, maxCooldown);
				float max = Math.max(minCooldown, maxCooldown);

				if (min >= 0 && max >= 0) {
					if (usePreciseCooldowns) cooldown = data -> min + (max - min) * random.nextFloat();
					else cooldown = data -> min + random.nextInt((int) max - (int) min + 1);
				}
			} catch (NumberFormatException ignored) {}
		}

		if (cooldown == null) cooldown = getConfigDataFloat("cooldown", 0);

		serverCooldown = (float) config.getDouble(internalKey + "server-cooldown", 0);
		ignoreGlobalCooldown = config.getBoolean(internalKey + "ignore-global-cooldown", false);
		charges = config.getInt(internalKey + "charges", 0);
		rechargeSound = config.getString(internalKey + "recharge-sound", "");
		nextCast = new HashMap<>();
		chargesConsumed = new IntMap<>();
		nextCastServer = 0;

		// Modifiers
		modifierStrings = config.getStringList(internalKey + "modifiers", null);
		targetModifierStrings = config.getStringList(internalKey + "target-modifiers", null);
		locationModifierStrings = config.getStringList(internalKey + "location-modifiers", null);

		// Variables
		varModsCast = config.getStringList(internalKey + "variable-mods-cast", null);
		varModsCasted = config.getStringList(internalKey + "variable-mods-casted", null);
		varModsTarget = config.getStringList(internalKey + "variable-mods-target", null);

		// Hierarchy options
		prerequisites = config.getStringList(internalKey + "prerequisites", null);
		replaces = config.getStringList(internalKey + "replaces", null);
		precludes = config.getStringList(internalKey + "precludes", null);
		worldRestrictions = config.getStringList(internalKey + "restrict-to-worlds", null);
		List<String> sXpGranted = config.getStringList(internalKey + "xp-granted", null);
		List<String> sXpRequired = config.getStringList(internalKey + "xp-required", null);
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

		soundOnCooldown = config.getString(internalKey + "sound-on-cooldown", MagicSpells.getCooldownSound());
		soundMissingReagents = config.getString(internalKey + "sound-missing-reagents", MagicSpells.getMissingReagentsSound());
		if (soundOnCooldown != null && soundOnCooldown.isEmpty()) soundOnCooldown = null;
		if (soundMissingReagents != null && soundMissingReagents.isEmpty()) soundMissingReagents = null;

		// Strings
		strCost = config.getString(internalKey + "str-cost", null);
		strCantCast = config.getString(internalKey + "str-cant-cast", MagicSpells.getCantCastMessage());
		strCantBind = config.getString(internalKey + "str-cant-bind", null);
		strCastSelf = config.getString(internalKey + "str-cast-self", null);
		strCastStart = config.getString(internalKey + "str-cast-start", null);
		strCastOthers = config.getString(internalKey + "str-cast-others", null);
		strCastCancelled = config.getString(internalKey + "str-cast-cancelled", null);
		strOnTeach = config.getString(internalKey + "str-on-teach", null);
		strOnCooldown = config.getString(internalKey + "str-on-cooldown", MagicSpells.getOnCooldownMessage());
		strWrongWorld = config.getString(internalKey + "str-wrong-world", MagicSpells.getWrongWorldMessage());
		strInterrupted = config.getString(internalKey + "str-interrupted", null);
		strXpAutoLearned = config.getString(internalKey + "str-xp-auto-learned", MagicSpells.getXpAutoLearnedMessage());
		strWrongCastItem = config.getString(internalKey + "str-wrong-cast-item", strCantCast);
		strModifierFailed = config.getString(internalKey + "str-modifier-failed", null);
		strMissingReagents = config.getString(internalKey + "str-missing-reagents", MagicSpells.getMissingReagentsMessage());
		strCantCastByCommand = config.getString(internalKey + "str-cant-cast-by-command", "You cannot cast this spell by commands.");
		if (strXpAutoLearned != null) strXpAutoLearned = strXpAutoLearned.replace("%s", name);

		tags = new HashSet<>(config.getStringList(internalKey + "tags", new ArrayList<>()));
		tags.add("spell-class:" + getClass().getCanonicalName());
		tags.add("spell-package:" + getClass().getPackage().getName());
		if (beneficial) tags.add("magicspells:beneficial");
		if (helperSpell) tags.add("magicspells:helper-spell");
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
		List<String> costList = config.getStringList(internalKey + option, null);
		if (costList == null || costList.isEmpty()) return null;

		SpellReagents reagents = new SpellReagents();
		String[] data;

		for (String costVal : costList) {
			try {
				// Parse cost data
				data = costVal.trim().split(" ", 2);

				switch (data[0].toLowerCase()) {
					case "health" -> {
						reagents.setHealth(Double.parseDouble(data[1]));
						continue;
					}
					case "mana" -> {
						reagents.setMana(Integer.parseInt(data[1]));
						continue;
					}
					case "hunger" -> {
						reagents.setHunger(Integer.parseInt(data[1]));
						continue;
					}
					case "experience" -> {
						reagents.setExperience(Integer.parseInt(data[1]));
						continue;
					}
					case "levels" -> {
						reagents.setLevels(Integer.parseInt(data[1]));
						continue;
					}
					case "durability" -> {
						reagents.setDurability(Integer.parseInt(data[1]));
						continue;
					}
					case "money" -> {
						reagents.setMoney(Float.parseFloat(data[1]));
						continue;
					}
					case "variable" -> {
						String[] variableData = data[1].split(" ", 2);
						reagents.addVariable(variableData[0], Double.parseDouble(variableData[1]));
						continue;
					}
				}

				int i = costVal.lastIndexOf(' ');

				int amount = 1;
				if (i != -1) {
					try {
						amount = Integer.parseInt(costVal.substring(i + 1));
					} catch (NumberFormatException e) {
						i = -1;
					}
				}

				MagicItemData itemData = MagicItems.getMagicItemDataFromString(i != -1 ? costVal.substring(0, i) : costVal);
				if (itemData == null) {
					MagicSpells.error("Failed to process cost value for " + internalName + " spell: " + costVal);
					continue;
				}

				reagents.addItem(new SpellReagents.ReagentItem(itemData, amount));
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
		effectTrackerSet = new HashSet<>();
		asyncEffectTrackerSet = new HashSet<>();

		if (!config.contains(internalKey + "effects")) return;

		effects = MultimapBuilder.enumKeys(EffectPosition.class).arrayListValues().build();

		Object effectsObject = config.get(internalKey + "effects");
		switch (effectsObject) {
			case ConfigurationSection effectSection -> {
				for (String key : effectSection.getKeys(false)) {
					ConfigurationSection section = effectSection.getConfigurationSection(key);
					if (section == null) {
						MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not contain a configuration section.");
						continue;
					}

					initializeSpellEffect(section, key);
				}
			}
			case List<?> effectList -> {
				for (int i = 0; i < effectList.size(); i++) {
					String key = "#" + i;
					if (!(effectList.get(i) instanceof Map<?, ?> map)) {
						MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not contain a configuration section.");
						continue;
					}

					initializeSpellEffect(ConfigReaderUtil.mapToSection(map), key);
				}
			}
			default -> MagicSpells.error("Invalid 'effects' section on spell '" + internalName + "'.");
		}
	}

	protected void initializeSpellEffect(ConfigurationSection section, String key) {
		String positionName = section.getString("position", "");
		if (positionName.isEmpty()) {
			MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not contain a 'position' value.");
			return;
		}

		EffectPosition position = EffectPosition.getPositionFromString(positionName);
		if (position == null) {
			MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not have a valid 'position' defined: " + positionName);
			return;
		}

		String effectType = section.getString("effect", "");
		if (effectType.isEmpty()) {
			MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not contain an 'effect' value.");
			return;
		}

		SpellEffect effect = MagicSpells.getSpellEffectManager().getSpellEffectByName(effectType);
		if (effect == null) {
			MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not have a valid 'effect' defined: " + effectType);
			return;
		}

		effect.loadFromConfiguration(section);
		effects.put(position, effect);
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
		List<?> rawSharedCooldowns = config.getList(internalKey + "shared-cooldowns", null);
		if (rawSharedCooldowns != null) {
			sharedCooldowns = new ArrayList<>();

			for (Object object : rawSharedCooldowns) {
				if (object instanceof String s) {
					String[] data = s.split(" ");
					if (data.length != 2) {
						MagicSpells.error("Invalid shared cooldown '" + s + "' on spell '" + internalName + "'.");
						continue;
					}

					Spell spell = MagicSpells.getSpellByInternalName(data[0]);
					if (spell == null) {
						MagicSpells.error("Invalid spell '" + data[0] + "' in shared cooldown '" + s + "' on spell '" + internalName + "'.");
						continue;
					}

					float cooldown;
					try {
						cooldown = Float.parseFloat(data[1]);
					} catch (NumberFormatException e) {
						MagicSpells.error("Invalid cooldown '" + data[1] + "' in shared cooldown '" + s + "' on spell '" + internalName + "'.");
						continue;
					}

					sharedCooldowns.add(new SharedCooldown(Set.of(spell), spellData -> cooldown));
					continue;
				}

				if (object instanceof Map<?, ?> map) {
					ConfigurationSection section = ConfigReaderUtil.mapToSection(map);

					if (!section.isString("filter") && !section.isConfigurationSection("filter")) {
						MagicSpells.error("No 'filter' specified in shared cooldown on spell '" + internalName + "'.");
						continue;
					}

					SpellFilter filter = SpellFilter.fromConfig(section, "filter");

					ConfigData<Float> cooldown = ConfigDataUtil.getFloat(section, "cooldown");
					if (cooldown.isNull()) {
						MagicSpells.error("Invalid or no 'cooldown' specified in shared cooldown on spell '" + internalName + "'.");
						continue;
					}

					sharedCooldowns.add(new SharedCooldown(filter.getMatchingSpells(), cooldown));
					continue;
				}

				MagicSpells.error("Invalid shared cooldown '" + object + "' on spell '" + internalName + "'.");
			}
		}

		// Register events
		registerEvents();

		// Other processing
		String error = "Spell '" + internalName + "' has an invalid '%s' defined!";
		spellOnFail = initSubspell(spellNameOnFail,
				error.formatted("spell-on-fail"),
				true);
		spellOnInterrupt = initSubspell(spellNameOnInterrupt,
				error.formatted("spell-on-interrupt"),
				true);

		interruptFilter = getConfigSpellFilter("interrupt-filter");
	}

	protected boolean configKeyExists(String key) {
		return config.contains(internalKey + key);
	}

	/**
	 * Access an integer config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected int getConfigInt(String key, int def) {
		return config.getInt(internalKey + key, def);
	}

	/**
	 * Access a long config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected long getConfigLong(String key, long def) {
		return config.getLong(internalKey + key, def);
	}

	/**
	 * Access a boolean config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected boolean getConfigBoolean(String key, boolean def) {
		return config.getBoolean(internalKey + key, def);
	}

	/**
	 * Access a String config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected String getConfigString(String key, String def) {
		return config.getString(internalKey + key, def);
	}

	/**
	 * Access a Vector config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected Vector getConfigVector(String key, String def) {
		String[] vecStrings = getConfigString(key, def).split(",");
		return new Vector(Double.parseDouble(vecStrings[0]), Double.parseDouble(vecStrings[1]), Double.parseDouble(vecStrings[2]));
	}

	/**
	 * Access a float config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected float getConfigFloat(String key, float def) {
		return (float) config.getDouble(internalKey + key, def);
	}

	/**
	 * Access a double config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected double getConfigDouble(String key, double def) {
		return config.getDouble(internalKey + key, def);
	}

	protected List<?> getConfigList(String key, List<?> def) {
		return config.getList(internalKey + key, def);
	}

	protected List<Integer> getConfigIntList(String key, List<Integer> def) {
		return config.getIntList(internalKey + key, def);
	}

	protected List<String> getConfigStringList(String key, List<String> def) {
		return config.getStringList(internalKey + key, def);
	}

	protected Set<String> getConfigKeys(String key) {
		return config.getKeys(internalKey + key);
	}

	protected ConfigurationSection getConfigSection(String key) {
		return config.getSection(internalKey + key);
	}

	protected ConfigData<Boolean> getConfigDataBoolean(String key, boolean def) {
		return ConfigDataUtil.getBoolean(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<Boolean> getConfigDataBoolean(String key, ConfigData<Boolean> def) {
		return ConfigDataUtil.getBoolean(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<Integer> getConfigDataInt(String key, int def) {
		return ConfigDataUtil.getInteger(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<Integer> getConfigDataInt(String key, ConfigData<Integer> def) {
		return ConfigDataUtil.getInteger(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<Double> getConfigDataDouble(String key, double def) {
		return ConfigDataUtil.getDouble(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<Double> getConfigDataDouble(String key, ConfigData<Double> def) {
		return ConfigDataUtil.getDouble(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<Float> getConfigDataFloat(String key, float def) {
		return ConfigDataUtil.getFloat(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<Float> getConfigDataFloat(String key, ConfigData<Float> def) {
		return ConfigDataUtil.getFloat(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<Long> getConfigDataLong(String key, long def) {
		return ConfigDataUtil.getLong(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<Long> getConfigDataLong(String key, ConfigData<Long> def) {
		return ConfigDataUtil.getLong(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<String> getConfigDataString(String key, String def) {
		return ConfigDataUtil.getString(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<Component> getConfigDataComponent(String key, Component def) {
		return ConfigDataUtil.getComponent(config.getMainConfig(), internalKey + key, def);
	}

	protected <T extends Enum<T>> ConfigData<T> getConfigDataEnum(String key, Class<T> type, T def) {
		return ConfigDataUtil.getEnum(config.getMainConfig(), internalKey + key, type, def);
	}

	protected ConfigData<Vector> getConfigDataVector(String key, Vector def) {
		return ConfigDataUtil.getVector(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<Color> getConfigDataColor(String key, Color def) {
		return ConfigDataUtil.getColor(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<Angle> getConfigDataAngle(String key, Angle def) {
		return ConfigDataUtil.getAngle(config.getMainConfig(), internalKey + key, def);
	}

	public ConfigData<Material> getConfigDataMaterial(String key, Material def) {
		return ConfigDataUtil.getMaterial(config.getMainConfig(), internalKey + key, def);
	}

	public ConfigData<TargetBooleanState> getConfigDataTargetBooleanState(String key, TargetBooleanState def) {
		return ConfigDataUtil.getTargetBooleanState(config.getMainConfig(), internalKey + key, def);
	}

	public ConfigData<EntityType> getConfigDataEntityType(String key, EntityType def) {
		return ConfigDataUtil.getEntityType(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<BlockData> getConfigDataBlockData(String key, BlockData def) {
		return ConfigDataUtil.getBlockData(config.getMainConfig(), internalKey + key, def);
	}

	protected <T extends Keyed> ConfigData<T> getConfigDataRegistryEntry(@NotNull String key, @NotNull RegistryKey<T> registryKey, @Nullable T def) {
		return ConfigDataUtil.getRegistryEntry(config.getMainConfig(), internalKey + key, RegistryAccess.registryAccess().getRegistry(registryKey), def);
	}

	protected <T extends Keyed> ConfigData<T> getConfigDataRegistryEntry(@NotNull String key, @NotNull Registry<T> registry, @Nullable T def) {
		return ConfigDataUtil.getRegistryEntry(config.getMainConfig(), internalKey + key, registry, def);
	}

	protected ConfigData<NamedTextColor> getConfigDataNamedTextColor(String key, NamedTextColor def) {
		return ConfigDataUtil.getNamedTextColor(config.getMainConfig(), internalKey + key, def);
	}

	protected ConfigData<NamespacedKey> getConfigDataNamespacedKey(String key, NamespacedKey def) {
		return ConfigDataUtil.getNamespacedKey(config.getMainConfig(), internalKey + key, def);
	}

	/**
	 * @param key Path for the string or section format SpellFilter to be read from.
	 */
	protected SpellFilter getConfigSpellFilter(String key) {
		return SpellFilter.fromConfig(config.getMainConfig(), internalKey + key);
	}

	/**
	 * Gets the section format SpellFilter under the base path of the spell config.
	 */
	protected SpellFilter getConfigSpellFilter() {
		return SpellFilter.fromSection(config.getMainConfig(), internalKey);
	}

	protected <T extends Keyed> Predicate<T> getConfigRegistryEntryPredicate(String path, RegistryKey<T> registryKey) {
		if (config.isList(internalKey + path)) {
			Set<Key> keys = getConfigRegistryKeys(path, registryKey);
			return entry -> keys.contains(entry.key());
		}

		return RegistryEntryPredicate.fromString(registryKey, config.getString(internalKey + path, null));
	}

	@SuppressWarnings("UnstableApiUsage")
	protected <T extends Keyed> Set<Key> getConfigRegistryKeys(String path, RegistryKey<T> registryKey) {
		List<String> keyStrings = config.getStringList(internalKey + path, null);
		if (keyStrings == null) return null;

		Set<Key> keys = new HashSet<>();

		Registry<T> registry = RegistryAccess.registryAccess().getRegistry(registryKey);
		for (String keyString : keyStrings) {
			if (!keyString.startsWith("#")) {
				NamespacedKey key = NamespacedKey.fromString(keyString);
				if (key == null || registry.get(key) == null) {
					MagicSpells.error("Invalid registry entry '" + keyString + "' found on spell '" + internalName + "'.");
					continue;
				}

				keys.add(key);
				continue;
			}

			NamespacedKey key = NamespacedKey.fromString(keyString.substring(1));
			if (key == null) {
				MagicSpells.error("Invalid tag '" + keyString + "' found on spell '" + internalName + "'.");
				continue;
			}

			TagKey<T> tagKey = TagKey.create(registryKey, key);
			if (!registry.hasTag(tagKey)) {
				MagicSpells.error("Invalid tag '" + keyString + "' found on spell '" + internalName + "'.");
				continue;
			}

			Tag<@NotNull T> tag = registry.getTag(tagKey);
			tag.values().forEach(typedKey -> keys.add(typedKey.key()));
		}

		return keys;
	}

	protected boolean isConfigString(String key) {
		return config.isString(internalKey + key);
	}

	protected boolean isConfigSection(String key) {
		return config.isSection(internalKey + key);
	}

	@Deprecated
	public final SpellCastResult cast(LivingEntity livingEntity) {
		return hardCast(new SpellData(livingEntity));
	}

	@Deprecated
	public final SpellCastResult cast(LivingEntity livingEntity, String[] args) {
		return hardCast(new SpellData(livingEntity, 1f, args));
	}

	@Deprecated
	public final SpellCastResult cast(LivingEntity livingEntity, float power, String[] args) {
		return hardCast(new SpellData(livingEntity, power, args));
	}

	// DEBUG INFO: level 2, spell cast state
	// DEBUG INFO: level 2, spell canceled
	// DEBUG INFO: level 2, spell cast state changed
	@Deprecated
	protected SpellCastEvent preCast(LivingEntity livingEntity, float power, String[] args) {
		return preCast(new SpellData(livingEntity, power, args));
	}

	// DEBUG INFO: level 3, power #
	// DEBUG INFO: level 3, cooldown #
	// DEBUG INFO: level 3, args argsvalue
	@Deprecated
	PostCastAction handleCast(SpellCastEvent spellCast) {
		return onCast(spellCast).action();
	}

	/**
	 * This method is called when a player casts a spell, either by command, with a wand item, or otherwise.
	 *
	 * @param caster the living entity casting the spell
	 * @param state  the state of the spell cast (normal, on cooldown, missing reagents, etc.)
	 * @param power  the power multiplier the spell should be cast with (1.0 is normal)
	 * @param args   the spell arguments, if cast by command
	 * @return the action to take after the spell is processed
	 */
	@Deprecated
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		return cast(state, new SpellData(caster, power, args)).action();
	}

	protected SpellCastState getCastState(LivingEntity caster) {
		if (caster instanceof Player player && !MagicSpells.getSpellbook(player).canCast(this))
			return SpellCastState.CANT_CAST;
		if (worldRestrictions != null && !worldRestrictions.contains(caster.getWorld().getName()))
			return SpellCastState.WRONG_WORLD;
		if (MagicSpells.getNoMagicZoneManager() != null && MagicSpells.getNoMagicZoneManager().willFizzle(caster, this))
			return SpellCastState.NO_MAGIC_ZONE;
		if (onCooldown(caster)) return SpellCastState.ON_COOLDOWN;
		if (!hasReagents(caster)) return SpellCastState.MISSING_REAGENTS;
		return SpellCastState.NORMAL;
	}

	@NotNull
	public SpellCastResult hardCast(@NotNull SpellData data) {
		data = data.noTargeting();

		SpellCastEvent castEvent = preCast(data);
		if (castEvent.isCancelled()) {
			postCast(castEvent, PostCastAction.HANDLE_NORMALLY);
			return new SpellCastResult(castEvent.getSpellCastState(), PostCastAction.HANDLE_NORMALLY, data);
		}

		SpellCastState state = castEvent.getSpellCastState();
		int castTime = castEvent.getCastTime();
		data = castEvent.getSpellData();

		if (castTime <= 0 || state != SpellCastState.NORMAL) {
			CastResult result = onCast(castEvent);
			return new SpellCastResult(state, result.action(), result.data());
		}

		if (!preCastTimeCheck(data.caster(), data.args())) {
			postCast(castEvent, PostCastAction.ALREADY_HANDLED);
			return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.ALREADY_HANDLED, data);
		}

		if (MagicSpells.useExpBarAsCastTimeBar()) new DelayedSpellCastWithBar(castEvent);
		else new DelayedSpellCast(castEvent);

		sendMessage(strCastStart, data.caster(), data);
		playSpellEffects(EffectPosition.START_CAST, data.caster(), data);

		return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.DELAYED, data);
	}

	@NotNull
	public SpellCastEvent preCast(@NotNull SpellData data) {
		SpellCastState state = getCastState(data.caster());
		debug(2, "    Spell cast state: " + state);

		SpellCastEvent castEvent = new SpellCastEvent(this, state, data, cooldown.get(data), reagents.clone(), castTime.get(data));
		if (!castEvent.callEvent()) {
			debug(2, "    Spell cancelled");
			return castEvent;
		}

		if (castEvent.haveReagentsChanged()) {
			boolean hasReagents = hasReagents(data.caster(), castEvent.getReagents());
			if (!hasReagents && state != SpellCastState.MISSING_REAGENTS) {
				castEvent.setSpellCastState(SpellCastState.MISSING_REAGENTS);
				debug(2, "    Spell cast state changed: " + state);
			} else if (hasReagents && state == SpellCastState.MISSING_REAGENTS) {
				castEvent.setSpellCastState(state = SpellCastState.NORMAL);
				debug(2, "    Spell cast state changed: " + state);
			}
		}

		if (castEvent.hasSpellCastStateChanged()) debug(2, "    Spell cast state changed: " + state);
		if (Perm.NO_CAST_TIME.has(data.caster())) castEvent.setCastTime(0);

		return castEvent;
	}

	@NotNull
	public CastResult onCast(@NotNull SpellCastEvent castEvent) {
		SpellData data = castEvent.getSpellData();
		long start = System.nanoTime();

		debug(3, "    Power: " + data.power());
		debug(3, "    Cooldown: " + castEvent.getCooldown());
		if (MagicSpells.isDebug() && data.hasArgs()) debug(3, "    Args: {" + Util.arrayJoin(data.args(), ',') + '}');

		CastResult result = cast(castEvent.getSpellCastState(), data);
		if (MagicSpells.hasProfilingEnabled()) {
			long total = MagicSpells.getProfilingTotalTime().getOrDefault(profilingKey, 0L);
			total += System.nanoTime() - start;
			MagicSpells.getProfilingTotalTime().put(profilingKey, total);

			int runs = MagicSpells.getProfilingRuns().getOrDefault(profilingKey, 0) + 1;
			MagicSpells.getProfilingRuns().put(profilingKey, runs);
		}

		postCast(castEvent, result);
		return result;
	}

	public void postCast(SpellCastEvent spellCast, PostCastAction action) {
		postCast(spellCast, action, spellCast.getSpellData());
	}

	public void postCast(@NotNull SpellCastEvent castEvent, @NotNull CastResult result) {
		postCast(castEvent, result.action(), result.data());
	}

	public void postCast(@NotNull SpellCastEvent castEvent, @NotNull PostCastAction action, @NotNull SpellData data) {
		debug(3, "    Post-cast action: " + action);

		if (action != PostCastAction.ALREADY_HANDLED) {
			LivingEntity caster = data.caster();

			switch (castEvent.getSpellCastState()) {
				case NORMAL -> {
					if (action.setCooldown()) setCooldown(caster, castEvent.getCooldown(), castEvent.getSpellData(), true);
					if (action.chargeReagents()) removeReagents(caster, castEvent.getReagents());
					if (action.sendMessages()) sendMessages(data);

					int experience = this.experience.get(data);
					if (experience > 0 && caster instanceof Player player) player.giveExp(experience);
				}
				case ON_COOLDOWN -> {
					MagicSpells.sendMessage(strOnCooldown, caster, data,
						"%c", String.valueOf(Math.round(getCooldown(caster))),
						"%s", getName());
					playSpellEffects(EffectPosition.COOLDOWN, caster, data);

					if (soundOnCooldown != null && caster instanceof Player player)
						player.playSound(caster.getLocation(), soundOnCooldown, 1F, 1F);
				}
				case MISSING_REAGENTS -> {
					MagicSpells.sendMessage(strMissingReagents, caster, data);
					playSpellEffects(EffectPosition.MISSING_REAGENTS, caster, data);

					if (MagicSpells.showStrCostOnMissingReagents() && strCost != null && !strCost.isEmpty())
						MagicSpells.sendMessage("    (" + strCost + ')', caster, data);

					if (soundMissingReagents != null && caster instanceof Player player)
						player.playSound(caster.getLocation(), soundMissingReagents, 1F, 1F);
				}
				case CANT_CAST -> MagicSpells.sendMessage(strCantCast, caster, data);
				case NO_MAGIC_ZONE -> MagicSpells.getNoMagicZoneManager().sendNoMagicMessage(this, data);
				case WRONG_WORLD -> MagicSpells.sendMessage(strWrongWorld, caster, data);
				case CANCELLED -> MagicSpells.sendMessage(strCastCancelled, caster, data);
			}
		}

		new SpellCastedEvent(castEvent, action, data).callEvent();
	}

	public CastResult cast(SpellCastState state, SpellData data) {
		return state == SpellCastState.NORMAL ? cast(data) : new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public CastResult cast(SpellData data) {
		return new CastResult(castSpell(data.caster(), SpellCastState.NORMAL, data.power(), data.args()), data);
	}

	@Deprecated
	public void sendMessages(LivingEntity caster, String[] args) {
		sendMessages(new SpellData(caster, 1f, args));
	}

	public void sendMessages(SpellData data, String... replacements) {
		sendMessage(strCastSelf, data.caster(), data, replacements);
		sendMessage(strCastTarget, data.target(), data, replacements);
		sendMessageNear(strCastOthers, data, broadcastRange.get(data), replacements);
	}

	protected boolean preCastTimeCheck(LivingEntity livingEntity, String[] args) {
		return true;
	}

	public List<String> tabComplete(CommandSender sender, String[] args) {
		return null;
	}

	/**
	 * This method is called when the spell is cast from the console.
	 *
	 * @param sender the console sender.
	 * @param args   the command arguments
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
		if (item == null && castItems.length == 1 && castItems[0].getType().isAir()) return true;
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

	public ConfigData<Float> getCooldown() {
		return cooldown;
	}

	/**
	 * Check whether this spell is currently on cooldown for the specified player
	 *
	 * @param livingEntity The living entity to check
	 * @return whether the spell is on cooldown
	 */
	public boolean onCooldown(LivingEntity livingEntity) {
		if (Perm.NO_COOLDOWN.has(livingEntity)) return false;
		if (serverCooldown > 0 && nextCastServer > System.currentTimeMillis()) return true;

		Long next = nextCast.get(livingEntity.getUniqueId());
		return next != null && next > System.currentTimeMillis();
	}

	/**
	 * Get how many seconds remain on the cooldown of this spell for the specified player
	 *
	 * @param livingEntity The living entity to check
	 * @return The number of seconds remaining in the cooldown
	 */
	public float getCooldown(LivingEntity livingEntity) {
		float cd = 0;

		Long next = nextCast.get(livingEntity.getUniqueId());
		if (next != null) {
			float c = (next - System.currentTimeMillis()) / ((float) TimeUtil.MILLISECONDS_PER_SECOND);
			cd = c > 0 ? c : 0;
		}

		if (serverCooldown > 0 && nextCastServer > System.currentTimeMillis()) {
			float c = (nextCastServer - System.currentTimeMillis()) / ((float) TimeUtil.MILLISECONDS_PER_SECOND);
			if (c > cd) cd = c;
		}

		return cd;
	}

	/**
	 * Begins the cooldown for the spell for the specified player
	 *
	 * @param livingEntity The living entity to set the cooldown for
	 * @param cooldown The cooldown to use
	 */
	public void setCooldown(LivingEntity livingEntity, float cooldown) {
		setCooldown(livingEntity, cooldown, SpellData.NULL, true);
	}

	/**
	 * Begins the cooldown for the spell for the specified player
	 *
	 * @param livingEntity The living entity to set the cooldown for
	 * @param cooldown The cooldown to use
	 * @param activateSharedCooldowns Whether shared cooldowns should be activated
	 */
	public void setCooldown(LivingEntity livingEntity, float cooldown, boolean activateSharedCooldowns) {
		setCooldown(livingEntity, cooldown, SpellData.NULL, activateSharedCooldowns);
	}

	/**
	 * Begins the cooldown for the spell for the specified player
	 *
	 * @param livingEntity The living entity to set the cooldown for
	 * @param data The associated spell data
	 */
	public void setCooldown(LivingEntity livingEntity, SpellData data) {
		setCooldown(livingEntity, cooldown.get(data), data, true);
	}

	/**
	 * Begins the cooldown for the spell for the specified player
	 *
	 * @param livingEntity The living entity to set the cooldown for
	 * @param cooldown The cooldown to use
	 * @param data The associated spell data
	 * @param activateSharedCooldowns Whether shared cooldowns should be activated
	 */
	public void setCooldown(LivingEntity livingEntity, float cooldown, SpellData data, boolean activateSharedCooldowns) {
		final UUID uuid = livingEntity.getUniqueId();

		if (cooldown > 0) {
			if (charges > 0) {
				chargesConsumed.increment(uuid);
				MagicSpells.scheduleDelayedTask(() -> {
					chargesConsumed.decrement(uuid);
					playSpellEffects(EffectPosition.CHARGE_USE, livingEntity, new SpellData(livingEntity));
					if (rechargeSound == null) return;
					if (rechargeSound.isEmpty()) return;
					if (livingEntity instanceof Player player)
						player.playSound(livingEntity.getLocation(), rechargeSound, 1.0F, 1.0F);
				}, Math.round(TimeUtil.TICKS_PER_SECOND * cooldown));
			}
			if (charges <= 0 || chargesConsumed.get(uuid) >= charges) {
				nextCast.put(uuid, System.currentTimeMillis() + (long) (cooldown * TimeUtil.MILLISECONDS_PER_SECOND));
			}

		} else {
			nextCast.remove(uuid);
			chargesConsumed.remove(uuid);
		}

		if (serverCooldown > 0)
			nextCastServer = System.currentTimeMillis() + (long) (serverCooldown * TimeUtil.MILLISECONDS_PER_SECOND);

		if (activateSharedCooldowns && sharedCooldowns != null) {
			for (SharedCooldown sharedCooldown : sharedCooldowns) {
				Float cd = sharedCooldown.cooldown().get(data);
				if (cd == null) continue;

				for (Spell spell : sharedCooldown.spells)
					spell.setCooldown(livingEntity, cd, data, false);
			}
		}
	}

	/**
	 * Checks if a player has the reagents required to cast this spell
	 *
	 * @param livingEntity the living entity to check
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(LivingEntity livingEntity) {
		return hasReagents(livingEntity, reagents);
	}

	// FIXME this doesn't seem strictly tied to Spell logic, could probably be moved

	/**
	 * Checks if a player has the reagents required to cast this spell
	 *
	 * @param livingEntity the living entity to check
	 * @param reagents     the reagents to check for
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(LivingEntity livingEntity, SpellReagents reagents) {
		if (reagents == null) return true;
		return SpellUtil.hasReagents(livingEntity, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels(), reagents.getDurability(), reagents.getMoney(), reagents.getVariables());
	}

	/**
	 * Removes the reagent cost of this spell from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 *
	 * @param livingEntity the living entity to remove reagents from
	 */
	protected void removeReagents(LivingEntity livingEntity) {
		removeReagents(livingEntity, reagents);
	}

	// TODO can this safely be made varargs?
	/**
	 * Removes the specified reagents from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 *
	 * @param livingEntity the living entity to remove the reagents from
	 * @param reagents     the inventory item reagents to remove
	 */
	protected void removeReagents(LivingEntity livingEntity, SpellReagents.ReagentItem[] reagents) {
		SpellUtil.removeReagents(livingEntity, reagents, 0, 0, 0, 0, 0, 0, 0, null);
	}

	protected void removeReagents(LivingEntity livingEntity, SpellReagents reagents) {
		SpellUtil.removeReagents(livingEntity, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels(), reagents.getDurability(), reagents.getMoney(), reagents.getVariables());
	}

	public Map<EffectPosition, List<SpellEffect>> getEffects() {
		return effects != null ? Multimaps.asMap(effects) : null;
	}

	protected int getRange(float power) {
		return getRange(new SpellData(null, power, null));
	}

	protected int getRange(SpellData data) {
		int range = this.range.get(data);
		return spellPowerAffectsRange.get(data) ? Math.round(range * data.power()) : range;
	}

	public int getCharges() {
		return charges;
	}

	/**
	 * Get how many charges the specified living entity has consumed.
	 *
	 * @param livingEntity The living entity to check
	 * @return The number of charges consumed
	 */
	public int getCharges(LivingEntity livingEntity) {
		return chargesConsumed.get(livingEntity.getUniqueId());
	}

	/**
	 * Gets the player a player is currently looking at, ignoring other living entities
	 *
	 * @param livingEntity the living entity to get the target for
	 * @return the targeted Player, or null if none was found
	 */
	protected TargetInfo<Player> getTargetedPlayer(LivingEntity livingEntity, float power) {
		return getTargetedPlayer(new SpellData(livingEntity, power, null));
	}

	/**
	 * Gets the player a player is currently looking at, ignoring other living entities
	 *
	 * @param data spell data
	 * @return the targeted Player, or null if none was found
	 */
	protected TargetInfo<Player> getTargetedPlayer(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data, true, e -> e instanceof Player);
		return new TargetInfo<>((Player) info.target(), info.spellData(), info.cancelled());
	}

	protected TargetInfo<Player> getTargetPlayer(LivingEntity caster, float power) {
		return getTargetedPlayer(caster, power);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power) {
		return getTargetedEntity(new SpellData(caster, power, null), false, null);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power, ValidTargetChecker checker) {
		return getTargetedEntity(new SpellData(caster, power, null), false, checker);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power, boolean forceTargetPlayers, ValidTargetChecker checker) {
		return getTargetedEntity(new SpellData(caster, power, null), forceTargetPlayers, checker);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(SpellData data) {
		return getTargetedEntity(data, false, null);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(SpellData data, ValidTargetChecker checker) {
		return getTargetedEntity(data, false, checker);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(SpellData data, boolean forceTargetPlayers, ValidTargetChecker checker) {
		LivingEntity caster = data.caster();

		if (targetSelf.get(data) || validTargetList.canTargetSelf()) {
			if (checker != null && !checker.isValidTarget(caster)) return new TargetInfo<>(null, data, false);

			SpellTargetEvent event = new SpellTargetEvent(this, data, caster);
			return new TargetInfo<>(event.callEvent() ? event.getTarget() : null, event.getSpellData(), event.isCastCancelled());
		}

		World world = caster.getWorld();

		boolean targetPlayers = forceTargetPlayers || validTargetList.canTargetPlayers();
		if (targetPlayers && MagicSpells.checkWorldPvpFlag() && caster instanceof Player && !isBeneficial() && Boolean.FALSE.equals(world.getGameRuleValue(GameRule.PVP))) {
			if (forceTargetPlayers) return new TargetInfo<>(null, data, false);
			targetPlayers = false;
		}

		Location startLocation = caster.getEyeLocation();
		Vector direction = startLocation.getDirection();
		Vector start = startLocation.toVector();

		double range = getRange(data), raySize = losRaySize.get(data);
		if (obeyLos) {
			RayTraceResult blockHit = world.rayTraceBlocks(startLocation, direction, range, losFluidCollisionMode.get(data), losIgnorePassableBlocks.get(data), block -> !losTransparentBlocks.contains(block.getType()));
			if (blockHit != null) range = blockHit.getHitPosition().distance(start);
		}

		Collection<Entity> nearbyEntities = world.getNearbyEntities(BoundingBox.of(start, start).expand(direction, range).expand(raySize));
		List<LivingEntity> potentialTargets = new ArrayList<>();

		for (Entity entity : nearbyEntities) {
			if (!(entity instanceof LivingEntity target)) continue;
			if (!validTargetList.canTarget(caster, target, targetPlayers)) continue;
			if (checker != null && !checker.isValidTarget(target)) continue;

			potentialTargets.add(target);
		}

		potentialTargets.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(startLocation)));

		int minRangeSq = this.minRange.get(data);
		minRangeSq *= minRangeSq;

		for (LivingEntity target : potentialTargets) {
			Location targetLocation = target.getLocation();
			if (targetLocation.distanceSquared(startLocation) < minRangeSq) continue;

			BoundingBox boundingBox = target.getBoundingBox().expand(raySize);
			if (boundingBox.rayTrace(start, direction, range) == null) continue;

			if (target instanceof ComplexLivingEntity complexEntity) {
				boolean collides = false;

				for (Entity part : complexEntity.getParts()) {
					if (part.getBoundingBox().expand(raySize).rayTrace(start, direction, range) != null) {
						collides = true;
						break;
					}
				}

				if (!collides) continue;
			}

			if (MagicSpells.getNoMagicZoneManager() != null && MagicSpells.getNoMagicZoneManager().willFizzle(targetLocation, this))
				continue;

			if (MagicSpells.checkScoreboardTeams()) {
				Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

				Team casterTeam = scoreboard.getEntityTeam(caster);
				Team targetTeam = scoreboard.getEntityTeam(target);

				if (casterTeam != null && targetTeam != null) {
					if (casterTeam.equals(targetTeam) ? !casterTeam.allowFriendlyFire() && !isBeneficial() : isBeneficial())
						continue;
				}
			}

			SpellTargetEvent targetEvent = new SpellTargetEvent(this, data, target);
			targetEvent.callEvent();

			if (targetEvent.isCastCancelled()) return new TargetInfo<>(null, targetEvent.getSpellData(), true);
			else if (targetEvent.isCancelled()) continue;

			target = targetEvent.getTarget();

			if (targetDamageCause != null && checkFakeDamageEvent(caster, target, targetDamageCause, targetDamageAmount))
				continue;

			return new TargetInfo<>(target, targetEvent.getSpellData(), false);
		}

		return new TargetInfo<>(null, data, false);
	}

	protected boolean checkFakeDamageEvent(@NotNull LivingEntity caster, @NotNull LivingEntity target) {
		return !createFakeDamageEvent(caster, target, DamageCause.ENTITY_ATTACK, 1).callEvent();
	}

	protected boolean checkFakeDamageEvent(@NotNull LivingEntity caster, @NotNull LivingEntity target, @NotNull DamageCause cause, double damage) {
		return !createFakeDamageEvent(caster, target, cause, damage).callEvent();
	}

	@SuppressWarnings({"UnstableApiUsage", "deprecation"})
	protected EntityDamageByEntityEvent createFakeDamageEvent(@NotNull LivingEntity caster, @NotNull LivingEntity target, @NotNull DamageCause cause, double damage) {
		DamageType damageType = caster instanceof Player ? DamageType.PLAYER_ATTACK : DamageType.MOB_ATTACK;
		DamageSource source = DamageSource.builder(damageType).withDirectEntity(caster).build();

		return new EntityDamageByEntityEvent(
			caster, target, cause, source,
			new HashMap<>(Map.of(EntityDamageEvent.DamageModifier.BASE, damage)),
			new HashMap<>(Map.of(EntityDamageEvent.DamageModifier.BASE, Functions.constant(-0.0))),
			false
		);
	}

	protected Block getTargetedBlock(LivingEntity entity, float power) {
		return BlockUtils.getTargetBlock(this, entity, getRange(new SpellData(entity, power, null)));
	}

	protected Block getTargetedBlock(SpellData data) {
		return BlockUtils.getTargetBlock(this, data.caster(), getRange(data));
	}

	protected List<Block> getLastTwoTargetedBlocks(LivingEntity entity, float power) {
		return BlockUtils.getLastTwoTargetBlock(this, entity, getRange(new SpellData(entity, power, null)));
	}

	protected List<Block> getLastTwoTargetedBlocks(SpellData data) {
		return BlockUtils.getLastTwoTargetBlock(this, data.caster(), getRange(data));
	}

	public TargetInfo<Location> getTargetedBlockLocation(SpellData data) {
		return getTargetedBlockLocation(data, 0, 0, 0, true);
	}

	public TargetInfo<Location> getTargetedBlockLocation(SpellData data, boolean allowAir) {
		return getTargetedBlockLocation(data, 0, 0, 0, allowAir);
	}

	public TargetInfo<Location> getTargetedBlockLocation(SpellData data, double offX, double offY, double offZ) {
		return getTargetedBlockLocation(data, offX, offY, offZ, true);
	}

	public TargetInfo<Location> getTargetedBlockLocation(SpellData data, double offX, double offY, double offZ, boolean allowAir) {
		Location start = data.caster().getEyeLocation();
		Vector direction = start.getDirection();

		int range = getRange(data);

		RayTraceResult result = start.getWorld().rayTraceBlocks(start, direction, range, losFluidCollisionMode.get(data), losIgnorePassableBlocks.get(data), block -> !losTransparentBlocks.contains(block.getType()));
		Location location;
		if (result == null) {
			if (!allowAir) return new TargetInfo<>(null, data, false);
			location = start.add(direction.multiply(range)).toBlockLocation();
		} else location = result.getHitBlock().getLocation();

		location.add(offX, offY, offZ);

		location.setDirection(location.toVector().subtract(start.toVector()));

		SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data, location);
		if (!event.callEvent()) return new TargetInfo<>(null, event.getSpellData(), event.isCastCancelled());

		return new TargetInfo<>(event.getTargetLocation(), event.getSpellData(), false);
	}

	public TargetInfo<Location> getTargetedLocation(SpellData data) {
		return getTargetedLocation(data, true);
	}

	public TargetInfo<Location> getTargetedLocation(SpellData data, boolean allowAir) {
		Location start = data.caster().getEyeLocation();
		Vector direction = start.getDirection();
		World world = start.getWorld();

		int range = getRange(data);

		RayTraceResult result = world.rayTraceBlocks(start, direction, range, losFluidCollisionMode.get(data), losIgnorePassableBlocks.get(data), block -> !losTransparentBlocks.contains(block.getType()));
		Location location;
		if (result == null) {
			if (!allowAir) return new TargetInfo<>(null, data, false);
			location = start.add(direction.multiply(range));
		} else {
			Vector hitPosition = result.getHitPosition();
			location = new Location(world, hitPosition.getX(), hitPosition.getY(), hitPosition.getZ());
		}

		location.setDirection(location.toVector().subtract(start.toVector()));

		SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data, location);
		if (!event.callEvent()) return new TargetInfo<>(null, event.getSpellData(), event.isCastCancelled());

		return new TargetInfo<>(event.getTargetLocation(), event.getSpellData(), false);
	}

	public RayTraceResult rayTraceBlocks(SpellData data) {
		return rayTraceBlocks(data, getRange(data));
	}

	public RayTraceResult rayTraceBlocks(SpellData data, double range) {
		Location start = data.caster().getEyeLocation();
		Vector direction = start.getDirection();
		World world = start.getWorld();

		return world.rayTraceBlocks(start, direction, range, losFluidCollisionMode.get(data), losIgnorePassableBlocks.get(data), block -> !losTransparentBlocks.contains(block.getType()));
	}

	public Set<Material> getLosTransparentBlocks() {
		return losTransparentBlocks;
	}

	public boolean isTransparent(Block block) {
		return losTransparentBlocks.contains(block.getType());
	}

	public Predicate<Location> isTransparent(SpellData data) {
		FluidCollisionMode losFluidCollisionMode = this.losFluidCollisionMode.get(data);
		boolean losIgnorePassableBlocks = this.losIgnorePassableBlocks.get(data);

		return location -> {
			Block block = location.getBlock();

			Material type = block.getType();
			if (type.isAir() || losTransparentBlocks.contains(type)) return true;

			int bx = block.getX();
			int by = block.getY();
			int bz = block.getZ();

			double x = location.getX();
			double y = location.getY();
			double z = location.getZ();

			VoxelShape shape = block.getCollisionShape();

			Collection<BoundingBox> boxes = shape.getBoundingBoxes();
			if (!boxes.isEmpty()) {
				for (BoundingBox boundingBox : boxes) {
					boundingBox.shift(bx, by, bz);
					if (boundingBox.contains(x, y, z)) return false;
				}
			} else if (!losIgnorePassableBlocks) {
				BoundingBox boundingBox = block.getBoundingBox();
				if (boundingBox.contains(x, y, z)) return false;
			}

			if (losFluidCollisionMode == FluidCollisionMode.NEVER) return true;

			FluidData fluidData = location.getWorld().getFluidData(bx, by, bz);
			if (fluidData.getFluidType() == Fluid.EMPTY || !fluidData.isSource() && losFluidCollisionMode == FluidCollisionMode.SOURCE_ONLY)
				return true;

			float height = fluidData.computeHeight(location);
			return new BoundingBox(bx, by, bz, bx + 1, by + height, bz).contains(x, y, z);
		};
	}

	protected void playSpellEffects(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		Location location = data.location();

		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster, data);

		if (target != null) playSpellEffects(EffectPosition.TARGET, target, data);
		else if (location != null) playSpellEffects(EffectPosition.TARGET, location, data);

		if (location != null && target != null) {
			playSpellEffects(EffectPosition.START_POSITION, location, data);
			playSpellEffects(EffectPosition.END_POSITION, target, data);
			playSpellEffectsTrail(location, target.getLocation(), data);

			return;
		}

		if (caster != null && target != null) {
			playSpellEffects(EffectPosition.START_POSITION, caster, data);
			playSpellEffects(EffectPosition.END_POSITION, target, data);
			playSpellEffectsTrail(caster.getLocation(), target.getLocation(), data);

			return;
		}

		if (caster != null && location != null) {
			playSpellEffects(EffectPosition.START_POSITION, caster, data);
			playSpellEffects(EffectPosition.END_POSITION, location, data);
			playSpellEffectsTrail(caster.getLocation(), location, data);
		}
	}

	@Deprecated
	protected void playSpellEffects(Entity caster, Entity target) {
		playSpellEffects(caster, target, SpellData.NULL);
	}

	protected void playSpellEffects(Entity caster, Entity target, SpellData data) {
		playSpellEffects(EffectPosition.CASTER, caster, data);
		playSpellEffects(EffectPosition.TARGET, target, data);
		playSpellEffects(EffectPosition.START_POSITION, caster, data);
		playSpellEffects(EffectPosition.END_POSITION, target, data);
		playSpellEffectsTrail(caster.getLocation(), target.getLocation(), data);
	}

	@Deprecated
	protected void playSpellEffects(Entity caster, Location target) {
		playSpellEffects(caster, target, SpellData.NULL);
	}

	protected void playSpellEffects(Entity caster, Location target, SpellData data) {
		playSpellEffects(EffectPosition.CASTER, caster, data);
		playSpellEffects(EffectPosition.TARGET, target, data);
		playSpellEffects(EffectPosition.START_POSITION, caster, data);
		playSpellEffects(EffectPosition.END_POSITION, target, data);
		playSpellEffectsTrail(caster.getLocation(), target, data);
	}

	protected void playSpellEffects(Entity caster, Location from, Entity target, SpellData data) {
		playSpellEffects(EffectPosition.CASTER, caster, data);
		playSpellEffects(EffectPosition.TARGET, target, data);
		playSpellEffects(EffectPosition.START_POSITION, from, data);
		playSpellEffects(EffectPosition.END_POSITION, target, data);
		playSpellEffectsTrail(from, target.getLocation(), data);
	}

	@Deprecated
	protected void playSpellEffects(Location from, Entity target) {
		playSpellEffects(from, target, SpellData.NULL);
	}

	protected void playSpellEffects(Location from, Entity target, SpellData data) {
		playSpellEffects(EffectPosition.TARGET, target, data);
		playSpellEffects(EffectPosition.START_POSITION, from, data);
		playSpellEffects(EffectPosition.END_POSITION, target, data);
		playSpellEffectsTrail(from, target.getLocation(), data);
	}

	@Deprecated
	protected void playSpellEffects(Location startLoc, Location endLoc) {
		playSpellEffects(startLoc, endLoc, SpellData.NULL);
	}

	protected void playSpellEffects(Location startLoc, Location endLoc, SpellData data) {
		playSpellEffects(EffectPosition.CASTER, startLoc, data);
		playSpellEffects(EffectPosition.TARGET, endLoc, data);
		playSpellEffects(EffectPosition.START_POSITION, startLoc, data);
		playSpellEffects(EffectPosition.END_POSITION, endLoc, data);
		playSpellEffectsTrail(startLoc, endLoc, data);
	}

	@Deprecated
	protected void playSpellEffects(EffectPosition pos, Entity entity) {
		playSpellEffects(pos, entity, SpellData.NULL);
	}

	protected void playSpellEffects(EffectPosition pos, Entity entity, SpellData data) {
		if (effects == null) return;

		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return;

		for (SpellEffect effect : effectsList) {
			Runnable canceler = effect.playEffect(entity, data);
			if (canceler == null) continue;
			if (!(entity instanceof Player player)) continue;

			Map<EffectPosition, List<Runnable>> runnablesMap = callbacks.get(player.getUniqueId().toString());
			if (runnablesMap == null) continue;

			List<Runnable> runnables = runnablesMap.get(pos);
			if (runnables == null) continue;

			runnables.add(canceler);
		}
	}

	@Deprecated
	protected void playSpellEffects(EffectPosition pos, Location location) {
		playSpellEffects(pos, location, SpellData.NULL);
	}

	protected void playSpellEffects(EffectPosition pos, Location location, SpellData data) {
		if (effects == null) return;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return;
		for (SpellEffect effect : effectsList) {
			effect.playEffect(location, data);
		}
	}

	@Deprecated
	protected Set<EffectlibSpellEffect> playSpellEffectLibEffects(EffectPosition pos, Location location) {
		return playSpellEffectLibEffects(pos, location, SpellData.NULL);
	}

	protected Set<EffectlibSpellEffect> playSpellEffectLibEffects(EffectPosition pos, Location location, SpellData data) {
		if (effects == null) return null;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return null;
		Set<EffectlibSpellEffect> spellEffects = new HashSet<>();
		for (SpellEffect effect : effectsList) {
			if (!(effect instanceof EffectLibEffect)) continue;
			Effect effectLibEffect = effect.playEffectLib(location, data);
			if (effectLibEffect == null) continue;
			spellEffects.add(new EffectlibSpellEffect(effectLibEffect, (EffectLibEffect) effect));
		}
		return spellEffects;
	}

	@Deprecated
	protected Map<SpellEffect, Entity> playSpellEntityEffects(EffectPosition pos, Location location) {
		Map<SpellEffect, DelayableEntity<Entity>> values = playSpellEntityEffects(pos, location, SpellData.NULL);
		if (values == null) return null;

		Map<SpellEffect, Entity> map = new HashMap<>();
		values.forEach((key, value) -> value.ifPresent(entity -> map.put(key, entity)));

		return map;
	}

	protected Map<SpellEffect, DelayableEntity<Entity>> playSpellEntityEffects(EffectPosition pos, Location location, SpellData data) {
		if (effects == null) return null;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return null;

		Map<SpellEffect, DelayableEntity<Entity>> values = new HashMap<>();
		for (SpellEffect effect : effectsList) {
			if (!(effect instanceof EntityEffect)) continue;
			DelayableEntity<Entity> entity = effect.playEntityEffect(location, data);
			if (entity == null) continue;
			values.put(effect, entity);
		}

		return values;
	}

	@Deprecated
	protected Set<ArmorStand> playSpellArmorStandEffects(EffectPosition pos, Location location) {
		Set<DelayableEntity<ArmorStand>> values = playSpellArmorStandEffects(pos, location, SpellData.NULL);
		if (values == null) return null;

		Set<ArmorStand> set = new HashSet<>();
		values.forEach(stand -> stand.ifPresent(set::add));

		return set;
	}

	protected Set<DelayableEntity<ArmorStand>> playSpellArmorStandEffects(EffectPosition pos, Location location, SpellData data) {
		if (effects == null) return null;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return null;
		Set<DelayableEntity<ArmorStand>> armorStands = new HashSet<>();
		for (SpellEffect effect : effectsList) {
			if (!(effect instanceof ArmorStandEffect)) continue;
			DelayableEntity<ArmorStand> stand = effect.playArmorStandEffect(location, data);
			if (stand == null) continue;
			armorStands.add(stand);
		}
		return armorStands;
	}

	@Deprecated
	protected void playSpellEffectsTrail(Location loc1, Location loc2) {
		playSpellEffectsTrail(loc1, loc2, SpellData.NULL);
	}

	protected void playSpellEffectsTrail(Location loc1, Location loc2, SpellData data) {
		if (effects == null) return;
		if (!LocationUtil.isSameWorld(loc1, loc2)) return;
		List<SpellEffect> effectsList = effects.get(EffectPosition.TRAIL);
		if (effectsList != null) {
			for (SpellEffect effect : effectsList) {
				effect.playEffect(loc1, loc2, data);
			}
		}
		List<SpellEffect> rTrailEffects = effects.get(EffectPosition.REVERSE_LINE);
		if (rTrailEffects != null) {
			for (SpellEffect effect : rTrailEffects) {
				effect.playEffect(loc2, loc1, data);
			}
		}
	}

	@Deprecated
	public void playTrackingLinePatterns(EffectPosition pos, Location origin, Location target, Entity originEntity, Entity targetEntity) {
		playTrackingLinePatterns(pos, origin, target, originEntity, targetEntity, SpellData.NULL);
	}

	public void playTrackingLinePatterns(EffectPosition pos, Location origin, Location target, Entity originEntity, Entity targetEntity, SpellData data) {
		if (effects == null) return;
		List<SpellEffect> spellEffects = effects.get(pos);
		if (spellEffects == null) return;
		for (SpellEffect e : spellEffects) {
			e.playTrackingLinePatterns(origin, target, originEntity, targetEntity, data);
		}
	}

	public void initializePlayerEffectTracker(Player p) {
		if (callbacks == null) return;
		String key = p.getUniqueId().toString();
		Map<EffectPosition, List<Runnable>> entry = new EnumMap<>(EffectPosition.class);
		for (EffectPosition pos : EffectPosition.values()) {
			entry.put(pos, new ArrayList<>());
		}
		callbacks.put(key, entry);
	}

	public void unloadPlayerEffectTracker(Player p) {
		String uuid = p.getUniqueId().toString();
		for (EffectPosition pos : EffectPosition.values()) {
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
		for (String key : callbacks.keySet()) {
			cancelEffects(pos, key);
		}
	}

	public Set<EffectTracker> getEffectTrackers() {
		return effectTrackerSet;
	}

	public Set<AsyncEffectTracker> getAsyncEffectTrackers() {
		return asyncEffectTrackerSet;
	}

	protected void playSpellEffectsBuff(Entity entity, SpellEffect.SpellEffectActiveChecker checker, SpellData data) {
		if (effects == null) return;
		List<SpellEffect> effectList = effects.get(EffectPosition.BUFF);
		if (effectList != null) {
			for (SpellEffect effect : effectList) {
				EffectTracker tracker = effect.playEffectWhileActiveOnEntity(entity, checker, data);
				if (this instanceof BuffSpell) tracker.setBuffSpell((BuffSpell) this);
				effectTrackerSet.add(tracker);
			}
		}

		effectList = effects.get(EffectPosition.ORBIT);
		if (effectList != null) {
			for (SpellEffect effect : effectList) {
				EffectTracker tracker = effect.playEffectWhileActiveOrbit(entity, checker, data);
				if (this instanceof BuffSpell) tracker.setBuffSpell((BuffSpell) this);
				effectTrackerSet.add(tracker);
			}
		}

		effectList = effects.get(EffectPosition.BUFF_EFFECTLIB);
		if (effectList != null) {
			for (SpellEffect effect : effectList) {
				if (!(effect instanceof EffectLibEffect)) continue;
				AsyncEffectTracker tracker = effect.playEffectlibEffectWhileActiveOnEntity(entity, checker, data);
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

				AsyncEffectTracker tracker = effect.playEffectlibEffectWhileActiveOrbit(entity, checker, data);
				if (this instanceof BuffSpell) tracker.setBuffSpell((BuffSpell) this);
				asyncEffectTrackerSet.add(tracker);
			}
		}
	}

	protected void playSpellEffectsBuff(Entity entity, SpellEffect.SpellEffectActiveChecker checker) {
		playSpellEffectsBuff(entity, checker, null);
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

	protected CastItem[] setupCastItems(String stringKey, String listKey, String errorOptionName) {
		String[] items = new String[0];
		if (config.isString(internalKey + stringKey))
			items = config.getString(internalKey + stringKey, "").split(MagicItemDataParser.DATA_REGEX);
		else if (config.isList(internalKey + listKey))
			items = config.getStringList(internalKey + listKey, new ArrayList<>()).toArray(new String[0]);

		CastItem[] castItems = new CastItem[items.length];
		for (int i = 0; i < items.length; i++) {
			MagicItem magicItem = MagicItems.getMagicItemFromString(items[i]);
			ItemStack item = magicItem == null ? null : magicItem.getItemStack();
			if (item == null) {
				MagicSpells.error("Spell '" + internalName + "' has an invalid " + errorOptionName + " specified: " + items[i]);
				continue;
			}
			castItems[i] = new CastItem(item);
		}
		return castItems;
	}

	/**
	 * Attempts to initialise a subspell. This method never ignores empty names.
	 * @see Spell#initSubspell(String, String, boolean)
	 */
	protected Subspell initSubspell(String subspellName, String errorMessage) {
		return initSubspell(subspellName, errorMessage, false);
	}

	/**
	 * Attempts to initialise a subspell.
	 * @see Spell#initSubspell(String, String)
	 */
	protected Subspell initSubspell(String subspellName, String errorMessage, boolean ignoreEmptyName) {
		if (ignoreEmptyName && (subspellName == null || subspellName.isEmpty())) return null;

		if (subspellName == null) {
			MagicSpells.error(errorMessage);
			return null;
		}
		Subspell subspell = new Subspell(subspellName);
		if (!subspell.process()) {
			MagicSpells.error(errorMessage);
			return null;
		}

		return subspell;
	}

	/**
	 * Formats a string by performing the specified replacements.
	 *
	 * @param message      the string to format
	 * @param replacements the replacements to make, in pairs.
	 * @return the formatted string
	 */
	protected String formatMessage(String message, String... replacements) {
		return MagicSpells.formatMessage(message, replacements);
	}

	/**
	 * Sends a message to a player, first making the specified replacements. This method also does color replacement and has multi-line functionality.
	 *
	 * @param livingEntity the living entity to send the message to
	 * @param message      the message to send
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(LivingEntity livingEntity, String message, String... replacements) {
		MagicSpells.sendMessage(message, livingEntity, SpellData.NULL, replacements);
	}

	protected void sendMessage(LivingEntity livingEntity, String message) {
		MagicSpells.sendMessage(message, livingEntity, SpellData.NULL);
	}

	/**
	 * Sends a message to a player. This method also does color replacement and has multi-line functionality.
	 *
	 * @param livingEntity the living entity to send the message to
	 * @param message      the message to send
	 */
	protected void sendMessage(String message, LivingEntity livingEntity, String[] args) {
		MagicSpells.sendMessage(message, livingEntity, SpellData.NULL.args(args));
	}

	protected void sendMessage(String message, LivingEntity livingEntity) {
		MagicSpells.sendMessage(message, livingEntity, SpellData.NULL);
	}

	/**
	 * Sends a message to a player, first making the specified replacements.This method also does color replacement and has multi-line functionality.
	 *
	 * @param message      the message to send
	 * @param livingEntity the player to send the message to
	 * @param args         the arguments of associated spell cast
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(String message, LivingEntity livingEntity, String[] args, String... replacements) {
		MagicSpells.sendMessage(message, livingEntity, SpellData.NULL.args(args), replacements);
	}

	/**
	 * Sends a message to a player, first making the specified replacements.This method also does color replacement and has multi-line functionality.
	 *
	 * @param message      the message to send
	 * @param data         the data of the associated spell cast
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(String message, SpellData data, String... replacements) {
		MagicSpells.sendMessage(message, data.caster(), data, replacements);
	}

	/**
	 * Sends a message to a player, first making the specified replacements.This method also does color replacement and has multi-line functionality.
	 *
	 * @param message      the message to send
	 * @param recipient    the player to send the message to
	 * @param data         the data of the associated spell cast
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(String message, LivingEntity recipient, SpellData data, String... replacements) {
		MagicSpells.sendMessage(message, recipient, data, replacements);
	}

	/**
	 * Sends a message to all players near the specified player, within the configured broadcast range.
	 * @param livingEntity the "center" living entity used to find nearby players
	 * @param message the message to send
	 */
	@Deprecated
	protected void sendMessageNear(LivingEntity livingEntity, String message) {
		SpellData data = new SpellData(livingEntity);
		sendMessageNear(message, data, broadcastRange.get(data));
	}

	/**
	 * Sends a message to all players near the specified player, within the specified broadcast range.
	 * @param livingEntity the "center" living entity used to find nearby players
	 * @param message the message to send
	 * @param range the broadcast range
	 */
	@Deprecated
	protected void sendMessageNear(LivingEntity livingEntity, Player ignore, String message, int range, String[] args) {
		sendMessageNear(message, new SpellData(livingEntity, ignore, 1f, args), range);
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
	@Deprecated
	protected void sendMessageNear(LivingEntity livingEntity, Player ignore, String message, int range, String[] args, String... replacements) {
		sendMessageNear(message, new SpellData(livingEntity, ignore, 1f, args), range, replacements);
	}

	/**
	 * Sends a message to all players near the specified player, within the specified broadcast range.
	 *
	 * @param message      the message to send
	 * @param data         the associated spell data
	 * @param range        the broadcast range
	 * @param replacements replacements to be done on message
	 */
	protected void sendMessageNear(String message, SpellData data, int range, String... replacements) {
		if (message == null || message.isEmpty() || Perm.SILENT.has(data.caster())) return;

		Collection<Player> players = data.caster().getLocation().getNearbyPlayers(range);
		for (Player player : players) {
			if (Objects.equals(player, data.caster()) || Objects.equals(player, data.target())) continue;
			MagicSpells.sendMessage(message, player, data, replacements);
		}
	}

	/**
	 * Plays the fizzle sound if it is enabled for this spell.
	 */
	protected void fizzle(SpellData data) {
		if (!playFizzleSound.get(data) || !(data.caster() instanceof Player player)) return;
		player.playEffect(player.getLocation(), org.bukkit.Effect.EXTINGUISH, null);
	}

	protected String getTargetName(LivingEntity target) {
		return MagicSpells.getTargetName(target);
	}

	/**
	 * This should be called if a target should not be found. It sends the no target message
	 * and returns the appropriate return value.
	 *
	 * @param event targeting event that was cancelled
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(SpellTargetEvent event) {
		return noTarget(strNoTarget, event);
	}

	/**
	 * This should be called if a target should not be found. It sends the no target message
	 * and returns the appropriate return value.
	 *
	 * @param event targeting event that was cancelled
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(SpellTargetLocationEvent event) {
		return noTarget(strNoTarget, event);
	}

	/**
	 * This should be called if a target should not be found. It sends the no target message
	 * and returns the appropriate return value.
	 *
	 * @param info targeting info of the spell cast
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(TargetInfo<?> info) {
		return noTarget(strNoTarget, info);
	}

	/**
	 * This should be called if a target should not be found. It sends the no target message
	 * and returns the appropriate return value.
	 *
	 * @param data spell data of the spell cast
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(SpellData data) {
		return noTarget(strNoTarget, data);
	}

	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 *
	 * @param message the message to send
	 * @param event   targeting event that was cancelled
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(String message, SpellTargetEvent event) {
		if (event.isCastCancelled()) return new CastResult(PostCastAction.ALREADY_HANDLED, event.getSpellData());
		return noTarget(message, event.getSpellData());
	}

	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 *
	 * @param message the message to send
	 * @param event   targeting event that was cancelled
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(String message, SpellTargetLocationEvent event) {
		if (event.isCastCancelled()) return new CastResult(PostCastAction.ALREADY_HANDLED, event.getSpellData());
		return noTarget(message, event.getSpellData());
	}

	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 *
	 * @param message the message to send
	 * @param info    targeting info of the spell cast
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(String message, TargetInfo<?> info) {
		if (info.cancelled()) return new CastResult(PostCastAction.ALREADY_HANDLED, info.spellData());
		return noTarget(message, info.spellData());
	}

	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 *
	 * @param message the message to send
	 * @param data    spell data of the spell cast
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(String message, SpellData data) {
		fizzle(data);
		if (message != null && !message.isEmpty()) sendMessage(message, data);
		if (spellOnFail != null) spellOnFail.subcast(data.noTargeting());
		return new CastResult(alwaysActivate.get(data) ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED, data);
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

	public String getStrWrongCastItem() {
		return strWrongCastItem;
	}

	public String getStrCantCastByCommand() {
		return strCantCastByCommand;
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

	public String getStrOnTeach() {
		return strOnTeach;
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
		WRONG_WORLD,
		CANCELLED

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

		private final boolean cooldown;
		private final boolean reagents;
		private final boolean messages;

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
		public SpellData data;

		public SpellCastResult(SpellCastState state, PostCastAction action) {
			this.state = state;
			this.action = action;
			this.data = SpellData.NULL;
		}

		public SpellCastResult(SpellCastState state, PostCastAction action, SpellData data) {
			this.state = state;
			this.action = action;
			this.data = data;
		}

		public boolean success() {
			return state == SpellCastState.NORMAL && action != PostCastAction.ALREADY_HANDLED;
		}

		public boolean fail() {
			return state != SpellCastState.NORMAL || action == PostCastAction.ALREADY_HANDLED;
		}

	}

	public class DelayedSpellCast implements Runnable, Listener {

		private static final double motionTolerance = 0.2;

		private final SpellCastEvent spellCast;
		private final LivingEntity caster;
		private final Location from;
		private final int taskId;

		private final boolean interruptOnCast;
		private final boolean interruptOnMove;
		private final boolean interruptOnDamage;
		private final boolean interruptOnTeleport;

		public DelayedSpellCast(SpellCastEvent spellCast) {
			this.spellCast = spellCast;

			taskId = scheduleDelayedTask(this, spellCast.getCastTime());
			caster = spellCast.getCaster();
			from = caster.getLocation();

			SpellData data = spellCast.getSpellData();
			interruptOnCast = Spell.this.interruptOnCast.get(data);
			interruptOnMove = Spell.this.interruptOnMove.get(data);
			interruptOnDamage = Spell.this.interruptOnDamage.get(data);
			interruptOnTeleport = Spell.this.interruptOnTeleport.get(data);

			registerEvents(this);
		}

		@Override
		public void run() {
			if (!caster.isValid() || caster.isDead()) {
				unregisterEvents(this);
				return;
			}

			if (interruptOnMove && !inBounds(caster.getLocation())) {
				interrupt();
				return;
			}

			unregisterEvents(this);

			spellCast.setSpellCastState(getCastState(caster));
			spellCast.getSpell().onCast(spellCast);
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onMove(PlayerMoveEvent event) {
			if (!interruptOnMove) return;
			if (!event.getPlayer().equals(caster)) return;
			if (inBounds(event.getTo())) return;

			interrupt();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onDamage(EntityDamageEvent event) {
			if (!interruptOnDamage) return;
			if (!event.getEntity().equals(caster)) return;

			interrupt();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onSpellCast(SpellCastEvent event) {
			if (!interruptOnCast) return;
			if (event.getSpell() instanceof PassiveSpell) return;
			if (!event.getCaster().equals(caster)) return;
			if (!interruptFilter.isEmpty() && interruptFilter.check(event.getSpell())) return;

			interrupt();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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

			sendMessage(strInterrupted, caster, spellCast.getSpellData());
			if (spellOnInterrupt != null)
				spellOnInterrupt.subcast(new SpellData(caster, caster.getLocation(), spellCast.getPower(), spellCast.getSpellArgs()));
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

		private final boolean interruptOnCast;
		private final boolean interruptOnMove;
		private final boolean interruptOnDamage;
		private final boolean interruptOnTeleport;

		private int elapsed = 0;

		public DelayedSpellCastWithBar(SpellCastEvent spellCast) {
			this.spellCast = spellCast;

			castTime = spellCast.getCastTime();
			caster = spellCast.getCaster();
			from = caster.getLocation();

			SpellData data = spellCast.getSpellData();
			interruptOnCast = Spell.this.interruptOnCast.get(data);
			interruptOnMove = Spell.this.interruptOnMove.get(data);
			interruptOnDamage = Spell.this.interruptOnDamage.get(data);
			interruptOnTeleport = Spell.this.interruptOnTeleport.get(data);

			if (caster instanceof Player pl) MagicSpells.getExpBarManager().lock(pl, this);
			taskId = scheduleRepeatingTask(this, interval, interval);
			registerEvents(this);
		}

		@Override
		public void run() {
			if (!caster.isValid() || caster.isDead()) {
				end();
				return;
			}

			elapsed += interval;

			if (interruptOnMove && !inBounds(caster.getLocation())) {
				interrupt();
				return;
			}

			if (caster instanceof Player pl)
				MagicSpells.getExpBarManager().update(pl, 0, (float) elapsed / (float) castTime, this);

			if (elapsed >= castTime) {
				end();

				spellCast.setSpellCastState(getCastState(caster));
				spellCast.getSpell().onCast(spellCast);
			}
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onMove(PlayerMoveEvent event) {
			if (!interruptOnMove) return;
			if (!event.getPlayer().equals(caster)) return;
			if (inBounds(event.getTo())) return;

			interrupt();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onDamage(EntityDamageEvent event) {
			if (!interruptOnDamage) return;
			if (!event.getEntity().equals(caster)) return;

			interrupt();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onSpellCast(SpellCastEvent event) {
			if (!interruptOnCast) return;
			if (event.getSpell() instanceof PassiveSpell) return;
			if (!caster.equals(event.getCaster())) return;
			if (!interruptFilter.isEmpty() && interruptFilter.check(event.getSpell())) return;

			interrupt();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
			SpellData subData = spellCast.getSpellData().builder().target(null).location(caster.getLocation()).build();
			sendMessage(strInterrupted, caster, subData);
			if (spellOnInterrupt != null) spellOnInterrupt.subcast(subData);
			end();
		}

		private void end() {
			MagicSpells.cancelTask(taskId);
			unregisterEvents(this);

			if (caster instanceof Player pl) {
				MagicSpells.getExpBarManager().unlock(pl, this);
				MagicSpells.getExpBarManager().update(pl, pl.getLevel(), pl.getExp());
				ManaHandler mana = MagicSpells.getManaHandler();
				if (mana != null) mana.showMana(pl);
			}
		}

	}

	protected record SharedCooldown(Collection<Spell> spells, ConfigData<Float> cooldown) {

	}

	public ValidTargetChecker getValidTargetChecker() {
		return null;
	}

}
