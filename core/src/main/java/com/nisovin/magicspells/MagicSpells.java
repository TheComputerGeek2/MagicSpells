package com.nisovin.magicspells;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.Metrics;
import com.nisovin.magicspells.util.TxtUtil;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.mana.ManaSystem;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.commands.XpCommand;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.commands.ManaCommand;
import com.nisovin.magicspells.commands.CastCommand;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.prompt.PromptType;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.variables.VariableManager;
import com.nisovin.magicspells.materials.ItemNameResolver;
import com.nisovin.magicspells.util.handlers.MoneyHandler;
import com.nisovin.magicspells.util.managers.BossBarManager;
import com.nisovin.magicspells.volatilecode.ManagerVolatile;
import com.nisovin.magicspells.events.MagicSpellsLoadedEvent;
import com.nisovin.magicspells.spells.passive.PassiveManager;
import com.nisovin.magicspells.util.managers.AttributeManager;
import com.nisovin.magicspells.events.MagicSpellsLoadingEvent;
import com.nisovin.magicspells.volatilecode.VolatileCodeHandle;
import com.nisovin.magicspells.materials.MagicItemNameResolver;
import com.nisovin.magicspells.volatilecode.VolatileCodeDisabled;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;
import com.nisovin.magicspells.util.managers.ExperienceBarManager;

import de.slikey.effectlib.EffectManager;

public class MagicSpells extends JavaPlugin {

	public static MagicSpells plugin;

	// Change this when you want to start tweaking the source and fixing bugs
	public static Level DEVELOPER_DEBUG_LEVEL = Level.OFF;

	// Pass this to methods that want spell arguments passed but doesn't have any to be passed
	public static final String[] NULL_ARGS = null;

	private Set<Material> losTransparentBlocks;
	private List<Material> ignoreCastItemDurability; // TODO: fix
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
	private NoMagicZoneManager noMagicZones;
	private ExperienceBarManager expBarManager;

	private MagicConfig config;
	private MagicLogger magicLogger;
	private LifeLengthTracker lifeLengthTracker;
	private ItemNameResolver itemNameResolver;

	boolean debug;
	boolean debugNull;
	boolean debugNumberFormat;

	boolean enableProfiling;
	boolean enableErrorLogging;

	boolean castOnAnimate;
	boolean enableManaBars;
	boolean ignoreCastPerms;
	boolean opsHaveAllSpells;
	boolean ignoreGrantPerms;
	boolean checkWorldPvpFlag;
	boolean allowCastWithFist;
	boolean castWithLeftClick;
	boolean castWithRightClick;
	boolean allowCycleToNoSpell;
	boolean ignoreCastItemNames;
	boolean checkScoreboardTeams;
	boolean enableTempGrantPerms;
	boolean ignoreDefaultBindings;
	boolean ignoreCastItemEnchants;
	boolean useExpBarAsCastTimeBar;
	boolean alwaysShowMessageOnCycle;
	boolean ignoreCastItemNameColors;
	boolean onlyCycleToCastableSpells;
	boolean ignoreGrantPermsFakeValue;
	boolean hidePredefinedItemTooltips;
	boolean cycleSpellsOnOffhandAction;
	boolean separatePlayerSpellsPerWorld;
	boolean showStrCostOnMissingReagents;
	boolean cooldownsPersistThroughReload;
	public boolean opsIgnoreReagents;
	public boolean opsIgnoreCastTimes;
	public boolean opsIgnoreCooldowns;
	public boolean defaultAllPermsFalse;
	public boolean allowAnticheatIntegrations;

	int debugLevel;
	int spellIconSlot;
	int globalCooldown;
	int broadcastRange;

	long lastReloadTime = 0;

	ChatColor textColor;

