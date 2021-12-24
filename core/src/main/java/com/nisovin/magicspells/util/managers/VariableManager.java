package com.nisovin.magicspells.util.managers;

import java.util.*;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.attribute.Attribute;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.variables.*;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.VariableMod;
import com.nisovin.magicspells.variables.meta.*;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.variables.variabletypes.*;

public class VariableManager {

	private static final Map<String, Class<? extends Variable>> variableTypes = new HashMap<>();
	private static final Map<String, Variable> metaVariables = new HashMap<>();
	private static final Map<String, Variable> variables = new HashMap<>();
	private static final Set<String> dirtyPlayerVars = new HashSet<>();

	private static boolean dirtyGlobalVars = false;
	private static File folder;

	public VariableManager() {
		initialize();
	}

	public void addVariableType(String name, Class<? extends Variable> variable) {
		variableTypes.put(name.toLowerCase(), variable);
	}

	public void addVariableType(Class<? extends Variable> variable, String name) {
		variableTypes.put(name.toLowerCase(), variable);
	}

	public void addMetaVariableType(String name, Variable variable) {
		metaVariables.put("meta_" + name.toLowerCase(), variable);
	}

	public void addMetaVariableType(Variable variable, String name) {
		metaVariables.put("meta_" + name.toLowerCase(), variable);
	}

	public Variable getVariableType(String name) {
		Class<? extends Variable> clazz = variableTypes.get(name.toLowerCase());
		if (clazz == null) return null;

		try {
			return clazz.newInstance();
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return null;
		}
	}

	public Map<String, Class<? extends Variable>> getVariableTypes() {
		return variableTypes;
	}

	public Map<String, Variable> getMetaVariables() {
		return metaVariables;
	}

	public Set<String> getDirtyPlayerVariables() {
		return dirtyPlayerVars;
	}

