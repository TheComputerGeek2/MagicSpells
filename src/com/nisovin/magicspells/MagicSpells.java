package com.nisovin.magicspells;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.events.MagicSpellsLoadedEvent;
import com.nisovin.magicspells.events.MagicSpellsLoadingEvent;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.mana.ManaSystem;
import com.nisovin.magicspells.materials.ItemNameResolver;
import com.nisovin.magicspells.materials.MagicItemNameResolver;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.spells.passive.PassiveManager;
import com.nisovin.magicspells.util.BossBarManager;
import com.nisovin.magicspells.util.BossBarManager_V1_8;
import com.nisovin.magicspells.util.BossBarManager_V1_9;
import com.nisovin.magicspells.util.ExperienceBarManager;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MoneyHandler;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.variables.VariableManager;
import com.nisovin.magicspells.volatilecode.VolatileCodeDisabled;
import com.nisovin.magicspells.volatilecode.VolatileCodeEnabled_1_10_R1;
import com.nisovin.magicspells.volatilecode.VolatileCodeEnabled_1_11_R1;
import com.nisovin.magicspells.volatilecode.VolatileCodeEnabled_1_8_R1;
import com.nisovin.magicspells.volatilecode.VolatileCodeEnabled_1_8_R3;
import com.nisovin.magicspells.volatilecode.VolatileCodeEnabled_1_9_R1;
import com.nisovin.magicspells.volatilecode.VolatileCodeEnabled_1_9_R2;
import com.nisovin.magicspells.volatilecode.VolatileCodeHandle;
import com.nisovin.magicspells.volatilecode.VolatileCodeProtocolLib;
import com.nisovin.magicspells.zones.NoMagicZoneManager;

import de.slikey.effectlib.EffectManager;

public class MagicSpells extends JavaPlugin {

	public static MagicSpells plugin;
	
	//change this when you want to start tweaking the source and fixing bugs
	public static Level DEVELOPER_DEBUG_LEVEL = Level.OFF;
	
	//pass this to methods that want spell arguments passed but doesn't have any to be passed
	public static final String[] NULL_ARGS = null;
	
	VolatileCodeHandle volatileCodeHandle;
	
	boolean debug;
	boolean debugNull;
	boolean debugNumberFormat;
	int debugLevel;
	boolean enableErrorLogging;
	boolean enableProfiling;
	ChatColor textColor;
	int broadcastRange;
	
	boolean opsHaveAllSpells;
	boolean defaultAllPermsFalse;
	boolean ignoreGrantPerms;
	boolean ignoreCastPerms;
	
	boolean separatePlayerSpellsPerWorld;
	boolean allowCycleToNoSpell;
	boolean alwaysShowMessageOnCycle;
	boolean onlyCycleToCastableSpells;
	int spellIconSlot;
	boolean allowCastWithFist;
	boolean castWithLeftClick;
	boolean castWithRightClick;
	boolean ignoreDefaultBindings;
	boolean showStrCostOnMissingReagents;
	HashSet<Byte> losTransparentBlocks; // TODO: fix
	List<Integer> ignoreCastItemDurability; // TODO: fix
	HashMap<EntityType, String> entityNames;
	int globalCooldown;
	boolean castOnAnimate;
	boolean useExpBarAsCastTimeBar;
	boolean cooldownsPersistThroughReload;
	boolean ignoreCastItemEnchants;
	boolean ignoreCastItemNames;
	boolean ignoreCastItemNameColors;
	boolean checkWorldPvpFlag;
	boolean checkScoreboardTeams;
	boolean hidePredefinedItemTooltips;
	
	boolean enableManaBars;
	int manaPotionCooldown;
	String strManaPotionOnCooldown;
	HashMap<ItemStack, Integer> manaPotions;
	
	String soundFailOnCooldown;
	String soundFailMissingReagents;
	
	// strings
	String strCastUsage;
	String strUnknownSpell;
	String strSpellChange;
	String strSpellChangeEmpty;
	String strOnCooldown;
	String strMissingReagents;
	String strCantCast;
	String strWrongWorld;
	String strCantBind;
	String strConsoleName;
	String strXpAutoLearned;
	
	// spell containers
	HashMap<String, Spell> spells; // map internal names to spells
	HashMap<String, Spell> spellNames; // map configured names to spells
	ArrayList<Spell> spellsOrdered; // spells in loaded order
	HashMap<String, Spellbook> spellbooks; // player spellbooks
	HashMap<String, Spell> incantations; // map incantation strings to spells
		