	// Strings
	String strCantCast;
	String strCantBind;
	String strCastUsage;
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
		if (!new File(getDataFolder(), "config.yml").exists() && !new File(getDataFolder(), "general.yml").exists()) {
			saveResource("general.yml", false);
			if (!new File(getDataFolder(), "mana.yml").exists()) saveResource("mana.yml", false);
			if (!new File(getDataFolder(), "spells-command.yml").exists()) saveResource("spells-command.yml", false);
			if (!new File(getDataFolder(), "spells-regular.yml").exists()) saveResource("spells-regular.yml", false);
			if (!new File(getDataFolder(), "zones.yml").exists()) saveResource("zones.yml", false);
		}
		config = new MagicConfig(this);
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
		ignoreCastItemNameColors = config.getBoolean(path + "ignore-cast-item-name-colors", false);
		checkWorldPvpFlag = config.getBoolean(path + "check-world-pvp-flag", true);
		checkScoreboardTeams = config.getBoolean(path + "check-scoreboard-teams", false);
		showStrCostOnMissingReagents = config.getBoolean(path + "show-str-cost-on-missing-reagents", true);
		losTransparentBlocks = Util.getMaterialList(config.getStringList(path + "los-transparent-blocks", new ArrayList<>()), HashSet::new);
		if (losTransparentBlocks.isEmpty()) {
			losTransparentBlocks.add(Material.AIR);
			losTransparentBlocks.add(Material.VOID_AIR);
			losTransparentBlocks.add(Material.CAVE_AIR);
		}
		ignoreCastItemDurability = Util.getMaterialList(config.getStringList(path + "ignore-cast-item-durability", new ArrayList<>()), ArrayList::new);
		globalCooldown = config.getInt(path + "global-cooldown", 500);
		castOnAnimate = config.getBoolean(path + "cast-on-animate", false);
		useExpBarAsCastTimeBar = config.getBoolean(path + "use-exp-bar-as-cast-time-bar", true);
		cooldownsPersistThroughReload = config.getBoolean(path + "cooldowns-persist-through-reload", true);

		entityNames = new HashMap<>();
		if (config.contains(path + "entity-names")) {
			Set<String> keys = config.getSection(path + "entity-names").getKeys(false);
			for (String key : keys) {
				EntityType entityType = Util.getEntityType(key);
				if (entityType != null) {
					entityNames.put(entityType, config.getString(path + "entity-names." + key, ""));
				}
			}
		}

		soundFailOnCooldown = config.getString(path + "sound-on-cooldown", null);
		soundFailMissingReagents = config.getString(path + "sound-missing-reagents", null);

		strCastUsage = config.getString(path + "str-cast-usage", "Usage: /cast <spell>. Use /cast list to see a list of spells.");
		strUnknownSpell = config.getString(path + "str-unknown-spell", "You do not know a spell with that name.");
		strSpellChange = config.getString(path + "str-spell-change", "You are now using the %s spell.");
		strSpellChangeEmpty = config.getString(path + "str-spell-change-empty", "You are no longer using a spell.");
		strOnCooldown = config.getString(path + "str-on-cooldown", "That spell is on cooldown.");
		strMissingReagents = config.getString(path + "str-missing-reagents", "You do not have the reagents for that spell.");
		strCantCast = config.getString(path + "str-cant-cast", "You can't cast that spell right now.");
		strCantBind = config.getString(path + "str-cant-bind", "You cannot bind that spell to that item.");
		strWrongWorld = config.getString(path + "str-wrong-world", "You cannot cast that spell here.");
		strConsoleName = config.getString(path + "console-name", "Admin");
		strXpAutoLearned = config.getString(path + "str-xp-auto-learned", "You have learned the %s spell!");

		allowAnticheatIntegrations = config.getBoolean(path + "allow-anticheat-integrations", false);

		enableManaBars = config.getBoolean(manaPath + "enable-mana-system", false);

		// Create handling objects
		if (enableManaBars) manaHandler = new ManaSystem(config);
		noMagicZones = new NoMagicZoneManager();
		buffManager = new BuffManager(config.getInt(path + "buff-check-interval", 100));
		expBarManager = new ExperienceBarManager();
		bossBarManager = new BossBarManager();
		attributeManager = new AttributeManager();
		itemNameResolver = new MagicItemNameResolver();
		if (CompatBasics.pluginEnabled("Vault")) moneyHandler = new MoneyHandler();
		lifeLengthTracker = new LifeLengthTracker();

		// Call loading event
		pm.callEvent(new MagicSpellsLoadingEvent(this));

		opsIgnoreReagents = config.getBoolean(path + "ops-ignore-reagents", true);
		opsIgnoreCooldowns = config.getBoolean(path + "ops-ignore-cooldowns", true);
		opsIgnoreCastTimes = config.getBoolean(path + "ops-ignore-cast-times", true);