	private void initialize() {
		// variable types
		addVariableType("player", PlayerVariable.class);
		addVariableType("global", GlobalVariable.class);
		addVariableType("globalstring", GlobalStringVariable.class);
		addVariableType("distancetolocation", DistanceToLocationVariable.class);
		addVariableType("squareddistancetolocation", SquaredDistanceToLocationVariable.class);
		addVariableType("playerstring", PlayerStringVariable.class);

		// meta variable types
		addMetaVariableType("location_x", new CoordXVariable());
		addMetaVariableType("location_y", new CoordYVariable());
		addMetaVariableType("location_z", new CoordZVariable());
		addMetaVariableType("location_pitch", new CoordPitchVariable());
		addMetaVariableType("location_yaw", new CoordYawVariable());
		addMetaVariableType("saturation", new SaturationVariable());
		addMetaVariableType("experience_level", new ExperienceLevelVariable());
		addMetaVariableType("experience_points", new ExperienceVariable());
		addMetaVariableType("remaining_air", new RemainingAirVariable());
		addMetaVariableType("max_air", new MaximumAirVariable());
		addMetaVariableType("fly_speed", new FlySpeedVariable());
		addMetaVariableType("walk_speed", new WalkSpeedVariable());
		addMetaVariableType("food_level", new FoodLevelVariable());
		addMetaVariableType("entity_id", new EntityIDVariable());
		addMetaVariableType("fire_ticks", new FireTicksVariable());
		addMetaVariableType("fall_distance", new FallDistanceVariable());
		addMetaVariableType("players_online", new PlayersOnlineVariable());
		addMetaVariableType("max_health", new MaxHealthVariable());
		addMetaVariableType("current_health", new CurrentHealthVariable());
		addMetaVariableType("health_scale", new HealthScaleVariable());
		addMetaVariableType("compass_target_x", new CompassTargetXVariable());
		addMetaVariableType("compass_target_y", new CompassTargetYVariable());
		addMetaVariableType("compass_target_z", new CompassTargetZVariable());
		addMetaVariableType("velocity_x", new VelocityXVariable());
		addMetaVariableType("velocity_y", new VelocityYVariable());
		addMetaVariableType("velocity_z", new VelocityZVariable());
		addMetaVariableType("no_damage_ticks", new NoDamageTicksVariable());
		addMetaVariableType("max_no_damage_ticks", new MaximumNoDamageTicksVariable());
		addMetaVariableType("last_damage", new LastDamageVariable());
		addMetaVariableType("sleep_ticks", new SleepTicksVariable());
		addMetaVariableType("bed_location_x", new BedCoordXVariable());
		addMetaVariableType("bed_location_y", new BedCoordYVariable());
		addMetaVariableType("bed_location_z", new BedCoordZVariable());
		addMetaVariableType("altitude", new AltitudeVariable());
		addMetaVariableType("absorption", new AbsorptionVariable());
		addMetaVariableType("timestamp_days", new TimestampDaysVariable());
		addMetaVariableType("timestamp_hours", new TimestampHoursVariable());
		addMetaVariableType("timestamp_minutes", new TimestampMinutesVariable());
		addMetaVariableType("timestamp_seconds", new TimestampSecondsVariable());
		addMetaVariableType("timestamp_milliseconds", new TimestampMillisecondsVariable());
		addMetaVariableType("mana", new ManaVariable());
		addMetaVariableType("max_mana", new MaxManaVariable());
		addMetaVariableType("mana_regen", new ManaRegenVariable());

		// meta variable attribute types
		addMetaVariableType("attribute_generic_max_health_base", new AttributeBaseValueVariable("GENERIC_MAX_HEALTH"));
		addMetaVariableType("attribute_generic_follow_range_base", new AttributeBaseValueVariable("GENERIC_FOLLOW_RANGE"));
		addMetaVariableType("attribute_generic_knockback_resistance_base", new AttributeBaseValueVariable("GENERIC_KNOCKBACK_RESISTANCE"));
		addMetaVariableType("attribute_generic_movement_speed_base", new AttributeBaseValueVariable("GENERIC_MOVEMENT_SPEED"));
		addMetaVariableType("attribute_generic_flying_speed_base", new AttributeBaseValueVariable("GENERIC_FLYING_SPEED"));
		addMetaVariableType("attribute_generic_attack_damage_base", new AttributeBaseValueVariable("GENERIC_ATTACK_DAMAGE"));
		addMetaVariableType("attribute_generic_attack_knockback_base", new AttributeBaseValueVariable("GENERIC_ATTACK_KNOCKBACK"));
		addMetaVariableType("attribute_generic_attack_speed_base", new AttributeBaseValueVariable("GENERIC_ATTACK_SPEED"));
		addMetaVariableType("attribute_generic_armor_base", new AttributeBaseValueVariable("GENERIC_ARMOR"));
		addMetaVariableType("attribute_generic_armor_toughness_base", new AttributeBaseValueVariable("GENERIC_ARMOR_TOUGHNESS"));
		addMetaVariableType("attribute_generic_luck_base", new AttributeBaseValueVariable("GENERIC_LUCK"));

		addMetaVariableType("attribute_generic_max_health", new AttributeVariable(Attribute.GENERIC_MAX_HEALTH));
		addMetaVariableType("attribute_generic_follow_range", new AttributeVariable(Attribute.GENERIC_FOLLOW_RANGE));
		addMetaVariableType("attribute_generic_knockback_resistance", new AttributeVariable(Attribute.GENERIC_KNOCKBACK_RESISTANCE));
		addMetaVariableType("attribute_generic_movement_speed", new AttributeVariable(Attribute.GENERIC_MOVEMENT_SPEED));
		addMetaVariableType("attribute_generic_flying_speed", new AttributeVariable(Attribute.GENERIC_FLYING_SPEED));
		addMetaVariableType("attribute_generic_attack_damage", new AttributeVariable(Attribute.GENERIC_ATTACK_DAMAGE));
		addMetaVariableType("attribute_generic_attack_knockback", new AttributeVariable(Attribute.GENERIC_ATTACK_KNOCKBACK));
		addMetaVariableType("attribute_generic_attack_speed", new AttributeVariable(Attribute.GENERIC_ATTACK_SPEED));
		addMetaVariableType("attribute_generic_armor", new AttributeVariable(Attribute.GENERIC_ARMOR));
		addMetaVariableType("attribute_generic_armor_toughness", new AttributeVariable(Attribute.GENERIC_ARMOR_TOUGHNESS));
		addMetaVariableType("attribute_generic_luck", new AttributeVariable(Attribute.GENERIC_LUCK));
	}

