package com.nisovin.magicspells;

import java.io.*;

import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.annotation.Annotation;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;

import co.aikar.commands.PaperCommandManager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.event.Event;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.entity.EntityType;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.events.*;
import com.nisovin.magicspells.handlers.*;
import com.nisovin.magicspells.listeners.*;
import com.nisovin.magicspells.util.managers.*;
import com.nisovin.magicspells.mana.ManaSystem;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.commands.MagicCommand;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.prompt.PromptType;
import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.commands.CommandHelpFilter;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.volatilecode.ManagerVolatile;
import com.nisovin.magicspells.volatilecode.VolatileCodeHandle;
import com.nisovin.magicspells.volatilecode.VolatileCodeDisabled;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;

import de.slikey.effectlib.EffectManager;

public class MagicSpells extends JavaPlugin {

	public static MagicSpells plugin;

	private static final FilenameFilter CLASS_DIRECTORY_FILTER = (File dir, String name) -> name.startsWith("classes");

	private static final List<ClassLoader> classLoaders = new ArrayList<>();

	// Change this when you want to start tweaking the source and fixing bugs
	public static Level DEVELOPER_DEBUG_LEVEL = Level.OFF;

	// Pass this to methods that want spell arguments passed but doesn't have any to be passed
	public static final String[] NULL_ARGS = null;

	private Set<Material> losTransparentBlocks;
	private List<Material> ignoreCastItemDurability;
	private Map<EntityType, String> entityNames;

	// Profiling
	private Map<String, Long> profilingTotalTime;
	private Map<String, Integer> profilingRuns;

	private Map<String, Spell> spells; // Map internal names to spells
	private Map<String, Spell> spellNames; // Map configured names to spells
	private Map<String, Spell> incantations; // Map incantation strings to spells
	private Map<String, Spellbook> spellbooks; // Player spellbooks

	private List<Spell> spellsOrdered; // Spells ordered

	// Container vars
	private ManaHandler manaHandler;
	private MoneyHandler moneyHandler;
	private MagicXpHandler magicXpHandler;
	private VolatileCodeHandle volatileCodeHandle;

	private BuffManager buffManager;
	private EffectManager effectManager;
	private BossBarManager bossBarManager;
	private VariableManager variableManager;
	private AttributeManager attributeManager;
	private PassiveManager passiveManager;
	private SpellEffectManager spellEffectManager;
	private ConditionManager conditionManager;
	private NoMagicZoneManager noMagicZones;
	private PaperCommandManager commandManager;
	private ExperienceBarManager expBarManager;

	private MagicConfig config;
	private MagicLogger magicLogger;
	private LifeLengthTracker lifeLengthTracker;

	boolean debug;
	boolean debugNull;
	boolean debugNumberFormat;
	boolean tabCompleteInternalNames;

	boolean enableProfiling;
	boolean enableErrorLogging;

	boolean hideMagicItemTooltips;
	boolean ignoreCastItemNames;
	boolean ignoreCastItemAmount;
	boolean ignoreCastItemEnchants;
	boolean ignoreCastItemNameColors;
	boolean ignoreCastItemBreakability;
	boolean ignoreCastItemColor;
	boolean ignoreCastItemPotionType;
	boolean ignoreCastItemTitle;
	boolean ignoreCastItemAuthor;
	boolean ignoreCastItemLore;
	boolean ignoreCastItemCustomModelData;

	boolean castOnAnimate;
	boolean enableManaSystem;
	boolean ignoreCastPerms;
	boolean opsHaveAllSpells;
	boolean ignoreGrantPerms;
	boolean checkWorldPvpFlag;
	boolean allowCastWithFist;
	boolean castWithLeftClick;
	boolean castWithRightClick;
	boolean reverseBowCycleButtons;
	boolean bowCycleSpellsSneaking;
	boolean allowCycleToNoSpell;

	boolean checkScoreboardTeams;
	boolean defaultAllPermsFalse;
	boolean enableTempGrantPerms;
	boolean ignoreDefaultBindings;
	boolean useExpBarAsCastTimeBar;
	boolean alwaysShowMessageOnCycle;
	boolean onlyCycleToCastableSpells;
	boolean ignoreGrantPermsFakeValue;
	boolean cycleSpellsOnOffhandAction;
	boolean separatePlayerSpellsPerWorld;
	boolean showStrCostOnMissingReagents;
	boolean cooldownsPersistThroughReload;
	public boolean allowAnticheatIntegrations;

	int debugLevel;
	int spellIconSlot;
	private int globalRadius;
	int globalCooldown;
	int broadcastRange;

	long lastReloadTime = 0;

	ChatColor textColor;

	// Strings
	String strCantCast;
	String strCantBind;
	String strWrongWorld;
	String strOnCooldown;
	String strSpellChange;
	String strConsoleName;
	String strUnknownSpell;
	String strXpAutoLearned;
	String strMissingReagents;
	String strSpellChangeEmpty;

	String soundFailOnCooldown;
	String soundFailMissingReagents;

	@Override
	public void onEnable() {
		load();

		Metrics metrics = new Metrics(this);
	}

