package com.nisovin.magicspells;

import java.io.*;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.function.Predicate;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.annotation.Annotation;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;

import java.nio.file.Path;
import java.nio.file.Files;

import com.google.common.collect.Sets;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.LinkedHashMultimap;

import de.slikey.effectlib.EffectManager;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;

import org.jetbrains.annotations.NotNull;

import co.aikar.commands.PaperCommandManager;

import org.bukkit.*;
import org.bukkit.event.Event;
import org.bukkit.entity.Entity;
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

import me.clip.placeholderapi.PlaceholderAPI;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.events.*;
import com.nisovin.magicspells.handlers.*;
import com.nisovin.magicspells.listeners.*;
import com.nisovin.magicspells.util.managers.*;
import com.nisovin.magicspells.mana.ManaSystem;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.commands.MagicCommand;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.storage.StorageHandler;
import com.nisovin.magicspells.util.prompt.PromptType;
import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.commands.CommandHelpFilter;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.recipes.CustomRecipes;
import com.nisovin.magicspells.util.ai.CustomGoalsManager;
import com.nisovin.magicspells.handlers.DeprecationHandler;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.storage.types.TXTFileStorage;
import com.nisovin.magicspells.volatilecode.ManagerVolatile;
import com.nisovin.magicspells.volatilecode.VolatileCodeHandle;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;
import com.nisovin.magicspells.spelleffects.trackers.EffectTracker;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.variables.variabletypes.GlobalVariable;
import com.nisovin.magicspells.spelleffects.trackers.AsyncEffectTracker;
import com.nisovin.magicspells.spelleffects.effecttypes.EffectLibEffect;
import com.nisovin.magicspells.util.config.expression.ExpressionDictionary;
import com.nisovin.magicspells.variables.variabletypes.GlobalStringVariable;
import com.nisovin.magicspells.variables.variabletypes.PlayerStringVariable;
import com.nisovin.magicspells.spells.targeted.cleanse.util.CleanserManager;

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
	private SetMultimap<String, Spell> spellsByTag; // Map of tag -> spell

	private List<Spell> spellsOrdered; // Spells ordered

	// Container vars
	private ManaHandler manaHandler;
	private MoneyHandler moneyHandler;
	private MagicXpHandler magicXpHandler;
	private StorageHandler storageHandler;
	private DeprecationHandler deprecationHandler;
	private VolatileCodeHandle volatileCodeHandle;

	private BuffManager buffManager;
	private EffectManager effectManager;
	private BossBarManager bossBarManager;
	private VariableManager variableManager;
	private PassiveManager passiveManager;
	private SpellEffectManager spellEffectManager;
	private ConditionManager conditionManager;
	private NoMagicZoneManager zoneManager;
	private CleanserManager cleanserManager;
	private CustomGoalsManager customGoalsManager;
	private PaperCommandManager commandManager;
	private ExperienceBarManager expBarManager;

	private ExpressionDictionary expressionDictionary;

	private MagicConfig config;
	private MagicLogger magicLogger;
	private LifeLengthTracker lifeLengthTracker;

	private boolean debug;
	private boolean debugNull;
	private boolean debugNumberFormat;
	private boolean tabCompleteInternalNames;
	private boolean terminateEffectlibInstances;

	private boolean enableProfiling;
	private boolean enableErrorLogging;

	private boolean hideMagicItemTooltips;
	private boolean ignoreCastItemNames;
	private boolean ignoreCastItemAmount;
	private boolean ignoreCastItemEnchants;
	private boolean ignoreCastItemNameColors;
	private boolean ignoreCastItemBreakability;
	private boolean ignoreCastItemColor;
	private boolean ignoreCastItemPotionType;
	private boolean ignoreCastItemTitle;
	private boolean ignoreCastItemAuthor;
	private boolean ignoreCastItemLore;
	private boolean ignoreCastItemCustomModelData;

	private boolean castOnAnimate;
	private boolean enableManaSystem;
	private boolean ignoreCastPerms;
	private boolean opsHaveAllSpells;
	private boolean ignoreGrantPerms;
	private boolean checkWorldPvpFlag;
	private boolean allowCastWithFist;
	private boolean castWithLeftClick;
	private boolean castWithRightClick;
	private boolean respectItemCooldowns;
	private boolean reverseBowCycleButtons;
	private boolean bowCycleSpellsSneaking;
	private boolean castBoundBowSpellsFromOffhand;
	private boolean allowCycleToNoSpell;

	private boolean checkScoreboardTeams;
	private boolean defaultAllPermsFalse;
	private boolean enableTempGrantPerms;
	private boolean ignoreDefaultBindings;
	private boolean useExpBarAsCastTimeBar;
	private boolean alwaysShowMessageOnCycle;
	private boolean onlyCycleToCastableSpells;
	private boolean ignoreGrantPermsFakeValue;
	private boolean cycleSpellsOnOffhandAction;
	private boolean separatePlayerSpellsPerWorld;
	private boolean showStrCostOnMissingReagents;
	private boolean cooldownsPersistThroughReload;
	private boolean allowAnticheatIntegrations;

	private int debugLevelOriginal;
	private int debugLevel;
	private int spellIconSlot;
	private int globalRadius;
	private int errorLogLimit;
	private int globalCooldown;
	private int broadcastRange;
	private int effectlibInstanceLimit;

	private long lastReloadTime = 0;

	private ChatColor textColor;

	private double losRaySize;
	private boolean losIgnorePassableBlocks;
	private FluidCollisionMode losFluidCollisionMode;

	// Strings
	private String strCantCast;
	private String strCantBind;
	private String strWrongWorld;
	private String strOnCooldown;
	private String strSpellChange;
	private String strUnknownSpell;
	private String strXpAutoLearned;
	private String strMissingReagents;
	private String strSpellChangeEmpty;
	private String consoleName;

	private String soundFailOnCooldown;
	private String soundFailMissingReagents;

	private boolean loaded = false;

	@Override
	public void onEnable() {
		load();

		Metrics metrics = new Metrics(this, 892);

		metrics.addCustomChart(new DrilldownPie("spells", () -> {
			Map<String, Map<String, Integer>> map = new HashMap<>();
			if (spells == null) return map;

			for (Spell spell : spells.values()) {
				String name = spell.getClass().getName();
				if (!name.startsWith("com.nisovin.magicspells.spells")) continue;
				name = name.replace("com.nisovin.magicspells.spells.", "");

				String[] typeSplit = name.split("\\.", 2);
				String formalPackage = typeSplit[0].substring(0, 1).toUpperCase() + typeSplit[0].substring(1);

				String spellPackage = (typeSplit.length == 1 ? "General" : formalPackage) + " Spells";
				String spellClass = typeSplit.length == 1 ? typeSplit[0] : typeSplit[1];

				map.computeIfAbsent(spellPackage, key -> new HashMap<>());
				map.get(spellPackage).compute(spellClass, (k, v) -> (v == null ? 0 : v) + 1);
			}
			return map;
		}));
		metrics.addCustomChart(new AdvancedPie("passive_listeners", () -> {
			IntMap<String> map = new IntMap<>();
			if (spells == null) return map;

			for (Spell spell : spells.values()) {
				if (!spell.getClass().getName().startsWith("com.nisovin.magicspells.spells")) continue;
				if (!(spell instanceof PassiveSpell passiveSpell)) continue;

				for (PassiveListener listener : passiveSpell.getPassiveListeners()) {
					String name = listener.getClass().getSimpleName();
					map.increment(name.substring(0, name.lastIndexOf("Listener")));
				}
			}
			return map;
		}));
		metrics.addCustomChart(new SimplePie("reload_time", () -> "<" + (lastReloadTime - lastReloadTime % 500 + 500) + " ms"));
	}

	public void load() {
		plugin = this;

		deprecationHandler = new DeprecationHandler();

		effectManager = new EffectManager(this);
		effectManager.enableDebug(debug);

		commandManager = new PaperCommandManager(plugin);

		// Create storage stuff
		spells = new HashMap<>();
		spellNames = new HashMap<>();
		spellsOrdered = new ArrayList<>();
		spellbooks = new HashMap<>();
		spellsByTag = LinkedHashMultimap.create();
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
			if (!new File(getDataFolder(), "defaults.yml").exists()) saveResource("defaults.yml", false);
		}
		config = new MagicConfig();
		if (!config.isLoaded()) {
			MagicSpells.log(Level.SEVERE, "Error in config file, stopping config load");
			return;
		}

		// Construct volatile handler
		volatileCodeHandle = ManagerVolatile.constructVolatileCodeHandler();

		String path = "general.";
		String manaPath = "mana.";

		debug = config.getBoolean(path + "debug", false);
		debugNull = config.getBoolean(path + "debug-null", true);
		debugNumberFormat = config.getBoolean(path + "debug-number-format", true);
		debugLevelOriginal = config.getInt(path + "debug-level", 3);
		debugLevel = debugLevelOriginal;

		tabCompleteInternalNames = config.getBoolean(path + "tab-complete-internal-names", false);
		terminateEffectlibInstances = config.getBoolean(path + "terminate-effectlib-instances", true);

		enableErrorLogging = config.getBoolean(path + "enable-error-logging", true);
		errorLogLimit = config.getInt(path + "error-log-limit", -1);
		enableProfiling = config.getBoolean(path + "enable-profiling", false);
		textColor = ChatColor.getByChar(config.getString(path + "text-color", ChatColor.DARK_AQUA.getChar() + ""));
		broadcastRange = config.getInt(path + "broadcast-range", 20);
		effectlibInstanceLimit = config.getInt(path + "effectlib-instance-limit", 20000);

		opsHaveAllSpells = config.getBoolean(path + "ops-have-all-spells", true);
		defaultAllPermsFalse = config.getBoolean(path + "default-all-perms-false", false);
		ignoreGrantPerms = config.getBoolean(path + "ignore-grant-perms", false);
		ignoreGrantPermsFakeValue = config.getBoolean(path + "ignore-grant-perms-fake-value", true);
		ignoreCastPerms = config.getBoolean(path + "ignore-cast-perms", false);
		enableTempGrantPerms = config.getBoolean(path + "enable-tempgrant-perms", true);

		separatePlayerSpellsPerWorld = config.getBoolean(path + "separate-player-spells-per-world", false);
		allowCycleToNoSpell = config.getBoolean(path + "allow-cycle-to-no-spell", false);
		reverseBowCycleButtons = config.getBoolean(path + "reverse-bow-cycle-buttons", true);
		castBoundBowSpellsFromOffhand = config.getBoolean(path + "cast-bound-bow-spells-from-offhand", false);
		bowCycleSpellsSneaking = config.getBoolean(path + "bow-cycle-spells-sneaking", true);
		alwaysShowMessageOnCycle = config.getBoolean(path + "always-show-message-on-cycle", false);
		onlyCycleToCastableSpells = config.getBoolean(path + "only-cycle-to-castable-spells", true);
		spellIconSlot = config.getInt(path + "spell-icon-slot", -1);
		allowCastWithFist = config.getBoolean(path + "allow-cast-with-fist", false);
		castWithLeftClick = config.getBoolean(path + "cast-with-left-click", true);
		castWithRightClick = config.getBoolean(path + "cast-with-right-click", false);
		respectItemCooldowns = config.getBoolean(path + "respect-item-cooldowns", false);
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
		losRaySize = config.getDouble(path + "los-ray-size", 0.2);
		losIgnorePassableBlocks = config.getBoolean(path + "los-ignore-passable-blocks", true);
		try {
			losFluidCollisionMode = FluidCollisionMode.valueOf(config.getString(path + "los-fluid-collision-mode", "ALWAYS").toUpperCase());
		} catch (IllegalArgumentException e) {
			losFluidCollisionMode = FluidCollisionMode.ALWAYS;
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
				EntityType entityType = MobUtil.getEntityType(key);
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
		strXpAutoLearned = config.getString(path + "str-xp-auto-learned", "You have learned the %s spell!");
		consoleName = config.getString(path + "console-name", "Admin");

		allowAnticheatIntegrations = config.getBoolean(path + "allow-anticheat-integrations", false);

		enableManaSystem = config.getBoolean(manaPath + "enable-mana-system", false);

		// Create handling objects
		zoneManager = new NoMagicZoneManager();
		cleanserManager = new CleanserManager();
		customGoalsManager = new CustomGoalsManager();
		buffManager = new BuffManager(config.getInt(path + "buff-check-interval", 1));
		expBarManager = new ExperienceBarManager();
		bossBarManager = new BossBarManager();
		if (CompatBasics.pluginEnabled("Vault")) moneyHandler = new MoneyHandler();
		lifeLengthTracker = new LifeLengthTracker();
		expressionDictionary = new ExpressionDictionary();

		// Call loading event
		Bukkit.getPluginManager().callEvent(new MagicSpellsLoadingEvent(this));

		// Load magic items
		log("Loading magic items...");
		hideMagicItemTooltips = config.getBoolean(path + "hide-magic-items-tooltips", false);
		if (hideMagicItemTooltips) log("... hiding tooltips!");

		MagicItems.getMagicItems().clear();
		String itemStr = "magic-items";
		if (config.contains(path + itemStr)) {
			Set<String> magicItems = config.getKeys(path + itemStr);
			String str;
			MagicItem magicItem;
			ConfigurationSection section;
			if (magicItems != null) {
				for (String key : magicItems) {
					if (config.isString(path + itemStr + "." + key)) {
						str = config.getString(path + itemStr + "." + key, null);
						if (str == null) continue;

						magicItem = MagicItems.getMagicItemFromString(str);
						if (magicItem != null) MagicItems.getMagicItems().put(key, magicItem);
						else MagicSpells.error("Invalid magic item: " + key + ": " + str);

					} else if (config.isSection(path + itemStr + "." + key)) {
						section = config.getSection(path + itemStr + "." + key);
						if (section == null) continue;

						magicItem = MagicItems.getMagicItemFromSection(section);
						if (magicItem != null) MagicItems.getMagicItems().put(key, magicItem);
						else MagicSpells.error("Invalid magic item: " + key + ": (section)");

					} else MagicSpells.error("Invalid magic item: " + key);
				}
			}
		}
		log("..." + MagicItems.getMagicItems().size() + " magic items loaded");

		if (config.isSection(path + "recipes")) {
			log("Loading recipes...");
			CustomRecipes.load(config.getSection(path + "recipes"));
			log("..." + CustomRecipes.getRecipes().size() + " recipes loaded");
		}

		// Load spells
		loadSpells();
		if (spells.isEmpty()) {
			MagicSpells.error("No spells loaded!");
			return;
		}

		initPermissions();

		// Load xp system
		if (config.getBoolean(path + "enable-magic-xp", false)) {
			log("Loading xp system...");
			magicXpHandler = new MagicXpHandler(this, config);
			log("...xp system loaded");
		}

		// Load player data using a storage handler
		log("Initializing storage handler...");
		storageHandler = new TXTFileStorage(plugin);
		//storageHandler = new DatabaseStorage(plugin, new SQLiteDatabase(plugin, "spellbooks.db"));
		storageHandler.initialize();
		log("...done");

		// Load online player spellbooks
		log("Loading online player spellbooks...");
		Util.forEachPlayerOnline(pl -> spellbooks.put(pl.getName(), new Spellbook(pl)));
		log("...done");

		// Load saved cooldowns
		if (cooldownsPersistThroughReload) {
			File file = new File(getDataFolder(), "cooldowns.txt");
			Scanner scanner = null;
			if (file.exists()) {
				try {
					scanner = new Scanner(file);
					String line;
					String[] data;
					long cooldown;
					Spell spell;
					while (scanner.hasNext()) {
						line = scanner.nextLine();
						if (line.isEmpty()) continue;
						data = line.split(":");
						cooldown = Long.parseLong(data[2]);
						if (cooldown > System.currentTimeMillis()) {
							spell = getSpellByInternalName(data[0]);
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

		// Load listeners
		log("Loading cast listeners...");
		registerEvents(new MagicPlayerListener());
		registerEvents(new MagicSpellListener());
		registerEvents(new CastListener());

		LeftClickListener leftClickListener = new LeftClickListener();
		if (leftClickListener.hasLeftClickCastItems()) registerEvents(leftClickListener);

		RightClickListener rightClickListener = new RightClickListener();
		if (rightClickListener.hasRightClickCastItems()) registerEvents(rightClickListener);

		ConsumeListener consumeListener = new ConsumeListener();
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
		commandManager.setValidNamePredicate(string -> true);
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

	private void initializeSpells() {
		log("Initializing spells...");

		Iterator<Map.Entry<String, Spell>> it = spells.entrySet().iterator();
		while (it.hasNext()) {
			Spell spell = it.next().getValue();

			DependsOn dependsOn = spell.getClass().getAnnotation(DependsOn.class);
			if (dependsOn != null && !Util.checkPluginsEnabled(dependsOn.value())) {
				spellsOrdered.remove(spell);
				it.remove();

				MagicSpells.error(spell.getClass().getSimpleName() + " '" + spell.internalName + "' could not be loaded.");
				continue;
			}

			spellNames.put(Util.getPlainString(Util.getMiniMessage(spell.getName().toLowerCase())), spell);

			for (String tag : spell.getTags())
				spellsByTag.put(tag, spell);

			String[] aliases = spell.getAliases();
			if (aliases != null) {
				for (String alias : aliases) {
					if (!spellNames.containsKey(alias.toLowerCase())) spellNames.put(alias.toLowerCase(), spell);
				}
			}

			List<String> incs = spell.getIncantations();
			if (incs != null && !incs.isEmpty()) {
				for (String s : incs) {
					incantations.put(s.toLowerCase(), spell);
				}
			}
		}

		spells.values().forEach(Spell::initialize);

		zoneManager.load(config);
		if (!incantations.isEmpty()) registerEvents(new MagicChatListener());

		log("...done");
	}

	private void loadExternalData() {
		log("Loading external data...");

		initializeSpells();
		loadVariables();
		loadSpellEffects();
		loadConditions();
		loadPassiveListeners();

		log("...done");

		// Call loaded event
		Bukkit.getPluginManager().callEvent(new MagicSpellsLoadedEvent(this));
		loaded = true;

		deprecationHandler.printDeprecationNotices();

		log("MagicSpells loading complete!");
	}

	private void loadVariables() {
		// Load variables
		log("Loading variables...");
		String path = "general.";
		ConfigurationSection varSec = null;
		if (config.contains(path + "variables") && config.isSection(path + "variables")) {
			varSec = config.getSection(path + "variables");
		}
		variableManager = new VariableManager();

		// Call variable event
		Bukkit.getPluginManager().callEvent(new VariablesLoadingEvent(plugin, variableManager));

		variableManager.loadVariables(varSec);

		spells.values().forEach(Spell::initializeVariables);

		if (!variableManager.getVariables().isEmpty()) registerEvents(new VariableListener());

		log("...variable meta types loaded: " + variableManager.getMetaVariables().size());
		log("...variable types loaded: " + variableManager.getVariableTypes().size());
		log("...variables loaded: " + (variableManager.getVariables().size() - variableManager.getMetaVariables().size()));
	}

	private void loadSpellEffects() {
		// Load spell effects
		log("Loading spell effect types...");
		spellEffectManager = new SpellEffectManager();

		// Call spell effect event
		Bukkit.getPluginManager().callEvent(new SpellEffectsLoadingEvent(plugin, spellEffectManager));

		spells.values().forEach(Spell::initializeSpellEffects);

		log("...spell effect types loaded: " + spellEffectManager.getSpellEffects().size());
	}

	private void loadConditions() {
		// Load conditions
		log("Loading conditions...");
		conditionManager = new ConditionManager();

		// Call condition event
		Bukkit.getPluginManager().callEvent(new ConditionsLoadingEvent(plugin, conditionManager));

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

	private void loadPassiveListeners() {
		// Load passive listeners
		log("Loading passive listeners...");
		passiveManager = new PassiveManager();

		// Call passive event
		Bukkit.getPluginManager().callEvent(new PassiveListenersLoadingEvent(plugin, passiveManager));

		for (Spell spell : spells.values()) {
			if (!(spell instanceof PassiveSpell)) continue;
			((PassiveSpell) spell).initializeListeners();
		}

		log("...passive listeners loaded: " + passiveManager.getListeners().size());
	}

	private static final int LONG_LOAD_THRESHOLD = 50;
	// DEBUG INFO: level 2, loaded spell spellName
	private void loadSpells() {
		log("Loading spells...");

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

		long startTime;
		long elapsed;
		long finalElapsed;

		String className;
		String permName;

		Constructor<? extends Spell> constructor;
		Class<? extends Spell> spellClass;
		Spell spell;

		for (String spellName : spellKeys) {
			if (!config.getBoolean("spells." + spellName + ".enabled", true)) continue;
			startTime = System.currentTimeMillis();
			className = "";
			if (config.contains("spells." + spellName + ".spell-class")) className = config.getString("spells." + spellName + ".spell-class", "");

			if (className == null || className.isEmpty()) {
				error("Spell '" + spellName + "' does not have a spell-class property");
				continue;
			}

			if (className.startsWith(".")) className = "com.nisovin.magicspells.spells" + className;

			constructor = constructors.get(className);

			// Load spell class
			if (constructor == null) {
				for (ClassLoader cl : classLoaders) {
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

			try {
				spell = constructor.newInstance(config, spellName);
			} catch (Exception e) {
				error("Unable to load spell " + spellName + " (general error)");
				e.printStackTrace();
				continue;
			}

			spells.put(spellName.toLowerCase(), spell);
			spellsOrdered.add(spell);

			// Done
			debug(2, "Loaded spell: " + spellName);

			elapsed = System.currentTimeMillis() - startTime;
			if (elapsed > LONG_LOAD_THRESHOLD) getLogger().warning("LONG SPELL LOAD TIME: " + spellName + ": " + elapsed + "ms");
		}

		finalElapsed = System.currentTimeMillis() - startTimePre;
		if (lastReloadTime != 0) getLogger().warning("Loaded in " + finalElapsed + "ms (previously " + lastReloadTime + " ms)");
		getLogger().warning("Need help? Check out our discord: https://discord.magicspells.dev/");
		lastReloadTime = finalElapsed;

		log("...spells loaded: " + spells.size());
	}

	private void initPermissions() {
		log("Initializing permissions...");

		List<Permission> permissions = new ArrayList<>();
		Map<String, Boolean> grantChildren = new HashMap<>();
		Map<String, Boolean> learnChildren = new HashMap<>();
		Map<String, Boolean> castChildren = new HashMap<>();
		Map<String, Boolean> teachChildren = new HashMap<>();
		Map<String, Boolean> advancedChildren = new HashMap<>();

		PermissionDefault defaultValue = defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE;

		// Spell permissions
		spellsOrdered.stream()
			.filter(Predicate.not(Spell::isHelperSpell))
			.collect(Collectors.toMap(s -> s.getPermissionName().toLowerCase(), Spell::isAlwaysGranted, Boolean::logicalOr))
			.forEach((permissionName, alwaysGranted) -> {
				if (permissionName.equals("*")) return;

				if (!alwaysGranted) {
					String grant = Perm.GRANT + permissionName;

					permissions.add(new Permission(grant, PermissionDefault.FALSE));
					grantChildren.put(grant, true);
				}

				if (areTempGrantPermsEnabled())
					permissions.add(new Permission(Perm.TEMPGRANT + permissionName, PermissionDefault.FALSE));

				String learn = Perm.LEARN + permissionName;
				permissions.add(new Permission(learn, defaultValue));
				learnChildren.put(learn, true);

				String cast = Perm.CAST + permissionName;
				permissions.add(new Permission(cast, defaultValue));
				castChildren.put(cast, true);

				String teach = Perm.TEACH + permissionName;
				permissions.add(new Permission(teach, defaultValue));
				teachChildren.put(teach, true);
			});

		// Advanced permissions
		permissions.add(new Permission(Perm.ADVANCED_LIST.getNode(), PermissionDefault.FALSE));
		advancedChildren.put(Perm.ADVANCED_LIST.getNode(), true);

		permissions.add(new Permission(Perm.ADVANCED_FORGET.getNode(), PermissionDefault.FALSE));
		advancedChildren.put(Perm.ADVANCED_FORGET.getNode(), true);

		permissions.add(new Permission(Perm.ADVANCED_SCROLL.getNode(), PermissionDefault.FALSE));
		advancedChildren.put(Perm.ADVANCED_SCROLL.getNode(), true);

		boolean opsIgnoreReagents = config.getBoolean("general.ops-ignore-reagents", false);
		boolean opsIgnoreCooldowns = config.getBoolean("general.ops-ignore-cooldowns", false);
		boolean opsIgnoreCastTimes = config.getBoolean("general.ops-ignore-cast-times", false);

		// Op permissions
		permissions.add(new Permission(
			Perm.NO_REAGENTS.getNode(),
			"Allows casting without needing reagents",
			opsIgnoreReagents ? PermissionDefault.OP : PermissionDefault.FALSE
		));

		permissions.add(new Permission(
			Perm.NO_COOLDOWN.getNode(),
			"Allows casting without being affected by cooldowns",
			opsIgnoreCooldowns ? PermissionDefault.OP : PermissionDefault.FALSE
		));

		permissions.add(new Permission(
			Perm.NO_CAST_TIME.getNode(),
			"Allows casting without being affected by cast times",
			opsIgnoreCastTimes ? PermissionDefault.OP : PermissionDefault.FALSE
		));

		permissions.add(new Permission(
			Perm.NO_TARGET.getNode(),
			"Prevents being targeted by any targeted spells",
			PermissionDefault.FALSE
		));

		permissions.add(new Permission(
			Perm.SILENT.getNode(),
			"Prevents cast messages from being broadcast to players",
			PermissionDefault.FALSE
		));

		// Command permissions
		permissions.add(new Permission(Perm.COMMAND_HELP.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_RELOAD.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_RELOAD_SPELLBOOK.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_RELOAD_EFFECTLIB.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_RESET_COOLDOWN.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_MANA_SHOW.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_MANA_RESET.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_MANA_SET_MAX.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_MANA_ADD.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_MANA_SET.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_MANA_UPDATE_RANK.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_VARIABLE_SHOW.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_VARIABLE_MODIFY.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_MAGIC_ITEM.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_UTIL_DOWNLOAD.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_UTIL_UPDATE.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_UTIL_SAVE_SKIN.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_PROFILE_REPORT.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_DEBUG.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_TASKINFO.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_MAGICXP.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_CAST_POWER.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_CAST_SELF.getNode(), PermissionDefault.TRUE));
		permissions.add(new Permission(Perm.COMMAND_CAST_AS.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_CAST_ON.getNode(), PermissionDefault.OP));
		permissions.add(new Permission(Perm.COMMAND_CAST_AT.getNode(), PermissionDefault.OP));

		permissions.add(new Permission(Perm.GRANT + "*", PermissionDefault.FALSE, grantChildren));
		permissions.add(new Permission(Perm.LEARN + "*", defaultValue, learnChildren));
		permissions.add(new Permission(Perm.CAST + "*", defaultValue, castChildren));
		permissions.add(new Permission(Perm.TEACH + "*", defaultValue, teachChildren));
		permissions.add(new Permission(Perm.ADVANCED + "*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.OP, advancedChildren));

		PluginManager pluginManager = Bukkit.getPluginManager();
		permissions.forEach(pluginManager::removePermission);
		//noinspection UnstableApiUsage
		pluginManager.addPermissions(permissions);

		log("...permissions initialized.");
	}

	public static List<ClassLoader> getClassLoaders() {
		return classLoaders;
	}

	// Create class loader with jar files within the directory
	public ClassLoader createSpellClassLoader(File dataFolder) {
		final List<File> jarList = new ArrayList<>();
		for (File file : dataFolder.listFiles()) {
			if (file.getName().endsWith(".jar")) jarList.add(file);
		}
		return createSpellClassLoader(jarList, dataFolder);
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

	public static void resetEffectlib() {
		for (Spell s : MagicSpells.getSpells().values()) {
			Set<EffectTracker> effectTrackers = s.getEffectTrackers();
			for (EffectTracker tracker : effectTrackers) {
				if (!(tracker.getEffect() instanceof EffectLibEffect)) continue;
				tracker.stop();
			}

			Set<AsyncEffectTracker> asyncEffectTrackers = s.getAsyncEffectTrackers();
			for (AsyncEffectTracker tracker : asyncEffectTrackers) {
				if (!(tracker.getEffect() instanceof EffectLibEffect)) continue;
				tracker.stop();
			}
		}
	}

	public static boolean isLoaded() {
		return plugin != null && plugin.loaded;
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
	 * @return {@link Spell} found, or null if no spell with that name was found
	 */
	public static Spell getSpellByInternalName(String spellName) {
		return plugin.spells.get(spellName.toLowerCase());
	}

	/**
	 * Gets a spell by its in-game name (<code>aliases</code>, the name specified with the <code>name</code>
	 * config option, or the internal spell name if <code>name</code> was not specified).
	 * @param spellName the in-game name of the spell to find
	 * @return {@link Spell} found, or null if no spell with that name was found
	 */
	public static Spell getSpellByInGameName(String spellName) {
		return plugin.spellNames.get(spellName.toLowerCase());
	}

	/**
	 * Gets a spell by its internal name, <code>aliases</code> or <code>name</code>).
	 * @param spellName the name of the spell to find
	 * @return {@link Spell} found, or null
	 */
	public static Spell getSpellByName(String spellName) {
		Spell spell = getSpellByInternalName(spellName);
		return spell == null ? getSpellByInGameName(spellName) : spell;
	}

	/**
	 * Gets a player's spellbook, which contains known spells and handles spell permissions.
	 * If a player does not have a spellbook, one will be created.
	 * @param player the player to get a spellbook for
	 * @return the player's spellbook
	 */
	public static Spellbook getSpellbook(Player player) {
		return plugin.spellbooks.computeIfAbsent(player.getName(), playerName -> new Spellbook(player));
	}

	public static ChatColor getTextColor() {
		return plugin.textColor;
	}

	/**
	 * Gets a list of blocks that are considered transparent
	 * @return set of block types
	 */
	public static Set<Material> getTransparentBlocks() {
		return plugin.losTransparentBlocks;
	}

	public static double getLosRaySize() {
		return plugin.losRaySize;
	}

	public static boolean isIgnoringPassableBlocks() {
		return plugin.losIgnorePassableBlocks;
	}

	public static FluidCollisionMode getFluidCollisionMode() {
		return plugin.losFluidCollisionMode;
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

	public static boolean castBoundBowSpellsFromOffhand() {
		return plugin.castBoundBowSpellsFromOffhand;
	}

	public static boolean tabCompleteInternalNames() {
		return plugin.tabCompleteInternalNames;
	}

	public static boolean shouldTerminateEffectlibEffects() {
		return plugin.terminateEffectlibInstances;
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

	public static int getBroadcastRange() {
		return plugin.broadcastRange;
	}

	public static int getEffectlibInstanceLimit() {
		return plugin.effectlibInstanceLimit;
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

	public static int getDebugLevelOriginal() {
		return plugin.debugLevelOriginal;
	}

	public static int getErrorLogLimit() {
		return plugin.errorLogLimit;
	}

	public static void setDebug(boolean debug) {
		plugin.debug = debug;
	}

	public static void setDebugLevel(int level) {
		plugin.debugLevel = level;
	}

	public static boolean hasProfilingEnabled() {
		return plugin.enableProfiling;
	}

	public static boolean hasAnticheatIntegrations() {
		return plugin.allowAnticheatIntegrations;
	}

	public static boolean hasCastPermsIgnored() {
		return plugin.ignoreCastPerms;
	}

	public static boolean grantOpsAllSpells() {
		return plugin.opsHaveAllSpells;
	}

	public static boolean ignoreGrantPerms() {
		return plugin.ignoreGrantPerms;
	}

	public static boolean canCastWithLeftClick() {
		return plugin.castWithLeftClick;
	}

	public static boolean canCastWithRightClick() {
		return plugin.castWithRightClick;
	}

	public static boolean isRespectingItemCooldowns() {
		return plugin.respectItemCooldowns;
	}

	public static boolean profilingEnabled() {
		return plugin.enableProfiling;
	}

	public static boolean errorLoggingEnabled() {
		return plugin.enableErrorLogging;
	}

	public static boolean canCycleToNoSpell() {
		return plugin.allowCycleToNoSpell;
	}

	public static boolean checkScoreboardTeams() {
		return plugin.checkScoreboardTeams;
	}

	public static boolean areTempGrantPermsEnabled() {
		return plugin.enableTempGrantPerms;
	}

	public static boolean checkWorldPvpFlag() {
		return plugin.checkWorldPvpFlag;
	}

	public static boolean ignoreGrantPermsFakeValue() {
		return plugin.ignoreGrantPermsFakeValue;
	}

	public static boolean ignoreDefaultBindings() {
		return plugin.ignoreDefaultBindings;
	}

	public static boolean useExpBarAsCastTimeBar() {
		return plugin.useExpBarAsCastTimeBar;
	}

	public static boolean showMessageOnCycle() {
		return plugin.alwaysShowMessageOnCycle;
	}

	public static boolean cycleToCastableSpells() {
		return plugin.onlyCycleToCastableSpells;
	}

	public static boolean cooldownsPersistThroughReload() {
		return plugin.cooldownsPersistThroughReload;
	}

	public static String getCantCastMessage() {
		return plugin.strCantCast;
	}

	public static String getCantBindMessage() {
		return plugin.strCantBind;
	}

	public static String getWrongWorldMessage() {
		return plugin.strWrongWorld;
	}

	public static String getOnCooldownMessage() {
		return plugin.strOnCooldown;
	}

	public static String getSpellChangeMessage() {
		return plugin.strSpellChange;
	}

	public static String getUnknownSpellMessage() {
		return plugin.strUnknownSpell;
	}

	public static String getXpAutoLearnedMessage() {
		return plugin.strXpAutoLearned;
	}

	public static String getMissingReagentsMessage() {
		return plugin.strMissingReagents;
	}

	public static String getSpellChangeEmptyMessage() {
		return plugin.strSpellChangeEmpty;
	}

	public static String getConsoleName() {
		return plugin.consoleName;
	}

	public static String getCooldownSound() {
		return plugin.soundFailOnCooldown;
	}

	public static String getMissingReagentsSound() {
		return plugin.soundFailMissingReagents;
	}

	/**
	 * Gets the handler for no-magic zones.
	 * @return the no-magic zone handler
	 */
	public static NoMagicZoneManager getNoMagicZoneManager() {
		return plugin.zoneManager;
	}

	/**
	 * Gets the handler for CleanseSpell cleansers.
	 * @return the CleanseSpell cleanser handler
	 */
	public static CleanserManager getCleanserManager() {
		return plugin.cleanserManager;
	}

	/**
	 * Gets the handler for mob goals.
	 * @return the mob goals handler
	 */
	public static CustomGoalsManager getCustomGoalsManager() {
		return plugin.customGoalsManager;
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

	public static ConditionManager getConditionManager() {
		return plugin.conditionManager;
	}

	public static PassiveManager getPassiveManager() {
		return plugin.passiveManager;
	}

	public static SpellEffectManager getSpellEffectManager() {
		return plugin.spellEffectManager;
	}

	public static ExpressionDictionary getExpressionDictionary() {
		return plugin.expressionDictionary;
	}

	public static MoneyHandler getMoneyHandler() {
		return plugin.moneyHandler;
	}

	public static MagicXpHandler getMagicXpHandler() {
		return plugin.magicXpHandler;
	}

	public static StorageHandler getStorageHandler() {
		return plugin.storageHandler;
	}

	public static VariableManager getVariableManager() {
		return plugin.variableManager;
	}

	public static LifeLengthTracker getLifeLengthTracker() {
		return plugin.lifeLengthTracker;
	}

	public static DeprecationHandler getDeprecationManager() {
		return plugin.deprecationHandler;
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

	public static SetMultimap<String, Spell> getSpellsByTag() {
		return plugin.spellsByTag;
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
		plugin.manaHandler.disable();
		plugin.manaHandler = handler;
	}

	/**
	 * Sets the storage handler, which handles data storage.
	 * @param handler the storage handler
	 */
	public static void setStorageHandler(StorageHandler handler) {
		plugin.storageHandler.disable();
		plugin.storageHandler = handler;
	}

	public static void sendMessage(Player recipient, String message) {
		sendMessage(message, recipient, SpellData.NULL);
	}

	/**
	 * Sends a message to a player. This method also does color replacement and has multi-line functionality.
	 *
	 * @param recipient the living entity to send the message to
	 * @param message   the message to send
	 * @param args      spell arguments
	 */
	public static void sendMessage(String message, LivingEntity recipient, String[] args) {
		sendMessage(message, recipient, SpellData.NULL.args(args));
	}

	/**
	 * Sends a message to a player, first making the specified replacements. This method also does color replacement and has multi-line functionality.
	 *
	 * @param recipient    the player to send the message to
	 * @param message      the message to send
	 * @param replacements the replacements to be made, in pairs
	 */
	public static void sendMessageAndFormat(Player recipient, String message, String... replacements) {
		sendMessage(message, recipient, SpellData.NULL, replacements);
	}

	/**
	 * Sends a message to a player, first making the specified replacements.This method also does color replacement and has multi-line functionality.
	 *
	 * @param message      the message to send
	 * @param recipient    the player to send the message to
	 * @param args         the arguments of associated spell cast
	 * @param replacements the replacements to be made, in pairs
	 */
	public static void sendMessageAndFormat(String message, LivingEntity recipient, String[] args, String... replacements) {
		sendMessage(message, recipient, SpellData.NULL.args(args), replacements);
	}

	/**
	 * Sends a message to a player, first by applying replacement using {@link MagicSpells#doReplacements}, then converting
	 * to a component using {@link Util#getMiniMessage}.
	 *
	 * @param message      the message to send
	 * @param recipient    the player to send the message to
	 * @param data         the data of associated spell cast
	 * @param replacements the replacements to be made, in pairs
	 */
	public static void sendMessage(String message, LivingEntity recipient, SpellData data, String... replacements) {
		if (!(recipient instanceof Player) || message == null || message.isEmpty()) return;

		message = doReplacements(message, recipient, data, replacements);

		recipient.sendMessage(Util.getMiniMessage(getTextColor() + message));
	}

	private static final Pattern chatVarMatchPattern = Pattern.compile("%var:(\\w+)(?::(\\d+))?%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	public static String doSubjectVariableReplacements(Player player, String string) {
		if (string == null || string.isEmpty() || plugin.variableManager == null) return string;

		Matcher matcher = chatVarMatchPattern.matcher(string);
		StringBuilder builder = new StringBuilder();

		while (matcher.find()) {
			String varName = matcher.group(1), place = matcher.group(2);

			Variable variable = plugin.variableManager.getVariable(varName);
			if (variable == null) {
				matcher.appendReplacement(builder, "0");
				continue;
			}

			String value;
			if (place != null) {
				if (variable instanceof GlobalStringVariable || variable instanceof PlayerStringVariable) {
					value = TxtUtil.getStringNumber(variable.getStringValue(player), Integer.parseInt(place));
				} else {
					value = TxtUtil.getStringNumber(variable.getValue(player), Integer.parseInt(place));
				}
			} else {
				value = variable.getStringValue(player);
			}

			matcher.appendReplacement(builder, Matcher.quoteReplacement(value));
		}

		return matcher.appendTail(builder).toString();
	}

	private static final Pattern chatPlayerVarMatchPattern = Pattern.compile("%playervar:([^:]+):(\\w+)(?::(\\d+))?%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	public static String doVariableReplacements(Player player, String string) {
		string = doSubjectVariableReplacements(player, string);
		if (string == null || string.isEmpty() || plugin.variableManager == null) return string;

		Matcher matcher = chatPlayerVarMatchPattern.matcher(string);
		StringBuilder builder = new StringBuilder();

		while (matcher.find()) {
			String variableOwnerName = matcher.group(1), varName = matcher.group(2), place = matcher.group(3);

			Variable variable = plugin.variableManager.getVariable(varName);
			if (variable == null) {
				matcher.appendReplacement(builder, "0");
				continue;
			}

			String value;
			if (place != null) {
				if (variable instanceof GlobalStringVariable || variable instanceof PlayerStringVariable) {
					value = TxtUtil.getStringNumber(variable.getStringValue(variableOwnerName), Integer.parseInt(place));
				} else {
					value = TxtUtil.getStringNumber(variable.getValue(variableOwnerName), Integer.parseInt(place));
				}
			} else {
				value = variable.getStringValue(variableOwnerName);
			}

			matcher.appendReplacement(builder, Matcher.quoteReplacement(value));
		}

		return matcher.appendTail(builder).toString();
	}

	private static final Pattern chatTargetedVarMatchPattern = Pattern.compile("%(castervar|targetvar):(\\w+)(?::(\\d+))?%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	public static String doTargetedVariableReplacements(Player caster, Player target, String string) {
		if (string == null || string.isEmpty() || plugin.variableManager == null) return string;

		Matcher matcher = chatTargetedVarMatchPattern.matcher(string);
		StringBuilder builder = new StringBuilder();
		Player varOwner;

		while (matcher.find()) {
			String varName = matcher.group(2);
			Variable variable = plugin.variableManager.getVariable(varName);
			if (variable == null) {
				matcher.appendReplacement(builder, "0");
				continue;
			}

			varOwner = matcher.group(1).equalsIgnoreCase("targetvar") ? target : caster;
			if (varOwner == null) continue;

			String value, place = matcher.group(3);
			if (place != null) {
				if (variable instanceof GlobalStringVariable || variable instanceof PlayerStringVariable) {
					value = TxtUtil.getStringNumber(variable.getStringValue(varOwner), Integer.parseInt(place));
				} else {
					value = TxtUtil.getStringNumber(variable.getValue(varOwner), Integer.parseInt(place));
				}
			} else {
				value = variable.getStringValue(varOwner);
			}

			matcher.appendReplacement(builder, Matcher.quoteReplacement(value));
		}

		return matcher.appendTail(builder).toString();
	}

	public static String doArgumentAndVariableSubstitution(String string, Player player, String[] args) {
		return doVariableReplacements(player, doArgumentSubstitution(string, args));
	}

	public static String doReplacements(String message, SpellData data, String... replacements) {
		return doReplacements(message, data.recipient(), data, replacements);
	}

	/**
	 * Formats the string, with the following replacements in order:
	 *
	 * <ul>
	 *    <li>Argument substitution {@link MagicSpells#doArgumentSubstitution} ()}</li>
	 *    <li>Variable replacements {@link MagicSpells#doVariableReplacements(String, LivingEntity, LivingEntity, LivingEntity)} ()}</li>
	 *    <li>PlaceholderAPI replacements {@link MagicSpells#doPlaceholderReplacements} ()}</li>
	 *    <li>Specified replacements {@link MagicSpells#formatMessage} ()}</li>
	 * <ul/>
	 *
	 * @param message      the message to send
	 * @param recipient    the player to send the message to
	 * @param data         the data of associated spell cast
	 * @param replacements the replacements to be made, in pairs
	 */
	public static String doReplacements(String message, LivingEntity recipient, SpellData data, String... replacements) {
		if (message == null || message.isEmpty()) return message;

		data = data.recipient(recipient);

		message = doArgumentSubstitution(message, data.args());
		message = doVariableReplacements(message, recipient, data.caster(), data.target());
		message = doPlaceholderReplacements(message, recipient, data.caster(), data.target());
		message = formatMessage(message, data, replacements);

		return message;
	}

	private static final Pattern ARGUMENT_PATTERN = Pattern.compile("%arg:(\\d+):([^%]+)%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	public static String doArgumentSubstitution(String string, String[] args) {
		if (string == null || string.isEmpty()) return string;

		Matcher matcher = ARGUMENT_PATTERN.matcher(string);
		StringBuilder builder = new StringBuilder();

		while (matcher.find()) {
			int argIndex = Integer.parseInt(matcher.group(1)) - 1;

			String newValue = matcher.group(2);
			if (args != null && argIndex >= 0 && argIndex < args.length) newValue = args[argIndex];

			matcher.appendReplacement(builder, Matcher.quoteReplacement(newValue));
		}

		return matcher.appendTail(builder).toString();
	}

	private static final Pattern VARIABLE_PATTERN = Pattern.compile("%(var|castervar|targetvar|playervar:([^:]+)):(\\w+)(?::(\\d+))?%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	public static String doVariableReplacements(String message, LivingEntity recipient, LivingEntity caster, LivingEntity target) {
		if (message == null || message.isEmpty()) return message;

		String playerRecipient = recipient instanceof Player player ? player.getName() : null;
		String playerCaster = caster instanceof Player player ? player.getName() : null;
		String playerTarget = target instanceof Player player ? player.getName() : null;

		Matcher matcher = VARIABLE_PATTERN.matcher(message);
		StringBuilder builder = new StringBuilder();

		while (matcher.find()) {
			String placeString = matcher.group(4);

			Variable variable = getVariableManager().getVariable(matcher.group(3));
			if (variable == null) continue;

			int place = -1;
			if (placeString != null) {
				try {
					place = Integer.parseInt(placeString);
				} catch (NumberFormatException ignored) {
					continue;
				}
			}

			String player = switch (matcher.group(1).toLowerCase()) {
				case "var" -> playerRecipient;
				case "castervar" -> playerCaster;
				case "targetvar" -> playerTarget;
				default -> matcher.group(2);
			};

			if (player == null && !(variable instanceof GlobalVariable) && !(variable instanceof GlobalStringVariable))
				continue;

			String value;
			if (place != -1) {
				if (variable instanceof GlobalStringVariable || variable instanceof PlayerStringVariable)
					value = TxtUtil.getStringNumber(variable.getStringValue(player), place);
				else value = TxtUtil.getStringNumber(variable.getValue(player), place);
			} else value = variable.getStringValue(player);

			matcher.appendReplacement(builder, Matcher.quoteReplacement(value));
		}

		return matcher.appendTail(builder).toString();
	}

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(papi|casterpapi|targetpapi|playerpapi:([^:]+)):([^%]+)%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	public static String doPlaceholderReplacements(String message, LivingEntity recipient, LivingEntity caster, LivingEntity target) {
		if (message == null || message.isEmpty() || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
			return message;

		Player playerRecipient = recipient instanceof Player player ? player : null;
		Player playerCaster = caster instanceof Player player ? player : null;
		Player playerTarget = target instanceof Player player ? player : null;

		Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
		StringBuilder builder = new StringBuilder();

		while (matcher.find()) {
			OfflinePlayer owner = switch (matcher.group(1).toLowerCase()) {
				case "papi" -> playerRecipient;
				case "casterpapi" -> playerCaster;
				case "targetpapi" -> playerTarget;
				default -> Bukkit.getOfflinePlayer(matcher.group(2));
			};

			String placeholder = '%' + matcher.group(3) + '%';
			matcher.appendReplacement(builder, Matcher.quoteReplacement(PlaceholderAPI.setPlaceholders(owner, placeholder)));
		}

		return matcher.appendTail(builder).toString();
	}

	/**
	 * Formats a string by performing the specified replacements.
	 * @param message the string to format
	 * @param replacements the replacements to make, in pairs.
	 * @return the formatted string
	 */
	public static String formatMessage(String message, String... replacements) {
		return formatMessage(message, SpellData.NULL, replacements);
	}

	private static final Pattern TARGET_NAME_PATTERN = Pattern.compile("%[art]");

	/**
	 * Formats a string by performing the specified replacements.
	 *
	 * @param message      the string to format
	 * @param data         the data of an associated spell cast
	 * @param replacements the replacements to make, in pairs.
	 * @return the formatted string
	 */
	public static String formatMessage(String message, SpellData data, String... replacements) {
		if (message == null || message.isEmpty()) return message;

		List<String> replacementList = new ArrayList<>();
		if (replacements != null) replacementList.addAll(Arrays.asList(replacements));

		if (data.hasRecipient()) {
			replacementList.add("%r_uuid");
			replacementList.add(data.recipient().getUniqueId().toString());

			replacementList.add("%r");
			replacementList.add(getTargetName(data.recipient()));
		}

		if (data.hasCaster()) {
			replacementList.add("%a_uuid");
			replacementList.add(data.caster().getUniqueId().toString());

			replacementList.add("%a");
			replacementList.add(getTargetName(data.caster()));
		}

		if (data.hasTarget()) {
			replacementList.add("%t_uuid");
			replacementList.add(data.target().getUniqueId().toString());

			replacementList.add("%t");
			replacementList.add(getTargetName(data.target()));
		}

		for (int i = 0; i < replacementList.size() - 1; i += 2) {
			String pattern = replacementList.get(i), replacement = replacementList.get(i + 1);
			if (pattern == null) continue;

			message = message.replace(pattern, replacement == null ? "" : replacement);
		}

		return message;
	}

	public static boolean requireReplacement(String message) {
		Matcher matcher = ARGUMENT_PATTERN.matcher(message);
		if (matcher.find()) return true;

		matcher = VARIABLE_PATTERN.matcher(message);
		if (matcher.find()) return true;

		matcher = PLACEHOLDER_PATTERN.matcher(message);
		if (matcher.find()) return true;

		matcher = TARGET_NAME_PATTERN.matcher(message);
		return matcher.find();
	}

	public static String getTargetName(Entity target) {
		if (target instanceof Player) return target.getName();

		if (target.customName() != null) return Util.getStrictStringFromComponent(target.customName());

		EntityType type = target.getType();
		String name = plugin.entityNames.get(type);
		if (name != null) return name;

		return Util.getStrictStringFromComponent(target.name());
	}

	public static void registerEvents(final Listener listener) {
		registerEvents(listener, EventPriority.NORMAL);
	}

	public static void registerEvents(final Listener listener, EventPriority customPriority) {
		if (customPriority == null) customPriority = EventPriority.NORMAL;
		Set<Method> methods;
		try {
			Class<?> listenerClazz = listener.getClass();
			methods = Sets.union(
				Set.of(listenerClazz.getMethods()),
				Set.of(listenerClazz.getDeclaredMethods())
			);
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
				public void execute(@NotNull Listener listener, @NotNull Event event) {
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
			Bukkit.getPluginManager().registerEvent(eventClass, listener, priority, executor, plugin, eh.ignoreCancelled());
		}
	}

	private static boolean hasAnnotation(Method m, Class<? extends Annotation> clazz) {
		return m.getAnnotation(clazz) != null;
	}

	public static int scheduleDelayedTask(final Runnable task, long delay) {
		return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, !plugin.enableErrorLogging ? task : () -> {
			try {
				task.run();
			} catch (Exception e) {
				handleException(e);
			}
		}, delay);
	}

	public static int scheduleRepeatingTask(final Runnable task, long delay, long interval) {
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

	public static void handleException(@NotNull Exception ex) {
		if (!plugin.enableErrorLogging) {
			ex.printStackTrace();
			return;
		}

		File folder = new File(plugin.getDataFolder(), "errors");
		if (!folder.exists()) folder.mkdir();

		plugin.getLogger().severe("AN EXCEPTION HAS OCCURED:");
		try (PrintWriter writer = new PrintWriter(new File(folder, System.currentTimeMillis() + ".txt"))) {
			Throwable t = ex;
			while (t != null) {
				plugin.getLogger().severe("    " + t.getMessage() + " (" + t.getClass().getName() + ')');
				t.printStackTrace(writer);
				writer.println();
				t = t.getCause();
			}

			plugin.getLogger().severe("This error has been saved in the errors folder.");
			writer.println("Server version: " + Bukkit.getVersion());
			writer.println("MagicSpells version: " + plugin.getPluginMeta().getVersion());
			writer.println("Error log date: " + new Date());
		} catch (Exception e) {
			plugin.getLogger().severe("ERROR WHILE HANDLING EXCEPTION:");
			e.printStackTrace();
			ex.printStackTrace();
		}

		// Delete old errors if the folder exceeds the limit.
		int limit = getErrorLogLimit();
		if (limit > 0) {
			try (Stream<Path> errorPaths = Files.list(folder.toPath())) {
				errorPaths
					.map(Path::toFile)
					.sorted(Comparator.comparing(File::lastModified, Comparator.reverseOrder()))
					.skip(limit)
					.forEach(File::delete);
			} catch (Exception e) {
				plugin.getLogger().severe("Error while cleaning up error folder:");
				e.printStackTrace();
			}
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

		if (spellbook.hasSpell(spell) || !spellbook.canLearn(spell)) return false;

		// Call event
		SpellLearnEvent event = new SpellLearnEvent(spell, player, LearnSource.OTHER, null);
		EventUtil.call(event);
		if (event.isCancelled()) return false;

		spellbook.addSpell(spell);
		spellbook.save();
		return true;
	}

	public void unload() {
		loaded = false;

		// save player data and disable storage
		if (storageHandler != null) {
			for (Spellbook spellBook : spellbooks.values()) {
				storageHandler.save(spellBook);
			}
			storageHandler.disable();
			storageHandler = null;
		}

		// Turn off spells and their spell effects
		for (Spell spell : spells.values()) {
			EffectPosition position;
			List<SpellEffect> spellEffects;
			Iterator<SpellEffect> iterator;
			SpellEffect effect;
			if (spell.getEffects() != null) {
				for (Map.Entry<EffectPosition, List<SpellEffect>> entry : spell.getEffects().entrySet()) {
					if (entry == null) continue;

					position = entry.getKey();
					spellEffects = entry.getValue();
					if (position == null || spellEffects == null) continue;

					iterator = spellEffects.iterator();
					while (iterator.hasNext()) {
						effect = iterator.next();
						effect.turnOff();
						iterator.remove();
					}
				}
			}

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
				Writer writer = new FileWriter(file);
				Map<UUID, Long> cooldowns;
				long cooldown;
				for (Spell spell : spells.values()) {
					cooldowns = spell.getCooldowns();
					for (UUID id : cooldowns.keySet()) {
						cooldown = cooldowns.get(id);
						if (cooldown <= System.currentTimeMillis()) continue;
						writer.append(spell.getInternalName())
								.append(String.valueOf(':'))
								.append(id.toString())
								.append(String.valueOf(':'))
								.append(String.valueOf(cooldown))
								.append(String.valueOf('\n'));
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
		spellsByTag.clear();
		spellsByTag = null;
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
			manaHandler.disable();
			manaHandler = null;
		}

		if (zoneManager != null) {
			zoneManager.disable();
			zoneManager = null;
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
			bossBarManager.disable();
			bossBarManager = null;
		}

		if (volatileCodeHandle != null) {
			volatileCodeHandle = null;
		}

		config = null;
		consoleName = null;
		strCantCast = null;
		strCantBind = null;
		moneyHandler = null;
		expBarManager = null;
		strOnCooldown = null;
		strWrongWorld = null;
		strSpellChange = null;
		strUnknownSpell = null;
		cleanserManager = null;
		strXpAutoLearned = null;
		lifeLengthTracker = null;
		deprecationHandler = null;
		customGoalsManager = null;
		strMissingReagents = null;
		strSpellChangeEmpty = null;
		soundFailOnCooldown = null;
		expressionDictionary = null;
		soundFailMissingReagents = null;

		// Remove star permissions (to allow new spells to be added to them)
		PluginManager pm = Bukkit.getPluginManager();
		pm.removePermission("magicspells.grant.*");
		pm.removePermission("magicspells.cast.*");
		pm.removePermission("magicspells.learn.*");
		pm.removePermission("magicspells.teach.*");

		// Unregister all listeners
		HandlerList.unregisterAll(this);

		// Cancel all tasks
		Bukkit.getScheduler().cancelTasks(this);

		ModifierSet.unload();
		CustomRecipes.clearRecipes();
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