	// DEBUG INFO: level 2, loaded variable (name)
	// DEBUG INFO: level 1, # variables loaded
	public void loadVariables(ConfigurationSection section) {
		if (section == null) {
			variables.putAll(getMetaVariables());

			// Load vars
			folder = new File(MagicSpells.getInstance().getDataFolder(), "vars");
			if (!folder.exists()) folder.mkdir();
			loadGlobalVariables();
			for (Player player : Bukkit.getOnlinePlayers()) {
				loadPlayerVariables(player.getName(), Util.getUniqueId(player));
				loadBossBars(player);
				loadExpBar(player);
			}

			// Start save task
			MagicSpells.scheduleRepeatingTask(() -> {
				if (dirtyGlobalVars) saveGlobalVariables();
				if (!dirtyPlayerVars.isEmpty()) saveAllPlayerVariables();
			}, TimeUtil.TICKS_PER_MINUTE, TimeUtil.TICKS_PER_MINUTE);
			return;
		}

		for (String var : section.getKeys(false)) {
			ConfigurationSection varSection = section.getConfigurationSection(var);
			String path = var + ".";
			String type = section.getString(path + "type", "global");
			double def = section.getDouble(path + "default", 0);
			double min = section.getDouble(path + "min", 0);
			double max = section.getDouble(path + "max", Double.MAX_VALUE);
			boolean perm = section.getBoolean(path + "permanent", true);

			Variable variable = getVariableType(type);
			if (variable == null) {
				MagicSpells.error("Variable '" + var + "' has an invalid variable type defined: " + type);
				continue;
			}

			String scoreName = section.getString(path + "scoreboard-title", null);
			String scorePos = section.getString(path + "scoreboard-position", null);
			Objective objective = null;
			if (scoreName != null && scorePos != null) {
				String objName = "MSV_" + var;
				if (objName.length() > 16) objName = objName.substring(0, 16);
				Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
				objective = scoreboard.getObjective(objName);
				if (objective != null) objective.unregister();
				objective = scoreboard.registerNewObjective(objName, "dummy", Util.getMiniMessage(scoreName));
				if (scorePos.equalsIgnoreCase("nameplate")) objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
				else if (scorePos.equalsIgnoreCase("playerlist")) objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
				else objective.setDisplaySlot(DisplaySlot.SIDEBAR);
			}

			boolean expBar = section.getBoolean(path + "exp-bar", false);

			String bossBarTitle = null;
			BarStyle bossBarStyle = null;
			BarColor bossBarColor = null;
			String bossBarNamespaceKey = null;
			// Reserve preceded handling.
			if (section.isString(path + "boss-bar")) bossBarTitle = section.getString(path + "boss-bar");
			else {
				ConfigurationSection bossBar = section.getConfigurationSection(path + "boss-bar");
				if (bossBar != null) {
					bossBarTitle = bossBar.getString("title");
					String style = bossBar.getString("style");
					String color = bossBar.getString("color");
					if (style != null) {
						try {
							bossBarStyle = BarStyle.valueOf(style.toUpperCase());
						} catch (IllegalArgumentException ignored) {
							MagicSpells.error("Variable '" + var + "' has an invalid bossBar style defined: '" + style + "'");
						}
					}
					if (color != null) {
						try {
							bossBarColor = BarColor.valueOf(color.toUpperCase());
						} catch (IllegalArgumentException ignored) {
							MagicSpells.error("Variable '" + var + "' has an invalid bossBar color defined: '" + color + "'");
						}
					}
					bossBarNamespaceKey = bossBar.getString("namespace-key");
					if (!MagicSpells.getBossBarManager().isNamespaceKey(bossBarNamespaceKey)) {
						MagicSpells.error("Variable '" + var + "' has an invalid bossBar namespace-key defined: '" + bossBarNamespaceKey + "'");
					}
				}
			}
			if (bossBarStyle == null) bossBarStyle = BarStyle.SOLID;
			if (bossBarColor == null) bossBarColor = BarColor.PURPLE;
			if (bossBarNamespaceKey == null || bossBarNamespaceKey.isEmpty()) bossBarNamespaceKey = MagicSpells.getBossBarManager().getNamespaceKeyVariable();

			variable.init(def, min, max, perm, objective, expBar, bossBarTitle, bossBarStyle, bossBarColor, bossBarNamespaceKey);
			variable.loadExtraData(varSection);
			variables.put(var, variable);
			MagicSpells.debug(2, "Loaded variable " + var);
		}

		MagicSpells.debug(1, variables.size() + " variables loaded!");

		variables.putAll(getMetaVariables());

		// Load vars
		folder = new File(MagicSpells.getInstance().getDataFolder(), "vars");
		if (!folder.exists()) folder.mkdir();

		loadGlobalVariables();
		for (Player player : Bukkit.getOnlinePlayers()) {
			loadPlayerVariables(player.getName(), Util.getUniqueId(player));
			loadBossBars(player);
			loadExpBar(player);
		}

		// Start save task
		MagicSpells.scheduleRepeatingTask(() -> {
			if (dirtyGlobalVars) saveGlobalVariables();
			if (!dirtyPlayerVars.isEmpty()) saveAllPlayerVariables();
		}, TimeUtil.TICKS_PER_MINUTE, TimeUtil.TICKS_PER_MINUTE);
	}