	public void load() {
		plugin = this;
		PluginManager pm = plugin.getServer().getPluginManager();

		effectManager = new EffectManager(this);
		effectManager.enableDebug(debug);

		commandManager = new PaperCommandManager(plugin);

		// Create storage stuff
		spells = new HashMap<>();
		spellNames = new HashMap<>();
		spellsOrdered = new ArrayList<>();
		spellbooks = new HashMap<>();
		incantations = new HashMap<>();

		// Make sure directories are created
		getDataFolder().mkdir();
		new File(getDataFolder(), "spellbooks").mkdir();

		// Load config
		if (!new File(getDataFolder(), "general.yml").exists()) {
			saveResource("general.yml", false);
			if (!new File(getDataFolder(), "mana.yml").exists()) saveResource("mana.yml", false);
			if (!new File(getDataFolder(), "spells-command.yml").exists()) saveResource("spells-command.yml", false);
			if (!new File(getDataFolder(), "spells-regular.yml").exists()) saveResource("spells-regular.yml", false);
			if (!new File(getDataFolder(), "zones.yml").exists()) saveResource("zones.yml", false);
		}
		config = new MagicConfig();
		if (!config.isLoaded()) {
			MagicSpells.log(Level.SEVERE, "Error in config file, stopping config load");
			return;
		}

		// Construct volatile handler
		try {
			volatileCodeHandle = ManagerVolatile.INSTANCE.constructVolatileCodeHandler();
		} catch (Throwable t) {
			MagicSpells.log("Volatile code handler not found, using fallback.");
			volatileCodeHandle = new VolatileCodeDisabled();
		}

		String path = "general.";
		String manaPath = "mana.";

		debug = config.getBoolean(path + "debug", false);
		debugNull = config.getBoolean(path + "debug-null", true);
		debugNumberFormat = config.getBoolean(path + "debug-number-format", true);
		debugLevel = config.getInt(path + "debug-level", 3);

		tabCompleteInternalNames = config.getBoolean(path + "tab-complete-internal-names", false);

		enableErrorLogging = config.getBoolean(path + "enable-error-logging", true);
		enableProfiling = config.getBoolean(path + "enable-profiling", false);
		textColor = ChatColor.getByChar(config.getString(path + "text-color", ChatColor.DARK_AQUA.getChar() + ""));
		broadcastRange = config.getInt(path + "broadcast-range", 20);

		opsHaveAllSpells = config.getBoolean(path + "ops-have-all-spells", true);
		defaultAllPermsFalse = config.getBoolean(path + "default-all-perms-false", false);
		ignoreGrantPerms = config.getBoolean(path + "ignore-grant-perms", false);
		ignoreGrantPermsFakeValue = config.getBoolean(path + "ignore-grant-perms-fake-value", true);
		ignoreCastPerms = config.getBoolean(path + "ignore-cast-perms", false);
		enableTempGrantPerms = config.getBoolean(path + "enable-tempgrant-perms", true);

		separatePlayerSpellsPerWorld = config.getBoolean(path + "separate-player-spells-per-world", false);
		allowCycleToNoSpell = config.getBoolean(path + "allow-cycle-to-no-spell", false);
		reverseBowCycleButtons = config.getBoolean(path + "reverse-bow-cycle-buttons", false);
		bowCycleSpellsSneaking = config.getBoolean(path + "bow-cycle-spells-sneaking", false);
		alwaysShowMessageOnCycle = config.getBoolean(path + "always-show-message-on-cycle", false);
		onlyCycleToCastableSpells = config.getBoolean(path + "only-cycle-to-castable-spells", true);
		spellIconSlot = config.getInt(path + "spell-icon-slot", -1);
		allowCastWithFist = config.getBoolean(path + "allow-cast-with-fist", false);
		castWithLeftClick = config.getBoolean(path + "cast-with-left-click", true);
		castWithRightClick = config.getBoolean(path + "cast-with-right-click", false);
		cycleSpellsOnOffhandAction = config.getBoolean(path + "cycle-spells-with-offhand-action", false);

		ignoreDefaultBindings = config.getBoolean(path + "ignore-default-bindings", false);
		ignoreCastItemEnchants = config.getBoolean(path + "ignore-cast-item-enchants", true);
		ignoreCastItemNames = config.getBoolean(path + "ignore-cast-item-names", false);
		ignoreCastItemAmount = config.getBoolean(path + "ignore-cast-item-amount", true);
		ignoreCastItemNameColors = config.getBoolean(path + "ignore-cast-item-name-colors", false);
		ignoreCastItemBreakability = config.getBoolean(path + "ignore-cast-item-breakability", true);
		ignoreCastItemColor = config.getBoolean(path + "ignore-cast-item-color", true);
		ignoreCastItemPotionType = config.getBoolean(path + "ignore-cast-item-potion-types", true);
		ignoreCastItemTitle = config.getBoolean(path + "ignore-cast-item-title", true);
		ignoreCastItemAuthor = config.getBoolean(path + "ignore-cast-item-author", true);
		ignoreCastItemLore = config.getBoolean(path + "ignore-cast-item-lore", true);
		ignoreCastItemCustomModelData = config.getBoolean(path + "ignore-cast-item-custom-model-data", true);
		ignoreCastItemDurability = Util.getMaterialList(config.getStringList(path + "ignore-cast-item-durability", new ArrayList<>()), ArrayList::new);

		checkWorldPvpFlag = config.getBoolean(path + "check-world-pvp-flag", true);
		checkScoreboardTeams = config.getBoolean(path + "check-scoreboard-teams", false);
		showStrCostOnMissingReagents = config.getBoolean(path + "show-str-cost-on-missing-reagents", true);
		losTransparentBlocks = Util.getMaterialList(config.getStringList(path + "los-transparent-blocks", new ArrayList<>()), HashSet::new);
		if (losTransparentBlocks.isEmpty()) {
			losTransparentBlocks.add(Material.AIR);
			losTransparentBlocks.add(Material.VOID_AIR);
			losTransparentBlocks.add(Material.CAVE_AIR);
		}
		globalRadius = config.getInt(path + "global-radius", 500);
		globalCooldown = config.getInt(path + "global-cooldown", 500);
		castOnAnimate = config.getBoolean(path + "cast-on-animate", false);
		useExpBarAsCastTimeBar = config.getBoolean(path + "use-exp-bar-as-cast-time-bar", true);
		cooldownsPersistThroughReload = config.getBoolean(path + "cooldowns-persist-through-reload", true);

		entityNames = new HashMap<>();
		if (config.contains(path + "entity-names")) {
			Set<String> keys = config.getSection(path + "entity-names").getKeys(false);
			for (String key : keys) {
				EntityType entityType = Util.getEntityType(key);
				if (entityType == null) continue;
				entityNames.put(entityType, config.getString(path + "entity-names." + key, ""));
			}
		}

		soundFailOnCooldown = config.getString(path + "sound-on-cooldown", null);
		soundFailMissingReagents = config.getString(path + "sound-missing-reagents", null);

		strUnknownSpell = config.getString(path + "str-unknown-spell", "You do not know a spell with that name.");
		strSpellChange = config.getString(path + "str-spell-change", "You are now using the %s spell.");
		strSpellChangeEmpty = config.getString(path + "str-spell-change-empty", "You are no longer using a spell.");
		strOnCooldown = config.getString(path + "str-on-cooldown", "That spell is on cooldown (%c seconds remaining).");
		strMissingReagents = config.getString(path + "str-missing-reagents", "You do not have the reagents for that spell.");
		strCantCast = config.getString(path + "str-cant-cast", "You can't cast that spell right now.");
		strCantBind = config.getString(path + "str-cant-bind", "You cannot bind that spell to that item.");
		strWrongWorld = config.getString(path + "str-wrong-world", "You cannot cast that spell here.");
		strConsoleName = config.getString(path + "console-name", "Admin");
		strXpAutoLearned = config.getString(path + "str-xp-auto-learned", "You have learned the %s spell!");

		allowAnticheatIntegrations = config.getBoolean(path + "allow-anticheat-integrations", false);

		enableManaSystem = config.getBoolean(manaPath + "enable-mana-system", false);

		// Create handling objects
		noMagicZones = new NoMagicZoneManager();
		buffManager = new BuffManager(config.getInt(path + "buff-check-interval", 100));
		expBarManager = new ExperienceBarManager();
		bossBarManager = new BossBarManager();
		attributeManager = new AttributeManager();
		if (CompatBasics.pluginEnabled("Vault")) moneyHandler = new MoneyHandler();
		lifeLengthTracker = new LifeLengthTracker();

		// Call loading event
		pm.callEvent(new MagicSpellsLoadingEvent(this));

		// Init permissions
		log("Initializing permissions");
		boolean opsIgnoreReagents = config.getBoolean(path + "ops-ignore-reagents", true);
		boolean opsIgnoreCooldowns = config.getBoolean(path + "ops-ignore-cooldowns", true);
		boolean opsIgnoreCastTimes = config.getBoolean(path + "ops-ignore-cast-times", true);
		addPermission(pm, "noreagents", opsIgnoreReagents? PermissionDefault.OP : PermissionDefault.FALSE, "Allows casting without needing reagents");
		addPermission(pm, "nocooldown", opsIgnoreCooldowns? PermissionDefault.OP : PermissionDefault.FALSE, "Allows casting without being affected by cooldowns");
		addPermission(pm, "nocasttime", opsIgnoreCastTimes? PermissionDefault.OP : PermissionDefault.FALSE, "Allows casting without being affected by cast times");
		addPermission(pm, "notarget", PermissionDefault.FALSE, "Prevents being targeted by any targeted spells");
		addPermission(pm, "silent", PermissionDefault.FALSE, "Prevents cast messages from being broadcast to players");
		Map<String, Boolean> permGrantChildren = new HashMap<>();
		Map<String, Boolean> permLearnChildren = new HashMap<>();
		Map<String, Boolean> permCastChildren = new HashMap<>();
		Map<String, Boolean> permTeachChildren = new HashMap<>();

		// Load magic items
		log("Loading magic items...");
		hideMagicItemTooltips = config.getBoolean(path + "hide-magic-items-tooltips", false);
		if (hideMagicItemTooltips) log("... hiding tooltips!");

		MagicItems.getMagicItems().clear();
		String itemStr = "magic-items";
		if (config.contains(path + itemStr)) {
			Set<String> magicItems = config.getKeys(path + itemStr);
			if (magicItems != null) {
				for (String key : magicItems) {
					if (config.isString(path + itemStr + "." + key)) {
						String str = config.getString(path + itemStr + "." + key, null);
						if (str == null) continue;

						MagicItem magicItem = MagicItems.getMagicItemFromString(str);
						if (magicItem != null) MagicItems.getMagicItems().put(key, magicItem);
						else MagicSpells.error("Invalid magic item: " + key + ": " + str);

					} else if (config.isSection(path + itemStr + "." + key)) {
						ConfigurationSection section = config.getSection(path + itemStr + "." + key);
						if (section == null) continue;

						MagicItem magicItem = MagicItems.getMagicItemFromSection(section);
						if (magicItem != null) MagicItems.getMagicItems().put(key, magicItem);
						else MagicSpells.error("Invalid magic item: " + key + ": (section)");

					} else MagicSpells.error("Invalid magic item: " + key);
				}
			}
		}
		log("..." + MagicItems.getMagicItems().size() + " magic items loaded");

		// Load crafting recipes.
		RecipeHandler.clearRecipes();
		log("Loading recipes...");
		if (config.contains(path + "recipes") && config.isSection(path + "recipes")) {
			ConfigurationSection recipeSec = config.getSection(path + "recipes");
			for (String recipeKey : recipeSec.getKeys(false)) {
				ConfigurationSection recipe = recipeSec.getConfigurationSection(recipeKey);
				if (recipe == null) continue;
				RecipeHandler.create(recipe);
			}
		}
		log("..." + RecipeHandler.getRecipes().size() + " recipes loaded");

		// Load spells
		log("Loading spells...");
		loadSpells(config, pm, permGrantChildren, permLearnChildren, permCastChildren, permTeachChildren);
		log("...spells loaded: " + spells.size());
		if (spells.isEmpty()) {
			MagicSpells.error("No spells loaded!");
			return;
		}

		log("Finalizing perms...");
		// Finalize spell permissions
		addPermission(pm, "grant.*", PermissionDefault.FALSE, permGrantChildren);
		addPermission(pm, "learn.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permLearnChildren);
		addPermission(pm, "cast.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permCastChildren);
		addPermission(pm, "teach.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permTeachChildren);

		// Advanced permissions
		addPermission(pm, "advanced.list", PermissionDefault.FALSE);
		addPermission(pm, "advanced.forget", PermissionDefault.FALSE);
		addPermission(pm, "advanced.scroll", PermissionDefault.FALSE);
		Map<String, Boolean> advancedPermChildren = new HashMap<>();
		advancedPermChildren.put(Perm.ADVANCED_LIST.getNode(), true);
		advancedPermChildren.put(Perm.ADVANCED_FORGET.getNode(), true);
		advancedPermChildren.put(Perm.ADVANCED_SCROLL.getNode(), true);
		addPermission(pm, "advanced.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.OP, advancedPermChildren);

		// Command permissions
		addPermission(pm, "command.help", PermissionDefault.OP);
		addPermission(pm, "command.reload", PermissionDefault.OP);
		addPermission(pm, "command.reload.spellbook", PermissionDefault.OP);
		addPermission(pm, "command.reload.effectlib", PermissionDefault.OP);
		addPermission(pm, "command.resetcd", PermissionDefault.OP);
		addPermission(pm, "command.mana.show", PermissionDefault.OP);
		addPermission(pm, "command.mana.reset", PermissionDefault.OP);
		addPermission(pm, "command.mana.setmax", PermissionDefault.OP);
		addPermission(pm, "command.mana.add", PermissionDefault.OP);
		addPermission(pm, "command.mana.set", PermissionDefault.OP);
		addPermission(pm, "command.mana.updaterank", PermissionDefault.OP);
		addPermission(pm, "command.variable.show", PermissionDefault.OP);
		addPermission(pm, "command.variable.modify", PermissionDefault.OP);
		addPermission(pm, "command.magicitem", PermissionDefault.OP);
		addPermission(pm, "command.util.download", PermissionDefault.OP);
		addPermission(pm, "command.util.update", PermissionDefault.OP);
		addPermission(pm, "command.util.saveskin", PermissionDefault.OP);
		addPermission(pm, "command.profilereport", PermissionDefault.OP);
		addPermission(pm, "command.debug", PermissionDefault.OP);
		addPermission(pm, "command.magicxp", PermissionDefault.OP);
		addPermission(pm, "command.cast.power", PermissionDefault.OP);
		addPermission(pm, "command.cast.self", PermissionDefault.TRUE);
		addPermission(pm, "command.cast.as", PermissionDefault.OP);
		addPermission(pm, "command.cast.on", PermissionDefault.OP);
		addPermission(pm, "command.cast.at", PermissionDefault.OP);

		log("...done");

		// Load xp system
		if (config.getBoolean(path + "enable-magic-xp", false)) {
			log("Loading xp system...");
			magicXpHandler = new MagicXpHandler(this, config);
			log("...xp system loaded");
		}

		// Load in-game spell names, incantations, and initialize spells
		log("Initializing spells...");
		for (Spell spell : spells.values()) {
			spellNames.put(Util.decolorize(spell.getName().toLowerCase()), spell);
			String[] aliases = spell.getAliases();
			if (aliases != null && aliases.length > 0) {
				for (String alias : aliases) {
					String lowercaseAlias = alias.toLowerCase();
					if (!spellNames.containsKey(lowercaseAlias)) spellNames.put(lowercaseAlias, spell);
				}
			}
			List<String> incs = spell.getIncantations();
			if (incs != null && !incs.isEmpty()) {
				for (String s : incs) {
					incantations.put(s.toLowerCase(), spell);
				}
			}
			spell.initialize();
		}
		log("...done");

		// Load online player spellbooks
		log("Loading online player spellbooks...");
		Util.forEachPlayerOnline(p -> spellbooks.put(p.getName(), new Spellbook(p, this)));
		log("...done");

		// Load saved cooldowns
		if (cooldownsPersistThroughReload) {
			File file = new File(getDataFolder(), "cooldowns.txt");
			Scanner scanner = null;
			if (file.exists()) {
				try {
					scanner = new Scanner(file);
					while (scanner.hasNext()) {
						String line = scanner.nextLine();
						if (line.isEmpty()) continue;
						String[] data = line.split(":");
						long cooldown = Long.parseLong(data[2]);
						if (cooldown > System.currentTimeMillis()) {
							Spell spell = getSpellByInternalName(data[0]);
							if (spell != null) spell.setCooldownManually(UUID.fromString(data[1]), cooldown);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (scanner != null) scanner.close();
					file.delete();
				}
			}
			log("Restored cooldowns");
		}

		// Setup mana
		if (enableManaSystem) {
			log("Enabling mana system...");

			manaHandler = new ManaSystem(config);

			log("...done");
		}

		// Load no-magic zones
		noMagicZones.load(config);
		if (noMagicZones.zoneCount() == 0) noMagicZones = null;

		// Load listeners
		log("Loading cast listeners...");
		registerEvents(new MagicPlayerListener(this));
		registerEvents(new MagicSpellListener(this));
		registerEvents(new CastListener(this));
		if (!incantations.isEmpty()) registerEvents(new MagicChatListener(this));

		RightClickListener rightClickListener = new RightClickListener(this);
		if (rightClickListener.hasRightClickCastItems()) registerEvents(rightClickListener);

		ConsumeListener consumeListener = new ConsumeListener(this);
		if (consumeListener.hasConsumeCastItems()) registerEvents(consumeListener);
		if (config.getBoolean(path + "enable-dance-casting", true)) new DanceCastListener(this, config);

		log("...done");

		// Initialize logger
		if (config.getBoolean(path + "enable-logging", false)) {
			magicLogger = new MagicLogger(this);
		}

		// Register commands
		commandManager.enableUnstableAPI("help");
		commandManager.registerCommand(new MagicCommand());
		CommandHelpFilter.mapPerms();

		// Setup profiling
		if (enableProfiling) {
			profilingTotalTime = new HashMap<>();
			profilingRuns = new HashMap<>();
		}

		CompatBasics.setupExemptionAssistant();

		// Load external data
		Bukkit.getScheduler().runTaskLater(this, this::loadExternalData, 1);
	}

	private void loadExternalData() {
		PluginManager pm = plugin.getServer().getPluginManager();
		log("Loading external data...");

		loadVariables(pm);
		loadSpellEffects(pm);
		loadConditions(pm);
		loadPassiveListeners(pm);

		log("...done");

		// Call loaded event
		pm.callEvent(new MagicSpellsLoadedEvent(this));

		log("MagicSpells loading complete!");
	}

	private void loadVariables(PluginManager pm) {
		// Load variables
		log("Loading variables...");
		String path = "general.";
		ConfigurationSection varSec = null;
		if (config.contains(path + "variables") && config.isSection(path + "variables")) {
			varSec = config.getSection(path + "variables");
		}
		variableManager = new VariableManager();

		// Call variable event
		pm.callEvent(new VariablesLoadingEvent(plugin, variableManager));

		variableManager.loadVariables(varSec);

		spells.values().forEach(Spell::initializeVariables);

		if (!variableManager.getVariables().isEmpty()) registerEvents(new VariableListener());

		log("...variable meta types loaded: " + variableManager.getMetaVariables().size());
		log("...variable types loaded: " + variableManager.getVariableTypes().size());
		log("...variables loaded: " + (variableManager.getVariables().size() - variableManager.getMetaVariables().size()));
	}

	private void loadSpellEffects(PluginManager pm) {
		// Load spell effects
		log("Loading spell effect types...");
		spellEffectManager = new SpellEffectManager();

		// Call spell effect event
		pm.callEvent(new SpellEffectsLoadingEvent(plugin, spellEffectManager));

		spells.values().forEach(Spell::initializeSpellEffects);

		log("...spell effect types loaded: " + spellEffectManager.getSpellEffects().size());
	}

	private void loadConditions(PluginManager pm) {
		// Load conditions
		log("Loading conditions...");
		conditionManager = new ConditionManager();

		// Call condition event
		pm.callEvent(new ConditionsLoadingEvent(plugin, conditionManager));

		for (Spell spell : spells.values()) {
			spell.initializeModifiers();
		}

		if (enableManaSystem) {
			// setup mana bar conditions
			manaHandler.initialize();

			// Setup online player mana bars
			Util.forEachPlayerOnline(p -> manaHandler.createManaBar(p));
		}

		ModifierSet.initializeModifierListeners();
		log("...conditions loaded: " + conditionManager.getConditions().size());
	}

	private void loadPassiveListeners(PluginManager pm) {
		// Load passive listeners
		log("Loading passive listeners...");
		passiveManager = new PassiveManager();

		// Call passive event
		pm.callEvent(new PassiveListenersLoadingEvent(plugin, passiveManager));

		for (Spell spell : spells.values()) {
			if (!(spell instanceof PassiveSpell)) continue;
			((PassiveSpell) spell).initializeListeners();
		}

		log("...passive listeners loaded: " + passiveManager.getListeners().size());
	}

	private static final int LONG_LOAD_THRESHOLD = 50;
	// DEBUG INFO: level 2, loaded spell spellName
	private void loadSpells(MagicConfig config, PluginManager pm, Map<String, Boolean> permGrantChildren, Map<String, Boolean> permLearnChildren, Map<String, Boolean> permCastChildren, Map<String, Boolean> permTeachChildren) {
		long startTimePre = System.currentTimeMillis();

		// Load classes from folders inside the plugin
		for (File directoryFile : getDataFolder().listFiles(CLASS_DIRECTORY_FILTER)) {
			if (!directoryFile.isDirectory()) continue;

			classLoaders.add(createSpellClassLoader(directoryFile));
		}

		// Load classes from the plugin folder
		classLoaders.add(createSpellClassLoader(getDataFolder()));

		// Get spells from config
		Set<String> spellKeys = config.getSpellKeys();
		if (spellKeys == null) return;

		Map<String, Constructor<? extends Spell>> constructors = new HashMap<>();
		for (String spellName : spellKeys) {
			if (!config.getBoolean("spells." + spellName + ".enabled", true)) continue;
			long startTime = System.currentTimeMillis();
			String className = "";
			if (config.contains("spells." + spellName + ".spell-class")) className = config.getString("spells." + spellName + ".spell-class", "");

			if (className == null || className.isEmpty()) {
				error("Spell '" + spellName + "' does not have a spell-class property");
				continue;
			}

			if (className.startsWith(".")) className = "com.nisovin.magicspells.spells" + className;

			Constructor<? extends Spell> constructor = constructors.get(className);

			// Load spell class
			if (constructor == null) {
				for (ClassLoader cl : classLoaders) {
					Class<? extends Spell> spellClass;
					try {
						spellClass = cl.loadClass(className).asSubclass(Spell.class);
					} catch (ClassNotFoundException e) {
						continue;
					}

					try {
						constructor = spellClass.getConstructor(MagicConfig.class, String.class);
					} catch (NoSuchMethodException e) {
						continue;
					}

					constructor.setAccessible(true);
					constructors.put(className, constructor);
				}
			}

			constructor = constructors.get(className);
			if (constructor == null) {
				error("Unable to load spell " + spellName + " (missing/malformed class " + className + ')');
				continue;
			}

			Spell spell;
			try {
				spell = constructor.newInstance(config, spellName);
			} catch (Exception e) {
				error("Unable to load spell " + spellName + " (general error)");
				e.printStackTrace();
				continue;
			}

			spells.put(spellName.toLowerCase(), spell);
			spellsOrdered.add(spell);

			// Add permissions
			if (!spell.isHelperSpell()) {
				String permName = spell.getPermissionName();
				if (!spell.isAlwaysGranted()) {
					addPermission(pm, "grant." + permName, PermissionDefault.FALSE);
					permGrantChildren.put(Perm.GRANT.getNode() + permName, true);
				}
				addPermission(pm, "learn." + permName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
				addPermission(pm, "cast." + permName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
				addPermission(pm, "teach." + permName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
				if (enableTempGrantPerms) addPermission(pm, "tempgrant." + permName, PermissionDefault.FALSE);

				permLearnChildren.put(Perm.LEARN.getNode() + permName, true);
				permCastChildren.put(Perm.CAST.getNode() + permName, true);
				permTeachChildren.put(Perm.TEACH.getNode() + permName, true);
			}

			// Done
			debug(2, "Loaded spell: " + spellName);

			long elapsed = System.currentTimeMillis() - startTime;
			if (elapsed > LONG_LOAD_THRESHOLD) getLogger().warning("LONG SPELL LOAD TIME: " + spellName + ": " + elapsed + "ms");
		}

		long finalElapsed = System.currentTimeMillis() - startTimePre;
		if (lastReloadTime != 0) getLogger().warning("Loaded in " + finalElapsed + "ms (previously " + lastReloadTime + " ms)");
		getLogger().warning("Need help? Check out our discord: discord.gg/6bYqnNy");
		lastReloadTime = finalElapsed;
	}

	public static List<ClassLoader> getClassLoaders() {
		return classLoaders;
	}

	// Create class loader
	public ClassLoader createSpellClassLoader(List<File> jarList, File dataFolder) {
		URL[] urls = new URL[jarList.size() + 1];
		ClassLoader cl = null;
		try {
			urls[0] = dataFolder.toURI().toURL();
			for (int i = 1; i <= jarList.size(); i++) {
				urls[i] = jarList.get(i - 1).toURI().toURL();
			}
			cl = new URLClassLoader(urls, getClassLoader());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return cl;
	}

	// Create class loader with jar files within the directory
	public ClassLoader createSpellClassLoader(File dataFolder) {
		final List<File> jarList = new ArrayList<>();
		for (File file : dataFolder.listFiles()) {
			if (file.getName().endsWith(".jar")) jarList.add(file);
		}
		return createSpellClassLoader(jarList, dataFolder);
	}

	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault) {
		addPermission(pm, perm, permDefault, null, null);
	}

	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault, String description) {
		addPermission(pm, perm, permDefault, null, description);
	}

	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault, Map<String,Boolean> children) {
		addPermission(pm, perm, permDefault, children, null);
	}

	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault, Map<String,Boolean> children, String description) {
		if (pm.getPermission("magicspells." + perm) == null) {
			if (description == null) pm.addPermission(new Permission("magicspells." + perm, permDefault, children));
			else pm.addPermission(new Permission("magicspells." + perm, description, permDefault, children));
		}
	}

	public static void setupEffectlib() {
		if (plugin.effectManager != null) return;
		plugin.effectManager = new EffectManager(plugin);
		plugin.effectManager.enableDebug(plugin.debug);
	}

	public static void disposeEffectlib() {
		if (plugin.effectManager == null) return;
		plugin.effectManager.cancel(true);
		plugin.effectManager.dispose();
		plugin.effectManager = null;
	}

	/**
	 * Gets the instance of the MagicSpells plugin
	 * @return the MagicSpells plugin
	 */
	public static MagicSpells getInstance() {
		return plugin;
	}

	/**
	 * Gets all the spells currently loaded
	 * @return a Collection of Spell objects
	 */
	public static Collection<Spell> spells() {
		return plugin.spells.values();
	}

	/**
	 * Gets a spell by its internal name (the key name in the config file)
	 * @param spellName the internal name of the spell to find
	 * @return the Spell found, or null if no spell with that name was found
	 */
	public static Spell getSpellByInternalName(String spellName) {
		return plugin.spells.get(spellName.toLowerCase());
	}

	/**
	 * Gets a spell by its in-game name (the name specified with the 'name' config option)
	 * @param spellName the in-game name of the spell to find
	 * @return the Spell found, or null if no spell with that name was found
	 */
	public static Spell getSpellByInGameName(String spellName) {
		return plugin.spellNames.get(spellName.toLowerCase());
	}

	/**
	 * Gets a player's spellbook, which contains known spells and handles spell permissions.
	 * If a player does not have a spellbook, one will be created.
	 * @param player the player to get a spellbook for
	 * @return the player's spellbook
	 */
	public static Spellbook getSpellbook(Player player) {
		Spellbook spellbook = plugin.spellbooks.computeIfAbsent(player.getName(), playerName -> new Spellbook(player, plugin));
		if (spellbook == null) throw new IllegalStateException();
		return spellbook;
	}

	public static ChatColor getTextColor() {
		return plugin.textColor;
	}

	/**
	 * Gets a list of blocks that are considered transparent
	 * @return list of block types
	 */
	public static Set<Material> getTransparentBlocks() {
		return plugin.losTransparentBlocks;
	}

	/**
	 * Gets a map of entity types and their configured names, to be used when sending messages to players
	 * @return the map
	 */
	public static Map<EntityType, String> getEntityNames() {
		return plugin.entityNames;
	}

	/**
	 * Checks whether to ignore the durability on the given type when using it as a cast item.
	 * @param type the type to check
	 * @return whether to ignore durability
	 */
	public static boolean ignoreCastItemDurability(Material type) {
		return plugin.ignoreCastItemDurability != null && plugin.ignoreCastItemDurability.contains(type);
	}

	public static boolean ignoreCastItemEnchants() {
		return plugin.ignoreCastItemEnchants;
	}

	public static boolean ignoreCastItemAmount() {
		return plugin.ignoreCastItemAmount;
	}

	public static boolean ignoreCastItemBreakability() {
		return plugin.ignoreCastItemBreakability;
	}

	public static boolean ignoreCastItemColor() {
		return plugin.ignoreCastItemColor;
	}

	public static boolean ignoreCastItemPotionType() {
		return plugin.ignoreCastItemPotionType;
	}

	public static boolean ignoreCastItemTitle() {
		return plugin.ignoreCastItemTitle;
	}

	public static boolean ignoreCastItemAuthor() {
		return plugin.ignoreCastItemAuthor;
	}

	public static boolean ignoreCastItemLore() {
		return plugin.ignoreCastItemLore;
	}

	public static boolean ignoreCastItemCustomModelData() {
		return plugin.ignoreCastItemCustomModelData;
	}

	public static boolean ignoreCastItemNames() {
		return plugin.ignoreCastItemNames;
	}

	public static boolean ignoreCastItemNameColors() {
		return plugin.ignoreCastItemNameColors;
	}

	public static boolean showStrCostOnMissingReagents() {
		return plugin.showStrCostOnMissingReagents;
	}

	public static boolean hideMagicItemTooltips() {
		return plugin.hideMagicItemTooltips;
	}

	public static boolean isManaSystemEnabled() {
		return plugin.enableManaSystem;
	}

	public static boolean isDebug() {
		return plugin.debug;
	}

	public static boolean isDebugNull() {
		return plugin.debugNull;
	}

	public static boolean isDebugNumberFormat() {
		return plugin.debugNumberFormat;
	}
	
	public static boolean areBowCycleButtonsReversed() {
		return plugin.reverseBowCycleButtons;
	}

	public static boolean canBowCycleSpellsSneaking() {
		return plugin.bowCycleSpellsSneaking;
	}

	public static boolean tabCompleteInternalNames() {
		return plugin.tabCompleteInternalNames;
	}

	public static boolean isCastingOnAnimate() {
		return plugin.castOnAnimate;
	}

	public static boolean isCyclingSpellsOnOffhandAction() {
		return plugin.cycleSpellsOnOffhandAction;
	}

	public static boolean canCastWithFist() {
		return plugin.allowCastWithFist;
	}

	public static boolean arePlayerSpellsSeparatedPerWorld() {
		return plugin.separatePlayerSpellsPerWorld;
	}

	public static int getSpellIconSlot() {
		return plugin.spellIconSlot;
	}

	public static int getGlobalRadius() {
		return plugin.globalRadius;
	}

	public static int getGlobalCooldown() {
		return plugin.globalCooldown;
	}

	public static String getStrSpellUsage() {
		return plugin.strSpellChange;
	}

	public static String getStrUnknownSpell() {
		return plugin.strUnknownSpell;
	}

	public static void setDebug(boolean debug) {
		plugin.debug = debug;
	}

	/**
	 * Gets the handler for no-magic zones.
	 * @return the no-magic zone handler
	 */
	public static NoMagicZoneManager getNoMagicZoneManager() {
		return plugin.noMagicZones;
	}

	public static BuffManager getBuffManager() {
		return plugin.buffManager;
	}

	/**
	 * Gets the mana handler, which handles all mana transactions.
	 * @return the mana handler
	 */
	public static ManaHandler getManaHandler() {
		return plugin.manaHandler;
	}

	public static VolatileCodeHandle getVolatileCodeHandler() {
		return plugin.volatileCodeHandle;
	}

	public static ExperienceBarManager getExpBarManager() {
		return plugin.expBarManager;
	}

	public static BossBarManager getBossBarManager() {
		return plugin.bossBarManager;
	}

	public static AttributeManager getAttributeManager() {
		return plugin.attributeManager;
	}

	public static ConditionManager getConditionManager() {
		return plugin.conditionManager;
	}

	public static PassiveManager getPassiveManager() {
		return plugin.passiveManager;
	}

	public static SpellEffectManager getSpellEffectManager() {
		return plugin.spellEffectManager;
	}

	public static MoneyHandler getMoneyHandler() {
		return plugin.moneyHandler;
	}

	public static MagicXpHandler getMagicXpHandler() {
		return plugin.magicXpHandler;
	}

	public static VariableManager getVariableManager() {
		return plugin.variableManager;
	}

	public static LifeLengthTracker getLifeLengthTracker() {
		return plugin.lifeLengthTracker;
	}

	public static Map<String, Spellbook> getSpellbooks() {
		return plugin.spellbooks;
	}

	public static Map<String, Spell> getSpells() {
		return plugin.spells;
	}

	public static List<Spell> getSpellsOrdered() {
		return plugin.spellsOrdered;
	}

	public static Map<String, Spell> getSpellNames() {
		return plugin.spellNames;
	}

	public static Map<String, Spell> getIncantations() {
		return plugin.incantations;
	}

	public static Map<String, Long> getProfilingTotalTime() {
		return plugin.profilingTotalTime;
	}

	public static Map<String, Integer> getProfilingRuns() {
		return plugin.profilingRuns;
	}

	public static EffectManager getEffectManager() {
		return plugin.effectManager;
	}

	public static PaperCommandManager getCommandManager() {
		return plugin.commandManager;
	}

	/**
	 * Sets the mana handler, which handles all mana transactions.
	 * @param handler the mana handler
	 */
	public static void setManaHandler(ManaHandler handler) {
		plugin.manaHandler.turnOff();
		plugin.manaHandler = handler;
	}

	/**
	 * Formats a string by performing the specified replacements.
	 * @param message the string to format
	 * @param replacements the replacements to make, in pairs.
	 * @return the formatted string
	 */
	public static String formatMessage(String message, String... replacements) {
		if (message == null) return null;

		String msg = message;
		for (int i = 0; i < replacements.length; i += 2) {
			if (replacements[i] == null) continue;

			if (replacements[i + 1] != null) {
				msg = msg.replace(replacements[i], replacements[i + 1]);
			} else {
				msg = msg.replace(replacements[i], "");
			}
		}
		return msg;
	}

	/**
	 * Sends a message to a player, first making the specified replacements. This method also does color replacement and has multi-line functionality.
	 * @param player the player to send the message to
	 * @param message the message to send
	 * @param replacements the replacements to be made, in pairs
	 */
	public static void sendMessageAndFormat(Player player, String message, String... replacements) {
		sendMessage(formatMessage(message, replacements), player, null);
	}

	public static void sendMessage(Player player, String message) {
		sendMessage(message, player, null);
	}

	/**
	 * Sends a message to a player. This method also does color replacement and has multi-line functionality.
	 * @param livingEntity the living entity to send the message to
	 * @param message the message to send
	 */
	public static void sendMessage(String message, LivingEntity livingEntity, String[] args) {
		if (!(livingEntity instanceof Player)) return;
		if (message != null && !message.isEmpty()) {
			// Do var replacements
			message = doArgumentAndVariableSubstitution(message, (Player) livingEntity, args);
			// Send messages
			for (String msg : message.split("\n")) {
				if (msg.isEmpty()) continue;
				livingEntity.sendMessage(Util.colorize(getTextColor() + msg));
			}
		}
	}

	private static Pattern chatVarMatchPattern = Pattern.compile("%var:[A-Za-z0-9_]+(:[0-9]+)?%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	public static String doSubjectVariableReplacements(Player player, String string) {
		if (string != null && plugin.variableManager != null && string.contains("%var")) {
			Matcher matcher = chatVarMatchPattern.matcher(string);
			while (matcher.find()) {
				String varText = matcher.group();
				String[] varData = varText.substring(5, varText.length() - 1).split(":");
				String val = plugin.variableManager.getStringValue(varData[0], player);
				String sval = varData.length == 1 ? TxtUtil.getStringNumber(val, -1) : TxtUtil.getStringNumber(val, Integer.parseInt(varData[1]));
				string = string.replace(varText, sval);
			}
		}
		return string;
	}

	private static Pattern chatPlayerVarMatchPattern = Pattern.compile("%playervar:" + RegexUtil.USERNAME_REGEXP + ":[A-Za-z0-9_]+(:[0-9]+)?%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	public static String doVariableReplacements(Player player, String string) {
		string = doSubjectVariableReplacements(player, string);
		if (string != null && plugin.variableManager != null && string.contains("%playervar")) {
			Matcher matcher = chatPlayerVarMatchPattern.matcher(string);
			while (matcher.find()) {
				String varText = matcher.group();
				String[] varData = varText.substring(11, varText.length() - 1).split(":");
				String variableOwnerName = varData[0];
				String val = plugin.variableManager.getStringValue(varData[1], variableOwnerName);
				String sval = varData.length == 2 ? TxtUtil.getStringNumber(val, -1) : TxtUtil.getStringNumber(val, Integer.parseInt(varData[2]));
				string = string.replace(varText, sval);
			}
		}
		return string;
	}

	//%arg:(index):defaultValue%
	private static Pattern argumentSubstitutionPattern = Pattern.compile("%arg:[0-9]+:[0-9a-zA-Z_]+%");
	public static String doArgumentSubstitution(String string, String[] args) {
		if (string != null && argumentSubstitutionPattern != null && string.contains("%arg")) {
			Matcher matcher = argumentSubstitutionPattern.matcher(string);
			while (matcher.find()) {
				String argText = matcher.group();
				String[] argData = argText.substring(5, argText.length() - 1).split(":");
				int argIndex = Integer.parseInt(argData[0]) - 1;
				String newValue;
				if (args != null && argIndex < args.length) newValue = args[argIndex];
				else newValue = argData[1];

				string = string.replace(argText, newValue);
			}
		}
		return string;
	}

	public static String doArgumentAndVariableSubstitution(String string, Player player, String[] args) {
		return doVariableReplacements(player, doArgumentSubstitution(string, args));
	}

	public static void registerEvents(final Listener listener) {
		registerEvents(listener, EventPriority.NORMAL);
	}

	public static void registerEvents(final Listener listener, EventPriority customPriority) {
		if (customPriority == null) customPriority = EventPriority.NORMAL;
		Method[] methods;
		try {
			methods = listener.getClass().getDeclaredMethods();
		} catch (NoClassDefFoundError e) {
			DebugHandler.debugNoClassDefFoundError(e);
			return;
		}
		for (final Method method : methods) {
			final EventHandler eh = method.getAnnotation(EventHandler.class);
			if (eh == null) continue;
			EventPriority priority = eh.priority();

			if (hasAnnotation(method, OverridePriority.class)) priority = customPriority;

			final Class<?> checkClass = method.getParameterTypes()[0];
			if (!Event.class.isAssignableFrom(checkClass) || method.getParameterTypes().length != 1) {
				plugin.getLogger().severe("Wrong method arguments used for event type registered");
				continue;
			}
			final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
			method.setAccessible(true);
			EventExecutor executor = new EventExecutor() {
				final String eventKey = plugin.enableProfiling ? "Event:" + listener.getClass().getName().replace("com.nisovin.magicspells.", "") + '.' + method.getName() + '(' + eventClass.getSimpleName() + ')' : null;

				@Override
				public void execute(Listener listener, Event event) {
					try {
						if (!eventClass.isAssignableFrom(event.getClass())) return;
						long start = System.nanoTime();
						method.invoke(listener, event);
						if (plugin.enableProfiling) {
							Long total = plugin.profilingTotalTime.get(eventKey);
							if (total == null) total = (long) 0;
							total += System.nanoTime() - start;
							plugin.profilingTotalTime.put(eventKey, total);
							Integer runs = plugin.profilingRuns.get(eventKey);
							if (runs == null) runs = 0;
							runs += 1;
							plugin.profilingRuns.put(eventKey, runs);
						}
					} catch (Exception ex) {
						handleException(ex);
					}
				}
			};
			plugin.getServer().getPluginManager().registerEvent(eventClass, listener, priority, executor, plugin, eh.ignoreCancelled());
		}
	}

	private static boolean hasAnnotation(Method m, Class<? extends Annotation> clazz) {
		return m.getAnnotation(clazz) != null;
	}

	public static int scheduleDelayedTask(final Runnable task, int delay) {
		return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, !plugin.enableErrorLogging ? task : () -> {
			try {
				task.run();
			} catch (Exception e) {
				handleException(e);
			}
		}, delay);
	}

	public static int scheduleRepeatingTask(final Runnable task, int delay, int interval) {
		return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, !plugin.enableErrorLogging ? task : () -> {
			try {
				task.run();
			} catch (Exception e) {
				handleException(e);
			}
		}, delay, interval);
	}

	public static void cancelTask(int taskId) {
		Bukkit.getScheduler().cancelTask(taskId);
	}

	public static void handleException(Exception ex) {
		if (plugin.enableErrorLogging) {
			plugin.getLogger().severe("AN EXCEPTION HAS OCCURED:");
			PrintWriter writer = null;
			try {
				File folder = new File(plugin.getDataFolder(), "errors");
				if (!folder.exists()) folder.mkdir();
				writer = new PrintWriter(new File(folder, System.currentTimeMillis() + ".txt"));
				Throwable t = ex;
				while (t != null) {
					plugin.getLogger().severe("    " + t.getMessage() + " (" + t.getClass().getName() + ')');
					t.printStackTrace(writer);
					writer.println();
					t = t.getCause();
				}
				plugin.getLogger().severe("This error has been saved in the errors folder");
				writer.println("Server version: " + Bukkit.getServer().getVersion());
				writer.println("MagicSpells version: " + plugin.getDescription().getVersion());
				writer.println("Error log date: " + new Date());
			} catch (Exception x) {
				plugin.getLogger().severe("ERROR HANDLING EXCEPTION");
				x.printStackTrace();
				ex.printStackTrace();
			} finally {
				if (writer != null) writer.close();
			}
		} else {
			ex.printStackTrace();
		}
	}

	public static void profilingReport() {
		if (plugin.profilingTotalTime == null) return;
		if (plugin.profilingRuns == null) return;

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new File(plugin.getDataFolder(), "profiling_report_" + System.currentTimeMillis() + ".txt"));
			long totalTime = 0;
			writer.println("Key\tRuns\tAvg\tTotal");
			for (String key : plugin.profilingTotalTime.keySet()) {
				long time = plugin.profilingTotalTime.get(key);
				int runs = plugin.profilingRuns.get(key);
				totalTime += time;
				writer.println(key + '\t' + runs + '\t' + (time / runs / 1000000F) + "ms\t" + (time / 1000000F) + "ms");
			}
			writer.println();
			writer.println("TOTAL TIME: " + (totalTime / 1000000F) + "ms");
		} catch (Exception ex) {
			error("Failed to save profiling report");
			handleException(ex);
		} finally {
			if (writer != null) writer.close();
		}
		plugin.profilingTotalTime.clear();
		plugin.profilingRuns.clear();
	}

	/**
	 * Writes a debug message to the console if the debug option is enabled.
	 * Uses debug level 2.
	 * @param message the message to write to the console
	 */
	public static void debug(String message) {
		debug(2, message);
	}

	/**
	 * Writes a debug message to the console if the debug option is enabled.
	 * @param level the debug level to log with
	 * @param message the message to write to the console
	 */
	public static void debug(int level, String message) {
		if (plugin.debug && level <= plugin.debugLevel) log(Level.INFO, message);
	}

	public static void log(String message) {
		log(Level.INFO, message);
	}

	public static void error(String message) {
		log(Level.WARNING, message);
	}

	/**
	 * Writes an error message to the console.
	 * @param level the error level
	 * @param message the error message
	 */
	public static void log(Level level, String message) {
		plugin.getLogger().log(level, message);
	}

	public static boolean profilingEnabled() {
		return plugin.enableProfiling;
	}

	public static void addProfile(String key, long time) {
		if (!plugin.enableProfiling) return;

		Long total = plugin.profilingTotalTime.get(key);
		if (total == null) total = (long) 0;
		total += time;
		plugin.profilingTotalTime.put(key, total);
		Integer runs = plugin.profilingRuns.get(key);
		if (runs == null) runs = 0;
		runs += 1;
		plugin.profilingRuns.put(key, runs);
	}

	/**
	 * Teaches a player a spell (adds it to their spellbook)
	 * @param player the player to teach
	 * @param spellName the spell name, either the in-game name or the internal name
	 * @return whether the spell was taught to the player
	 */
	public static boolean teachSpell(Player player, String spellName) {
		Spell spell = plugin.spellNames.get(spellName.toLowerCase());
		if (spell == null) {
			spell = plugin.spells.get(spellName.toLowerCase());
			if (spell == null) return false;
		}

		Spellbook spellbook = getSpellbook(player);

		if (spellbook == null || spellbook.hasSpell(spell) || !spellbook.canLearn(spell)) return false;

		// Call event
		SpellLearnEvent event = new SpellLearnEvent(spell, player, LearnSource.OTHER, null);
		EventUtil.call(event);
		if (event.isCancelled()) return false;

		spellbook.addSpell(spell);
		spellbook.save();
		return true;
	}

	public void unload() {
		// Turn off spells
		for (Spell spell : spells.values()) {
			spell.turnOff();
		}
		// Clear spell animations.
		for (SpellAnimation animation : SpellAnimation.getAnimations()) {
			animation.stop(false);
		}
		SpellAnimation.getAnimations().clear();

		// Save cooldowns
		if (cooldownsPersistThroughReload) {
			File file = new File(getDataFolder(), "cooldowns.txt");
			if (file.exists()) file.delete();
			try {
				FileWriter writer = new FileWriter(file);
				for (Spell spell : spells.values()) {
					Map<UUID, Long> cooldowns = spell.getCooldowns();
					for (UUID id : cooldowns.keySet()) {
						long cooldown = cooldowns.get(id);
						if (cooldown <= System.currentTimeMillis()) continue;
						writer.append(spell.getInternalName()).append(String.valueOf(':')).append(id.toString()).append(String.valueOf(':')).append(String.valueOf(cooldown)).append(String.valueOf('\n'));
					}
				}
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
				file.delete();
			}
		}

		// Turn off buff manager
		if (buffManager != null) {
			buffManager.turnOff();
			buffManager = null;
		}

		// Clear memory
		spells.clear();
		spells = null;
		spellNames.clear();
		spellNames = null;
		spellsOrdered.clear();
		spellsOrdered = null;
		spellbooks.clear();
		spellbooks = null;
		incantations.clear();
		incantations = null;
		entityNames.clear();
		entityNames = null;
		losTransparentBlocks.clear();
		losTransparentBlocks = null;
		ignoreCastItemDurability.clear();
		ignoreCastItemDurability = null;

		if (profilingRuns != null) {
			profilingRuns.clear();
			profilingRuns = null;
		}
		if (profilingTotalTime != null) {
			profilingTotalTime.clear();
			profilingTotalTime = null;
		}
		if (magicXpHandler != null) {
			magicXpHandler.saveAll();
			magicXpHandler = null;
		}
		if (manaHandler != null) {
			manaHandler.turnOff();
			manaHandler = null;
		}
		if (noMagicZones != null) {
			noMagicZones.turnOff();
			noMagicZones = null;
		}
		if (magicLogger != null) {
			magicLogger.disable();
			magicLogger = null;
		}
		if (variableManager != null) {
			variableManager.disable();
			variableManager = null;
		}
		if (bossBarManager != null) {
			bossBarManager.turnOff();
			bossBarManager = null;
		}
		if (volatileCodeHandle != null) {
			volatileCodeHandle = null;
		}

		config = null;
		strCantCast = null;
		strCantBind = null;
		moneyHandler = null;
		expBarManager = null;
		strOnCooldown = null;
		strWrongWorld = null;
		strSpellChange = null;
		strConsoleName = null;
		strUnknownSpell = null;
		strXpAutoLearned = null;
		lifeLengthTracker = null;
		strMissingReagents = null;
		strSpellChangeEmpty = null;
		soundFailOnCooldown = null;
		soundFailMissingReagents = null;

		// Remove star permissions (to allow new spells to be added to them)
		PluginManager pm = getServer().getPluginManager();
		pm.removePermission("magicspells.grant.*");
		pm.removePermission("magicspells.cast.*");
		pm.removePermission("magicspells.learn.*");
		pm.removePermission("magicspells.teach.*");

		// Unregister all listeners
		HandlerList.unregisterAll(this);

		// Cancel all tasks
		Bukkit.getScheduler().cancelTasks(this);

		ModifierSet.unload();
		PromptType.unloadDestructPromptData();
		CompatBasics.destructExemptionAssistant();

		classLoaders.clear();
		effectManager.dispose();
		effectManager = null;
		plugin = null;
	}

	@Override
	public void onDisable() {
		unload();
	}

	public ClassLoader getPluginClassLoader() {
		return getClassLoader();
	}

	public MagicConfig getMagicConfig() {
		return config;
	}

}

/*
 * TODO:
 * - Move NoMagicZoneWorldGuard outside of the core plugin
 */