	// container vars
	ManaHandler mana;
	HashMap<Player, Long> manaPotionCooldowns;
	NoMagicZoneManager noMagicZones;
	BuffManager buffManager;
	ExperienceBarManager expBarManager;
	BossBarManager bossBarManager;
	ItemNameResolver itemNameResolver;
	MoneyHandler moneyHandler;
	MagicXpHandler magicXpHandler;
	VariableManager variableManager;
	MagicLogger magicLogger;
	LifeLengthTracker lifeLengthTracker;
	
	MagicConfig config;
	
	// profiling
	HashMap<String, Long> profilingTotalTime;
	HashMap<String, Integer> profilingRuns;
	
	public EffectManager effectManager;
	
	
	@Override
	public void onEnable() {
		load();
	}
	
	void load() {
		plugin = this;
		PluginManager pm = plugin.getServer().getPluginManager();
		
		effectManager = new EffectManager(this);
		effectManager.enableDebug(debug);
		
		// create storage stuff
		spells = new HashMap<String,Spell>();
		spellNames = new HashMap<String,Spell>();
		spellsOrdered = new ArrayList<Spell>();
		spellbooks = new HashMap<String,Spellbook>();
		incantations = new HashMap<String,Spell>();
		
		// make sure directories are created
		this.getDataFolder().mkdir();
		new File(this.getDataFolder(), "spellbooks").mkdir();
		
		// load config
		if (!(new File(getDataFolder(), "config.yml")).exists() && !(new File(getDataFolder(), "general.yml")).exists()) {
			saveResource("general.yml", false);
			if (!(new File(getDataFolder(), "mana.yml")).exists()) saveResource("mana.yml", false);
			if (!(new File(getDataFolder(), "spells-command.yml")).exists()) saveResource("spells-command.yml", false);
			if (!(new File(getDataFolder(), "spells-regular.yml")).exists()) saveResource("spells-regular.yml", false);
			if (!(new File(getDataFolder(), "zones.yml")).exists()) saveResource("zones.yml", false);
		}
		config = new MagicConfig(this);
		if (!config.isLoaded()) {
			MagicSpells.log(Level.SEVERE, "Error in config file, stopping config load");
			return;
		}
		
		boolean v1_9 = false;
		if (config.getBoolean("general.enable-volatile-features", true)) {
			try {
				Class.forName("net.minecraft.server.v1_11_R1.MinecraftServer");
				v1_9 = true;
				volatileCodeHandle = new VolatileCodeEnabled_1_11_R1(config);
			} catch (ClassNotFoundException e_1_11_1) {
				try {
					Class.forName("net.minecraft.server.v1_10_R1.MinecraftServer");
					volatileCodeHandle = new VolatileCodeEnabled_1_10_R1(config);
					v1_9 = true;
				} catch (ClassNotFoundException e_1_10_1) {
					try {
						Class.forName("net.minecraft.server.v1_9_R2.MinecraftServer");
						volatileCodeHandle = new VolatileCodeEnabled_1_9_R2();
						v1_9 = true;
					} catch (ClassNotFoundException e_1_9_2) {
						try {
							Class.forName("net.minecraft.server.v1_9_R1.MinecraftServer");
							volatileCodeHandle = new VolatileCodeEnabled_1_9_R1();
							v1_9 = true;
						} catch (ClassNotFoundException e_1_9_r1) {
							try {
								Class.forName("net.minecraft.server.v1_8_R3.MinecraftServer");
								volatileCodeHandle = new VolatileCodeEnabled_1_8_R3();
							} catch (ClassNotFoundException e_1_8_r3) {
								try {
									Class.forName("net.minecraft.server.v1_8_R1.MinecraftServer");
									volatileCodeHandle = new VolatileCodeEnabled_1_8_R1();
								} catch (ClassNotFoundException e_1_8_r1) {
									error("This MagicSpells version is not fully compatible with this server version.");
									error("Some features have been disabled.");
									error("See http://nisovin.com/magicspells/volatilefeatures for more information.");
									if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
										error("ProtocolLib found: some compatibility re-enabled");
										volatileCodeHandle = new VolatileCodeProtocolLib();
									} else {
										volatileCodeHandle = new VolatileCodeDisabled();
									}
								}
							}
						}
					}
				}
			}
		} else {
			volatileCodeHandle = new VolatileCodeDisabled();
		}
		
		debug = config.getBoolean("general.debug", false);
		debugNull = config.getBoolean("general.debug-null", true);
		debugNumberFormat = config.getBoolean("general.debug-number-format", true);
		debugLevel = config.getInt("general.debug-level", 3);
		