	/**
	 * Adds a variable with the provided name to the list of variables.
	 * This will replace existing variables if the same name is used.
	 * @param name the name of the variable
	 * @param variable the variable to add
	 * @return Returns true if an existing variable was overwritten
	 */
	public boolean addVariable(String name, Variable variable) {
		return variables.put(name, variable) != null;
	}

	public Map<String, Variable> getVariables() {
		return variables;
	}

	public void set(String variable, Player player, double amount) {
		set(variable, player.getName(), amount);
	}

	public void set(String variable, String player, double amount) {
		Variable var = variables.get(variable);
		set(var, player, amount);
	}

	public void set(Variable variable, String player, double amount) {
		if (variable == null) return;
		variable.set(player, amount);
		updateBossBar(variable, player);
		updateExpBar(variable, player);
		if (!variable.isPermanent()) return;
		if (variable instanceof PlayerVariable) dirtyPlayerVars.add(player);
		else if (variable instanceof GlobalVariable) dirtyGlobalVars = true;
		else if (variable instanceof GlobalStringVariable) dirtyGlobalVars = true;
	}

	public void set(String variable, Player player, String amount) {
		set(variable, player.getName(), amount);
	}

	public void set(String variable, String player, String amount) {
		Variable var = variables.get(variable);
		set(var, player, amount);
	}

	public void set(Variable variable, String player, String amount) {
		if (variable == null) return;
		variable.parseAndSet(player, amount);
		updateBossBar(variable, player);
		updateExpBar(variable, player);
		if (!variable.isPermanent()) return;
		if (variable instanceof PlayerVariable) dirtyPlayerVars.add(player);
		else if (variable instanceof GlobalVariable) dirtyGlobalVars = true;
		else if (variable instanceof GlobalStringVariable) dirtyGlobalVars = true;
	}

	public double getValue(String variable, Player player) {
		Variable var = variables.get(variable);
		if (var != null) return var.getValue(player);
		return 0D;
	}

	public String getStringValue(String variable, Player player) {
		Variable var = variables.get(variable);
		if (var != null) return var.getStringValue(player);
		return 0D + "";
	}

	public double getValue(String variable, String player) {
		Variable var = variables.get(variable);
		if (var != null) return var.getValue(player);
		return 0;
	}

	public String getStringValue(String variable, String player) {
		Variable var = variables.get(variable);
		if (var != null) return var.getStringValue(player);
		return 0D + "";
	}

	public Variable getVariable(String name) {
		return variables.get(name);
	}

	public void reset(String variable, Player player) {
		Variable var = variables.get(variable);
		reset(var, player);
	}

	public void reset(Variable variable, Player player) {
		if (variable == null) return;
		variable.reset(player);
		updateBossBar(variable, player != null ? player.getName() : "");
		updateExpBar(variable, player != null ? player.getName() : "");
		if (!variable.isPermanent()) return;
		if (variable instanceof PlayerVariable) dirtyPlayerVars.add(player != null ? player.getName() : "");
		else if (variable instanceof GlobalVariable) dirtyGlobalVars = true;
		else if (variable instanceof GlobalStringVariable) dirtyGlobalVars = true;
	}