		// Load predefined items
		log("Loading predefined items...");
		hidePredefinedItemTooltips = config.getBoolean(path + "hide-predefined-items-tooltips", false);
		if (hidePredefinedItemTooltips) {
			log("... hiding tooltips!");
		}
		Util.predefinedItems.clear();
		if (config.contains(path + "predefined-items")) {
			Set<String> predefinedItems = config.getKeys(path + "predefined-items");
			if (predefinedItems != null) {
				for (String key : predefinedItems) {
					if (config.isString(path + "predefined-items." + key)) {
						String s = config.getString(path + "predefined-items." + key, null);
						if (s != null) {
							ItemStack is = Util.getItemStackFromString(s);
							if (is != null) {
								Util.predefinedItems.put(key, is);
							} else {
								MagicSpells.error("Invalid predefined item: " + key + ": " + s);
							}
						}
					} else if (config.isSection(path + "predefined-items." + key)) {
						ConfigurationSection s = config.getSection(path + "predefined-items." + key);
						if (s != null) {
							ItemStack is = Util.getItemStackFromConfig(s);
							if (is != null) {
								Util.predefinedItems.put(key, is);
							} else {
								MagicSpells.error("Invalid predefined item: " + key + ": (section)");
							}
						}
					} else {
						MagicSpells.error("Invalid predefined item: " + key);
					}
				}
			}
		}
		log("..." + Util.predefinedItems.size() + " predefined items loaded");

		// Load variables
		log("Loading variables...");
		ConfigurationSection varSec = null;
		if (config.contains(path + "variables") && config.isSection(path + "variables")) {
			varSec = config.getSection(path + "variables");
		}
		variableManager = new VariableManager(this, varSec);
		log("..." + variableManager.count() + " variables loaded");

		// Load spells
		log("Loading spells...");
		loadSpells(config);
		log("...spells loaded: " + spells.size());
		if (spells.isEmpty()) {
			MagicSpells.error("No spells loaded!");
			return;
		}

		// Load xp system
		if (config.getBoolean(path + "enable-magic-xp", false)) {
			log("Loading xp system...");
			magicXpHandler = new MagicXpHandler(this, config);
			log("...xp system loaded");
		}