		enableErrorLogging = config.getBoolean("general.enable-error-logging", true);
		enableProfiling = config.getBoolean("general.enable-profiling", false);
		textColor = ChatColor.getByChar(config.getString("general.text-color", ChatColor.DARK_AQUA.getChar() + ""));
		broadcastRange = config.getInt("general.broadcast-range", 20);
		
		opsHaveAllSpells = config.getBoolean("general.ops-have-all-spells", true);
		defaultAllPermsFalse = config.getBoolean("general.default-all-perms-false", false);
		ignoreGrantPerms = config.getBoolean("general.ignore-grant-perms", false);
		ignoreCastPerms = config.getBoolean("general.ignore-cast-perms", false);

		separatePlayerSpellsPerWorld = config.getBoolean("general.separate-player-spells-per-world", false);
		allowCycleToNoSpell = config.getBoolean("general.allow-cycle-to-no-spell", false);
		alwaysShowMessageOnCycle = config.getBoolean("general.always-show-message-on-cycle", false);
		onlyCycleToCastableSpells = config.getBoolean("general.only-cycle-to-castable-spells", true);
		spellIconSlot = config.getInt("general.spell-icon-slot", -1);
		allowCastWithFist = config.getBoolean("general.allow-cast-with-fist", false);
		castWithLeftClick = config.getBoolean("general.cast-with-left-click", true);
		castWithRightClick = config.getBoolean("general.cast-with-right-click", false);
		ignoreDefaultBindings = config.getBoolean("general.ignore-default-bindings", false);
		ignoreCastItemEnchants = config.getBoolean("general.ignore-cast-item-enchants", true);
		ignoreCastItemNames = config.getBoolean("general.ignore-cast-item-names", false);
		ignoreCastItemNameColors = config.getBoolean("general.ignore-cast-item-name-colors", false);
		checkWorldPvpFlag = config.getBoolean("general.check-world-pvp-flag", true);
		checkScoreboardTeams = config.getBoolean("general.check-scoreboard-teams", false);
		showStrCostOnMissingReagents = config.getBoolean("general.show-str-cost-on-missing-reagents", true);
		losTransparentBlocks = new HashSet<Byte>(config.getByteList("general.los-transparent-blocks", new ArrayList<Byte>()));
		if (losTransparentBlocks.size() == 0) {
			losTransparentBlocks.add((byte)0);
		}
		ignoreCastItemDurability = config.getIntList("general.ignore-cast-item-durability", new ArrayList<Integer>());
		globalCooldown = config.getInt("general.global-cooldown", 500);
		castOnAnimate = config.getBoolean("general.cast-on-animate", false);
		useExpBarAsCastTimeBar = config.getBoolean("general.use-exp-bar-as-cast-time-bar", true);
		cooldownsPersistThroughReload = config.getBoolean("general.cooldowns-persist-through-reload", true);
		
		entityNames = new HashMap<EntityType, String>();
		if (config.contains("general.entity-names")) {
			Set<String> keys = config.getSection("general.entity-names").getKeys(false);
			for (String key : keys) {
				EntityType entityType = Util.getEntityType(key);
				if (entityType != null) {
					entityNames.put(entityType, config.getString("general.entity-names." + key, ""));
				}
			}
		}

		soundFailOnCooldown = config.getString("general.sound-on-cooldown", null);
		soundFailMissingReagents = config.getString("general.sound-missing-reagents", null);
		
		strCastUsage = config.getString("general.str-cast-usage", "Usage: /cast <spell>. Use /cast list to see a list of spells.");
		strUnknownSpell = config.getString("general.str-unknown-spell", "You do not know a spell with that name.");
		strSpellChange = config.getString("general.str-spell-change", "You are now using the %s spell.");
		strSpellChangeEmpty = config.getString("general.str-spell-change-empty", "You are no longer using a spell.");
		strOnCooldown = config.getString("general.str-on-cooldown", "That spell is on cooldown.");
		strMissingReagents = config.getString("general.str-missing-reagents", "You do not have the reagents for that spell.");
		strCantCast = config.getString("general.str-cant-cast", "You can't cast that spell right now.");
		strCantBind = config.getString("general.str-cant-bind", "You cannot bind that spell to that item.");
		strWrongWorld = config.getString("general.str-wrong-world", "You cannot cast that spell here.");
		strConsoleName = config.getString("general.console-name", "Admin");
		strXpAutoLearned = config.getString("general.str-xp-auto-learned", "You have learned the %s spell!");
		
		enableManaBars = config.getBoolean("mana.enable-mana-system", false);
		manaPotionCooldown = config.getInt("mana.mana-potion-cooldown", 30);
		strManaPotionOnCooldown = config.getString("mana.str-mana-potion-on-cooldown", "You cannot use another mana potion yet.");
		