	public void updateBossBar(Variable var, String player) {
		if (var == null) return;
		if (var.getBossBarTitle() == null) return;
		if (player == null || player.isEmpty()) return;
		if (var instanceof GlobalVariable || var instanceof GlobalStringVariable) {
			double pct = var.getValue("") / var.getMaxValue();
			for (Player pl : Bukkit.getOnlinePlayers()) {
				if (pl == null || !pl.isValid()) continue;
				BossBarManager.Bar bar = MagicSpells.getBossBarManager().getBar(pl, var.getBossBarNamespacedKey());
				if (bar == null) continue;
				bar.set(var.getBossBarTitle(), pct, var.getBossBarStyle(), var.getBossBarColor());
			}
			return;
		}
		if (var instanceof PlayerVariable) {
			Player pl = PlayerNameUtils.getPlayerExact(player);
			if (pl == null) return;
			BossBarManager.Bar bar = MagicSpells.getBossBarManager().getBar(pl, var.getBossBarNamespacedKey());
			if (bar == null) return;
			bar.set(var.getBossBarTitle(), var.getValue(pl) / var.getMaxValue(), var.getBossBarStyle(), var.getBossBarColor());
		}
	}

	public void updateExpBar(Variable var, String player) {
		if (var == null) return;
		if (!var.isDisplayedOnExpBar()) return;
		if (player == null || player.isEmpty()) return;
		if (var instanceof GlobalVariable) {
			double pct = var.getValue("") / var.getMaxValue();
			Util.forEachPlayerOnline(p -> p.sendExperienceChange((float) pct, (int) var.getValue("")));
			return;
		}
		if (var instanceof PlayerVariable) {
			Player p = PlayerNameUtils.getPlayerExact(player);
			if (p == null) return;
			p.sendExperienceChange((float) (var.getValue(p) / var.getMaxValue()), (int) var.getValue(p));
		}
	}

	public void loadGlobalVariables() {
		File file = new File(folder, "GLOBAL.txt");
		if (!file.exists()) {
			dirtyGlobalVars = false;
			return;
		}

		try {
			Scanner scanner = new Scanner(file, StandardCharsets.UTF_8);
			while (scanner.hasNext()) {
				String line = scanner.nextLine().trim();
				if (!line.isEmpty()) {
					String[] s = line.split("=", 2);
					Variable variable = variables.get(s[0]);
					if (variable instanceof GlobalVariable && variable.isPermanent()) variable.parseAndSet("", s[1]);
					else if (variable instanceof GlobalStringVariable && variable.isPermanent()) variable.parseAndSet("", s[1]);
				}
			}
			scanner.close();
		} catch (Exception e) {
			MagicSpells.error("ERROR LOADING GLOBAL VARIABLES");
			MagicSpells.handleException(e);
		}

		dirtyGlobalVars = false;
	}