		// Load in-game spell names, incantations, and initialize spells
		log("Initializing spells...");
		for (Spell spell : spells.values()) {
			spellNames.put(ChatColor.stripColor(Util.colorize(spell.getName().toLowerCase())), spell);
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

		// Initialize passive manager
		log("Initializing passive manager...");
		PassiveManager passiveManager = PassiveSpell.getManager();
		if (passiveManager != null) passiveManager.initialize();
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
		if (enableManaBars) {
			log("Enabling mana bars...");
			// Init
			manaHandler.initialize();

			// Setup online player mana bars
			Util.forEachPlayerOnline(p -> manaHandler.createManaBar(p));

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

		ModifierSet.initializeModifierListeners();
		log("...done");

		// Initialize logger
		if (config.getBoolean(path + "enable-logging", false)) {
			magicLogger = new MagicLogger(this);
		}

		// Register commands
		CastCommand castCommandExecutor = new CastCommand(this, config.getBoolean(path + "enable-tab-completion", true));
		ManaCommand manaCommandExecutor = new ManaCommand(this, config.getBoolean(path + "enable-tab-completion", true));
		XpCommand xpCommandExecutor = new XpCommand(this, config.getBoolean(path + "enable-tab-completion", true));
		getCommand("magicspellcast").setExecutor(castCommandExecutor);
		getCommand("magicspellmana").setExecutor(manaCommandExecutor);
		getCommand("magicspellxp").setExecutor(xpCommandExecutor);

		// Setup profiling
		if (enableProfiling) {
			profilingTotalTime = new HashMap<>();
			profilingRuns = new HashMap<>();
		}

		CompatBasics.setupExemptionAssistant();

		// Call loaded event
		pm.callEvent(new MagicSpellsLoadedEvent(this));

		log("MagicSpells loading complete!");
	}

	private static final int LONG_LOAD_THRESHOLD = 50;
	// DEBUG INFO: level 2, loaded spell spellname
	private void loadSpells(MagicConfig config) {
		long startTimePre = System.currentTimeMillis();

		// Load spells from plugin folder
		final List<File> jarList = new ArrayList<>();
		for (File file : getDataFolder().listFiles()) {
			if (file.getName().endsWith(".jar")) jarList.add(file);
		}

		// Create class loader
		URL[] urls = new URL[jarList.size() + 1];
		ClassLoader cl = getClassLoader();
		try {
			urls[0] = getDataFolder().toURI().toURL();
			for(int i = 1; i <= jarList.size(); i++) {
				urls[i] = jarList.get(i - 1).toURI().toURL();
			}
			cl = new URLClassLoader(urls, getClassLoader());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

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
			} else if (className.startsWith(".")) {
				className = "com.nisovin.magicspells.spells" + className;
			}

			try {
				// Load spell class
				Constructor<? extends Spell> constructor = constructors.get(className);
				if (constructor == null) {
					Class<? extends Spell> spellClass = cl.loadClass(className).asSubclass(Spell.class);
					constructor = spellClass.getConstructor(MagicConfig.class, String.class);
					constructor.setAccessible(true);
					constructors.put(className, constructor);
				}
				Spell spell = constructor.newInstance(config, spellName);
				spells.put(spellName.toLowerCase(), spell);
				spellsOrdered.add(spell);

				// Done
				debug(2, "Loaded spell: " + spellName);
			} catch (ClassNotFoundException e) {
				error("Unable to load spell " + spellName + " (missing class " + className + ')');
			} catch (NoSuchMethodException e) {
				error("Unable to load spell " + spellName + " (malformed class)");
			} catch (Exception e) {
				error("Unable to load spell " + spellName + " (general error)");
				e.printStackTrace();
			}
			long elapsed = System.currentTimeMillis() - startTime;
			if (elapsed > LONG_LOAD_THRESHOLD) getLogger().warning("LONG SPELL LOAD TIME: " + spellName + ": " + elapsed + "ms");
		}

		long finalElapsed = System.currentTimeMillis() - startTimePre;
		if (lastReloadTime != 0) getLogger().warning("Loaded in " + finalElapsed + "ms (previously " + lastReloadTime + " ms)");
		getLogger().warning("Need help? Check out our discord: discord.gg/6bYqnNy");
		lastReloadTime = finalElapsed;
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

	public static boolean ignoreCastItemNames() {
		return plugin.ignoreCastItemNames;
	}

	public static boolean ignoreCastItemNameColors() {
		return plugin.ignoreCastItemNameColors;
	}

	public static boolean showStrCostOnMissingReagents() {
		return plugin.showStrCostOnMissingReagents;
	}

	public static boolean hidePredefinedItemTooltips() {
		return plugin.hidePredefinedItemTooltips;
	}

	public static boolean enableManaBars() {
		return plugin.enableManaBars;
	}

	public static boolean isDebug() {
		return plugin.debug;
	}

	public static String getStrCastUsage() {
		return plugin.strCastUsage;
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

	public static MagicXpHandler getMagicExpHandler() {
		return plugin.magicXpHandler;
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

	public static ItemNameResolver getItemNameResolver() {
		return plugin.itemNameResolver;
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

	public static void setItemNameResolver(ItemNameResolver resolver) {
		plugin.itemNameResolver = resolver;
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
			String [] msgs = message.replaceAll("&([0-9a-fk-or])", "\u00A7$1").split("\n");
			for (String msg : msgs) {
				if (!msg.isEmpty()) {
					livingEntity.sendMessage(plugin.textColor + msg);
				}
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
		return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, !plugin.enableErrorLogging ? task : (Runnable) () -> {
			try {
				task.run();
			} catch (Exception e) {
				handleException(e);
			}
		}, delay);
	}

	public static int scheduleRepeatingTask(final Runnable task, int delay, int interval) {
		return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, !plugin.enableErrorLogging ? task : (Runnable) () -> {
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
		if (plugin.profilingTotalTime != null && plugin.profilingRuns != null) {
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
		if (plugin.enableProfiling) {
			Long total = plugin.profilingTotalTime.get(key);
			if (total == null) total = (long)0;
			total += time;
			plugin.profilingTotalTime.put(key, total);
			Integer runs = plugin.profilingRuns.get(key);
			if (runs == null) runs = 0;
			runs += 1;
			plugin.profilingRuns.put(key, runs);
		}
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
		PassiveSpell.resetManager();

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
		strCastUsage = null;
		moneyHandler = null;
		expBarManager = null;
		strOnCooldown = null;
		strWrongWorld = null;
		strSpellChange = null;
		strConsoleName = null;
		strUnknownSpell = null;
		strXpAutoLearned = null;
		itemNameResolver = null;
		lifeLengthTracker = null;
		strMissingReagents = null;
		strSpellChangeEmpty = null;
		soundFailOnCooldown = null;
		soundFailMissingReagents = null;

		// Unregister all listeners
		HandlerList.unregisterAll(this);

		// Cancel all tasks
		Bukkit.getScheduler().cancelTasks(this);

		ModifierSet.unload();
		PromptType.unloadDestructPromptData();
		CompatBasics.destructExemptionAssistant();

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
 *
 * - Use MagicPlayer (Caster/PlayerCaster) across the entire plugin
 * - Allow spells to be cast by something other than players, like blocks and other entities
 * - Move NoMagicZoneWorldGuard outside of the core plugin
 *
 */