		// create handling objects
		if (enableManaBars) mana = new ManaSystem(config);
		noMagicZones = new NoMagicZoneManager();
		buffManager = new BuffManager(config.getInt("general.buff-check-interval", 0));
		expBarManager = new ExperienceBarManager();
		if (v1_9) {
			bossBarManager = new BossBarManager_V1_9();
		} else {
			bossBarManager = new BossBarManager_V1_8();
		}
		itemNameResolver = new MagicItemNameResolver();
		if (getServer().getPluginManager().isPluginEnabled("Vault")) {
			moneyHandler = new MoneyHandler();
		}
		lifeLengthTracker = new LifeLengthTracker();
		
		// call loading event
		pm.callEvent(new MagicSpellsLoadingEvent(this));
				
		// init permissions
		log("Initializing permissions");
		boolean opsIgnoreReagents = config.getBoolean("general.ops-ignore-reagents", true);
		boolean opsIgnoreCooldowns = config.getBoolean("general.ops-ignore-cooldowns", true);
		boolean opsIgnoreCastTimes = config.getBoolean("general.ops-ignore-cast-times", true);
		addPermission(pm, "noreagents", opsIgnoreReagents? PermissionDefault.OP : PermissionDefault.FALSE, "Allows casting without needing reagents");
		addPermission(pm, "nocooldown", opsIgnoreCooldowns? PermissionDefault.OP : PermissionDefault.FALSE, "Allows casting without being affected by cooldowns");
		addPermission(pm, "nocasttime", opsIgnoreCastTimes? PermissionDefault.OP : PermissionDefault.FALSE, "Allows casting without being affected by cast times");
		addPermission(pm, "notarget", PermissionDefault.FALSE, "Prevents being targeted by any targeted spells");
		addPermission(pm, "silent", PermissionDefault.FALSE, "Prevents cast messages from being broadcast to players");
		HashMap<String, Boolean> permGrantChildren = new HashMap<String,Boolean>();
		HashMap<String, Boolean> permLearnChildren = new HashMap<String,Boolean>();
		HashMap<String, Boolean> permCastChildren = new HashMap<String,Boolean>();
		HashMap<String, Boolean> permTeachChildren = new HashMap<String,Boolean>();
		