	public void saveGlobalVariables() {
		File file = new File(folder, "GLOBAL.txt");
		if (file.exists()) file.delete();

		List<String> lines = new ArrayList<>();
		for (String variableName : variables.keySet()) {
			Variable variable = variables.get(variableName);
			if ((variable instanceof GlobalVariable || variable instanceof GlobalStringVariable) && variable.isPermanent()) {
				String val = variable.getStringValue("");
				if (!val.equals(variable.getDefaultStringValue())) lines.add(variableName + '=' + Util.flattenLineBreaks(val));
			}
		}

		if (lines.isEmpty()) {
			dirtyGlobalVars = false;
			return;
		}

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, false));
			for (String line : lines) {
				writer.write(line);
				writer.newLine();
			}
			writer.flush();
		} catch (Exception e) {
			MagicSpells.error("ERROR SAVING GLOBAL VARIABLES");
			MagicSpells.handleException(e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e) {
					// No op
				}
			}
		}

		dirtyGlobalVars = false;
	}

	public void loadPlayerVariables(String player, String uniqueId) {
		File file = new File(folder, "PLAYER_" + uniqueId + ".txt");
		if (!file.exists()) {
			File file2 = new File(folder, "PLAYER_" + player + ".txt");
			if (file2.exists()) file2.renameTo(file);
		}
		if (!file.exists()) {
			dirtyPlayerVars.remove(player);
			return;
		}

		try {
			Scanner scanner = new Scanner(file, StandardCharsets.UTF_8);
			while (scanner.hasNext()) {
				String line = scanner.nextLine().trim();
				if (!line.isEmpty()) {
					String[] s = line.split("=", 2);
					Variable variable = variables.get(s[0]);
					if (variable instanceof PlayerVariable && variable.isPermanent()) variable.parseAndSet(player, s[1]);
				}
			}
			scanner.close();
		} catch (Exception e) {
			MagicSpells.error("ERROR LOADING PLAYER VARIABLES FOR " + player);
			MagicSpells.handleException(e);
		}

		dirtyPlayerVars.remove(player);
	}

	public void savePlayerVariables(String player, String uniqueId) {
		File file = new File(folder, "PLAYER_" + player + ".txt");
		if (file.exists()) file.delete();
		file = new File(folder, "PLAYER_" + uniqueId + ".txt");
		if (file.exists()) file.delete();

		List<String> lines = new ArrayList<>();
		for (String variableName : variables.keySet()) {
			Variable variable = variables.get(variableName);
			if (variable instanceof PlayerVariable && variable.isPermanent()) {
				String val = variable.getStringValue(player);
				if (!val.equals(variable.getDefaultStringValue())) lines.add(variableName + '=' + Util.flattenLineBreaks(val));
			}
		}

		if (lines.isEmpty()) {
			dirtyPlayerVars.remove(player);
			return;
		}

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, false));
			for (String line : lines) {
				writer.write(line);
				writer.newLine();
			}
			writer.flush();
		} catch (Exception e) {
			MagicSpells.error("ERROR SAVING PLAYER VARIABLES FOR " + player);
			MagicSpells.handleException(e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e) {
					DebugHandler.debugGeneral(e);
				}
			}
		}

		dirtyPlayerVars.remove(player);
	}

	public void saveAllPlayerVariables() {
		for (String playerName : new HashSet<>(dirtyPlayerVars)) {
			String uid = Util.getUniqueId(playerName);
			if (uid != null) savePlayerVariables(playerName, uid);
		}
	}

	public void loadBossBars(Player player) {
		for (Variable var : variables.values()) {
			if (var.getBossBarTitle() == null) continue;
			MagicSpells.getBossBarManager().getBar(player, var.getBossBarNamespacedKey()).set(var.getBossBarTitle(), var.getValue(player) / var.getMaxValue(), var.getBossBarStyle(), var.getBossBarColor());
		}
	}

	public void loadExpBar(Player player) {
		for (Variable var : variables.values()) {
			if (!var.isDisplayedOnExpBar()) continue;
			player.sendExperienceChange((float) (var.getValue(player) / var.getMaxValue()), (int) var.getValue(player));
			break;
		}
	}

	public void disable() {
		if (dirtyGlobalVars) saveGlobalVariables();
		if (!dirtyPlayerVars.isEmpty()) saveAllPlayerVariables();
		variables.clear();
	}

	public String processVariableMods(String var, VariableMod mod, Player playerToMod, Player caster, Player target, float power, String[] args) {
		if (mod == null) return 0 + "";
		if (playerToMod == null) return 0 + "";

		Variable variable = getVariable(var);
		if (variable == null) return 0 + "";

		VariableMod.Operation op = mod.getOperation();

		if (variable instanceof PlayerStringVariable || variable instanceof GlobalStringVariable) {
			switch (op) {
				case SET -> {
					String value = mod.getStringValue(caster, target, args);

					if (value.equals(variable.getDefaultStringValue())) reset(variable, playerToMod);
					else set(variable, playerToMod.getName(), value);

					return value;
				}
				case ADD -> {
					String value = variable.getStringValue(caster) + mod.getStringValue(caster, target, args);

					if (value.equals(variable.getDefaultStringValue())) reset(variable, playerToMod);
					else set(variable, playerToMod.getName(), value);

					return value;
				}
				case MULTIPLY -> {
					int count = (int) mod.getValue(caster, target, power, args);
					String value = variable.getStringValue(caster).repeat(count);

					if (value.equals(variable.getDefaultStringValue())) reset(variable, playerToMod);
					else set(variable, playerToMod.getName(), value);

					return value;
				}
			}
		}

		double value = op.applyTo(variable.getValue(playerToMod), mod.getValue(caster, target, power, args));

		if (value == variable.getDefaultValue() && !(variable instanceof MetaVariable)) {
			reset(variable, playerToMod);
		} else {
			set(variable, playerToMod.getName(), value);
		}

		return Double.toString(value);
	}

}