		// load predefined items
		log("Loading predefined items...");
		hidePredefinedItemTooltips = config.getBoolean("general.hide-predefined-items-tooltips", false);
		if (hidePredefinedItemTooltips) {
			log("... hiding tooltips!");
		}
		Util.predefinedItems.clear();
		if (config.contains("general.predefined-items")) {
			Set<String> predefinedItems = config.getKeys("general.predefined-items");
			if (predefinedItems != null) {
				for (String key : predefinedItems) {
					if (config.isString("general.predefined-items." + key)) {
						String s = config.getString("general.predefined-items." + key, null);
						if (s != null) {
							ItemStack is = Util.getItemStackFromString(s);
							if (is != null) {
								Util.predefinedItems.put(key, is);
							} else {
								MagicSpells.error("Invalid predefined item: " + key + ": " + s);
							}
						}
					} else if (config.isSection("general.predefined-items." + key)) {
						ConfigurationSection s = config.getSection("general.predefined-items." + key);
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
		
		// load variables
		log("Loading variables...");
		ConfigurationSection varSec = null;
		if (config.contains("general.variables") && config.isSection("general.variables")) {
			varSec = config.getSection("general.variables");
		}
		variableManager = new VariableManager(this, varSec);
		log("..." + variableManager.count() + " variables loaded");
		
		// load spells
		log("Loading spells...");
		loadSpells(config, pm, permGrantChildren, permLearnChildren, permCastChildren, permTeachChildren);
		log("...spells loaded: " + spells.size());
		if (spells.size() == 0) {
			MagicSpells.error("No spells loaded!");
			return;
		}
		
		log("Finalizing perms...");
		// finalize spell permissions
		addPermission(pm, "grant.*", PermissionDefault.FALSE, permGrantChildren);
		addPermission(pm, "learn.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permLearnChildren);
		addPermission(pm, "cast.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permCastChildren);
		addPermission(pm, "teach.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permTeachChildren);
		
		// advanced perms
		addPermission(pm, "advanced.list", PermissionDefault.FALSE);
		addPermission(pm, "advanced.forget", PermissionDefault.FALSE);
		addPermission(pm, "advanced.scroll", PermissionDefault.FALSE);
		HashMap<String, Boolean> advancedPermChildren = new HashMap<String,Boolean>();
		advancedPermChildren.put("magicspells.advanced.list", true);
		advancedPermChildren.put("magicspells.advanced.forget", true);
		advancedPermChildren.put("magicspells.advanced.scroll", true);
		addPermission(pm, "advanced.*", defaultAllPermsFalse? PermissionDefault.FALSE : PermissionDefault.OP, advancedPermChildren);
		log("...done");
		
		// load xp system
		if (config.getBoolean("general.enable-magic-xp", false)) {
			log("Loading xp system...");
			magicXpHandler = new MagicXpHandler(this, config);
			log("...xp system loaded");
		}
		
		// load in-game spell names, incantations, and initialize spells
		log("Initializing spells...");
		for (Spell spell : spells.values()) {
			spellNames.put(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', spell.getName().toLowerCase())), spell);
			String[] aliases = spell.getAliases();
			if (aliases != null && aliases.length > 0) {
				for (String alias : aliases) {
					if (!spellNames.containsKey(alias.toLowerCase())) {
						spellNames.put(alias.toLowerCase(), spell);
					}
				}
			}
			List<String> incs = spell.getIncantations();
			if (incs != null && incs.size() > 0) {
				for (String s : incs) {
					incantations.put(s.toLowerCase(), spell);
				}
			}
			spell.initialize();
		}
		log("...done");
		
		// load online player spellbooks
		log("Loading online player spellbooks...");
		for (Player p : getServer().getOnlinePlayers()) {
			spellbooks.put(p.getName(), new Spellbook(p, this));
		}
		log("...done");
		
		// initialize passive manager
		log("Initializing passive manager...");
		PassiveManager passiveManager = PassiveSpell.getManager();
		if (passiveManager != null) {
			passiveManager.initialize();
		}
		log("...done");
		
		// load saved cooldowns
		if (cooldownsPersistThroughReload) {
			File file = new File(getDataFolder(), "cooldowns.txt");
			Scanner scanner = null;
			if (file.exists()) {
				try {
					scanner = new Scanner(file);
					while (scanner.hasNext()) {
						String line = scanner.nextLine();
						if (!line.isEmpty()) {
							String[] data = line.split(":");
							long cooldown = Long.parseLong(data[2]);
							if (cooldown > System.currentTimeMillis()) {
								Spell spell = getSpellByInternalName(data[0]);
								if (spell != null) {
									spell.setCooldownManually(data[1], cooldown);
								}
							}
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
		
		// setup mana
		if (enableManaBars) {
			log("Enabling mana bars...");
			// init
			mana.initialize();
			
			// setup online player mana bars
			for (Player p : getServer().getOnlinePlayers()) {
				mana.createManaBar(p);
			}
			
			// load mana potions
			List<String> manaPots = config.getStringList("mana.mana-potions", null);
			if (manaPots != null && manaPots.size() > 0) {
				manaPotions = new LinkedHashMap<ItemStack,Integer>();
				for (int i = 0; i < manaPots.size(); i++) {
					String[] data = manaPots.get(i).split(" ");
					if (data.length == 2 && RegexUtil.matches(RegexUtil.SIMPLE_INT_PATTERN, data[1])) {
						ItemStack item = Util.getItemStackFromString(data[0]);
						if (item != null) {
							manaPotions.put(item, Integer.parseInt(data[1]));
						} else {
							error("Invalid mana potion: " + manaPots.get(i));
						}
					} else {
						error("Invalid mana potion: " + manaPots.get(i));
					}
				}
				manaPotionCooldowns = new HashMap<Player,Long>();
			}
			log("...done");
		}
		
		// load no-magic zones
		noMagicZones.load(config);
		if (noMagicZones.zoneCount() == 0) {
			noMagicZones = null;
		}
		
		// load listeners
		log("Loading cast listeners...");
		registerEvents(new MagicPlayerListener(this));
		registerEvents(new MagicSpellListener(this));
		registerEvents(new CastListener(this));
		if (incantations.size() > 0) {
			registerEvents(new MagicChatListener(this));
		}
		RightClickListener rightClickListener = new RightClickListener(this);
		if (rightClickListener.hasRightClickCastItems()) {
			registerEvents(rightClickListener);
		}
		ConsumeListener consumeListener = new ConsumeListener(this);
		if (consumeListener.hasConsumeCastItems()) {
			registerEvents(consumeListener);
		}
		if (config.getBoolean("general.enable-dance-casting", true)) {
			new DanceCastListener(this, config);
		}
		ModifierSet.initializeModifierListeners();
		log("...done");
		
		// initialize logger
		if (config.getBoolean("general.enable-logging", false)) {
			magicLogger = new MagicLogger(this);
		}
		
		// register commands
		CastCommand exec = new CastCommand(this, config.getBoolean("general.enable-tab-completion", true));
		getCommand("magicspellcast").setExecutor(exec);
		getCommand("magicspellmana").setExecutor(exec);
		getCommand("magicspellxp").setExecutor(exec);
		
		// setup profiling
		if (enableProfiling) {
			profilingTotalTime = new HashMap<String, Long>();
			profilingRuns = new HashMap<String, Integer>();
		}
		
		// call loaded event
		pm.callEvent(new MagicSpellsLoadedEvent(this));
		
		log("MagicSpells loading complete!");
	}
	
	private void loadSpells(MagicConfig config, PluginManager pm, HashMap<String, Boolean> permGrantChildren, HashMap<String, Boolean> permLearnChildren, HashMap<String, Boolean> permCastChildren, HashMap<String, Boolean> permTeachChildren) {
		// load spells from plugin folder
		final List<File> jarList = new ArrayList<File>();
		for (File file : getDataFolder().listFiles()) {
			if (file.getName().endsWith(".jar")) {
				jarList.add(file);
			}
		}

		// create class loader
		URL[] urls = new URL[jarList.size()+1];
		ClassLoader cl = getClassLoader();
		try {		
			urls[0] = getDataFolder().toURI().toURL();
			for(int i = 1; i <= jarList.size(); i++) {
				urls[i] = jarList.get(i-1).toURI().toURL();
			}
			cl = new URLClassLoader(urls, getClassLoader());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		// get spells from config
		Set<String> spellKeys = config.getSpellKeys();
		if (spellKeys == null) return;
		for (String spellName : spellKeys) {
			if (config.getBoolean("spells." + spellName + ".enabled", true)) {
				long starttime = System.currentTimeMillis();
				String className = "";
				if (config.contains("spells." + spellName + ".spell-class")) {
					className = config.getString("spells." + spellName + ".spell-class", "");
				}
				if (className == null || className.isEmpty()) {
					error("Spell '" + spellName + "' does not have a spell-class property");
					continue;
				} else if (className.startsWith(".")) {
					className = "com.nisovin.magicspells.spells" + className;
				}
				try {
					// load spell class
					Class<? extends Spell> spellClass = cl.loadClass(className).asSubclass(Spell.class);
					Constructor<? extends Spell> constructor = spellClass.getConstructor(MagicConfig.class, String.class);
					constructor.setAccessible(true);
					Spell spell = constructor.newInstance(config, spellName);
					spells.put(spellName.toLowerCase(), spell);
					spellsOrdered.add(spell);
					
					// add permissions
					if (!spell.isHelperSpell()) {
						String permName = spell.getPermissionName();
						if (!spell.isAlwaysGranted()) {
							addPermission(pm, "grant." + permName, PermissionDefault.FALSE);
							permGrantChildren.put("magicspells.grant." + permName, true);
						}
						addPermission(pm, "learn." + permName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
						addPermission(pm, "cast." + permName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
						addPermission(pm, "teach." + permName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
						addPermission(pm, "tempgrant." + permName, PermissionDefault.FALSE);
						permLearnChildren.put("magicspells.learn." + permName, true);
						permCastChildren.put("magicspells.cast." + permName, true);
						permTeachChildren.put("magicspells.teach." + permName, true);
					}
					
					// done
					debug(2, "Loaded spell: " + spellName);
					
				} catch (ClassNotFoundException e) {
					error("Unable to load spell " + spellName + " (missing class " + className + ")");
				} catch (NoSuchMethodException e) {
					error("Unable to load spell " + spellName + " (malformed class)");
				} catch (Exception e) {
					error("Unable to load spell " + spellName + " (general error)");
					e.printStackTrace();
				}
				long elapsed = System.currentTimeMillis() - starttime;
				if (elapsed > 50) getLogger().warning("LONG SPELL LOAD TIME: " + spellName + ": " + elapsed + "ms");
			}
		}
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
			if (description == null) {
				pm.addPermission(new Permission("magicspells." + perm, permDefault, children));
			} else {
				pm.addPermission(new Permission("magicspells." + perm, description, permDefault, children));
			}
		}
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
		Spellbook spellbook = plugin.spellbooks.get(player.getName());
		if (spellbook == null) {
			spellbook = new Spellbook(player, plugin);
			plugin.spellbooks.put(player.getName(), spellbook);
		}
		return spellbook;
	}
	
	public static ChatColor getTextColor() {
		return plugin.textColor;
	}
	
	/**
	 * Gets a list of blocks that are considered transparent
	 * @return list of block types
	 */
	public static HashSet<Byte> getTransparentBlocks() {
		return plugin.losTransparentBlocks;
	}
	
	/**
	 * Gets a map of entity types and their configured names, to be used when sending messages to players
	 * @return the map
	 */
	public static HashMap<EntityType, String> getEntityNames() {
		return plugin.entityNames;
	}
	
	/**
	 * Checks whether to ignore the durability on the given type when using it as a cast item.
	 * @param type the type to check
	 * @return whether to ignore durability
	 */
	public static boolean ignoreCastItemDurability(int type) {
		if (plugin.ignoreCastItemDurability != null && plugin.ignoreCastItemDurability.contains(type)) {
			return true;
		} else {
			return false;
		}
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
		return plugin.mana;
	}
	
	/**
	 * Sets the mana handler, which handles all mana transactions.
	 * @param handler the mana handler
	 */
	public static void setManaHandler(ManaHandler handler) {
		plugin.mana.turnOff();
		plugin.mana = handler;
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
	
	public static ItemNameResolver getItemNameResolver() {
		return plugin.itemNameResolver;
	}
	
	public static void setItemNameResolver(ItemNameResolver resolver) {
		plugin.itemNameResolver = resolver;
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
	
	/**
	 * Formats a string by performing the specified replacements.
	 * @param message the string to format
	 * @param replacements the replacements to make, in pairs.
	 * @return the formatted string
	 */
	public static String formatMessage(String message, String... replacements) {
		if (message == null) return null;
		
		String msg = message;
		for (int i = 0; i < replacements.length; i+=2) {
			if (replacements[i] != null) {
				if (replacements[i+1] != null) {
					msg = msg.replace(replacements[i], replacements[i+1]);
				} else {
					msg = msg.replace(replacements[i], "");
				}
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
	 * @param player the player to send the message to
	 * @param message the message to send
	 */
	public static void sendMessage(String message, Player player, String[] args) {
		if (message != null && !message.equals("")) {
			// do var replacements
			message = doArgumentAndVariableSubstitution(message, player, args);
			// send messages
			String [] msgs = message.replaceAll("&([0-9a-fk-or])", "\u00A7$1").split("\n");
			for (String msg : msgs) {
				if (!msg.equals("")) {
					player.sendMessage(plugin.textColor + msg);
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
				double val = plugin.variableManager.getValue(varData[0], player);
				String sval = varData.length == 1 ? Util.getStringNumber(val, -1) : Util.getStringNumber(val, Integer.parseInt(varData[1]));
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
				double val = plugin.variableManager.getValue(varData[1], variableOwnerName);
				String sval = varData.length == 2 ? Util.getStringNumber(val, -1) : Util.getStringNumber(val, Integer.parseInt(varData[2]));
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
				String[] argData = argText.substring(5, argText.length()-1).split(":");
				int argIndex = Integer.parseInt(argData[0]) - 1;
				String newValue;
				if (args != null && args.length < argIndex + 1) {
					newValue = args[argIndex];
				} else {
					newValue = argData[1];
				}
				string = string.replace(argText, newValue);
			}
		}
		return string;
	}
	
	public static String doArgumentAndVariableSubstitution(String string, Player player, String[] args) {
		return doVariableReplacements(player, doArgumentSubstitution(string, args));
	}
	
	public static void registerEvents(final Listener listener) {
		Method[] methods;
        try {
            methods = listener.getClass().getDeclaredMethods();
        } catch (NoClassDefFoundError e) {
        	DebugHandler.debugNoClassDefFoundError(e);
            return;
        }
        for (int i = 0; i < methods.length; i++) {
            final Method method = methods[i];
            final EventHandler eh = method.getAnnotation(EventHandler.class);
            if (eh == null) continue;
            final Class<?> checkClass = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(checkClass) || method.getParameterTypes().length != 1) {
                plugin.getLogger().severe("Wrong method arguments used for event type registered");
                continue;
            }
            final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
            method.setAccessible(true);
            EventExecutor executor = new EventExecutor() {
            	final String eventKey = plugin.enableProfiling ? "Event:" + listener.getClass().getName().replace("com.nisovin.magicspells.","") + "." + method.getName() + "(" + eventClass.getSimpleName() + ")" : null;
                
            	@Override
            	public void execute(Listener listener, Event event) {
                    try {
                        if (!eventClass.isAssignableFrom(event.getClass())) {
                            return;
                        }
                        if (listener == null) {
							return;
						}
                        long start = System.nanoTime();
                        method.invoke(listener, event);
                        if (plugin.enableProfiling) {
                        	Long total = plugin.profilingTotalTime.get(eventKey);
                        	if (total == null) total = (long)0;
                        	total += (System.nanoTime() - start);
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
            plugin.getServer().getPluginManager().registerEvent(eventClass, listener, eh.priority(), executor, plugin, eh.ignoreCancelled());
        }
	}
	
	public static int scheduleDelayedTask(final Runnable task, int delay) {
		return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, !plugin.enableErrorLogging ? task : new Runnable() {
			@Override
			public void run() {
				try {
					task.run();
				} catch (Exception e) {
					handleException(e);
				}
			}
		}, delay);
	}
	
	public static int scheduleRepeatingTask(final Runnable task, int delay, int interval) {
		return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, !plugin.enableErrorLogging ? task : new Runnable() {
			@Override
			public void run() {
				try {
					task.run();
				} catch (Exception e) {
					handleException(e);
				}
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
					plugin.getLogger().severe("    " + t.getMessage() + " (" + t.getClass().getName() + ")");
					t.printStackTrace(writer);
					writer.println();
					t = t.getCause();
				}
				plugin.getLogger().severe("This error has been saved in the errors folder");
				writer.println("Server version: " + Bukkit.getServer().getVersion());
				writer.println("MagicSpells version: " + plugin.getDescription().getVersion());
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
	
	static void profilingReport() {
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
					writer.println(key + "\t" + runs + "\t" + (time/runs/1000000F) + "ms\t" + (time/1000000F) + "ms");
				}
				writer.println();
				writer.println("TOTAL TIME: " + (totalTime/1000000F) + "ms");
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
		if (plugin.debug && level <= plugin.debugLevel) {
			log(Level.INFO, message);
		}
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
			if (spell == null) {
				return false;
			}
		}
		
		Spellbook spellbook = getSpellbook(player);
		
		if (spellbook == null || spellbook.hasSpell(spell) || !spellbook.canLearn(spell)) {
			return false;
		} else {
			// call event
			SpellLearnEvent event = new SpellLearnEvent(spell, player, LearnSource.OTHER, null);
			plugin.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return false;
			} else {
				spellbook.addSpell(spell);
				spellbook.save();
				return true;
			}
		}
	}
	
	void unload() {
		// turn off spells
		for (Spell spell : spells.values()) {
			spell.turnOff();
		}
		PassiveSpell.resetManager();
		
		// save cooldowns
		if (cooldownsPersistThroughReload) {
			File file = new File(getDataFolder(), "cooldowns.txt");
			if (file.exists()) file.delete();
			try {
				FileWriter writer = new FileWriter(file);
				for (Spell spell : spells.values()) {
					Map<String, Long> cooldowns = spell.getCooldowns();
					for (String name : cooldowns.keySet()) {
						long cooldown = cooldowns.get(name);
						if (cooldown > System.currentTimeMillis()) {
							writer.append(spell.getInternalName() + ":" + name + ":" + cooldown + "\n");
						}
					}
				}
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
				file.delete();
			}
		}
		
		// turn off buff manager
		if (buffManager != null) {
			buffManager.turnOff();
			buffManager = null;
		}
		
		// clear memory
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
		if (magicXpHandler != null) {
			magicXpHandler.saveAll();
			magicXpHandler = null;
		}
		if (mana != null) {
			mana.turnOff();
			mana = null;
		}
		if (manaPotionCooldowns != null) {
			manaPotionCooldowns.clear();
			manaPotionCooldowns = null;
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
		expBarManager = null;
		itemNameResolver = null;
		moneyHandler = null;
		losTransparentBlocks = null;
		ignoreCastItemDurability = null;
		entityNames = null;
		profilingTotalTime = null;
		profilingRuns = null;
		strCastUsage = null;
		strUnknownSpell = null;
		strSpellChange = null;
		strSpellChangeEmpty = null;
		strOnCooldown = null;
		strMissingReagents = null;
		strCantCast = null;
		strWrongWorld = null;
		strCantBind = null;
		strConsoleName = null;
		
		config = null;
		
		// remove star permissions (to allow new spells to be added to them)
		getServer().getPluginManager().removePermission("magicspells.grant.*");
		getServer().getPluginManager().removePermission("magicspells.cast.*");
		getServer().getPluginManager().removePermission("magicspells.learn.*");
		getServer().getPluginManager().removePermission("magicspells.teach.*");
		
		// unregister all listeners
		HandlerList.unregisterAll(this);
		
		// cancel all tasks
		Bukkit.getScheduler().cancelTasks(this);
		
		plugin = null;
		effectManager.dispose();
		effectManager = null;
		ModifierSet.unload();
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
 * - Move NoMagicZoneWorldGuard and NoMagicZoneResidence outside of the core plugin
 * 
 */