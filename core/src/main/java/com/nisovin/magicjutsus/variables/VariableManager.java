package com.nisovin.magicjutsus.variables;

import java.util.*;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;

import com.google.common.collect.Multimap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.VariableMod;
import com.nisovin.magicjutsus.Jutsu.PostCastAction;
import com.nisovin.magicjutsus.Jutsu.JutsuCastState;
import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.events.JutsuCastedEvent;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;

public class VariableManager implements Listener {
	
	private Map<String, Variable> variables = new HashMap<>();
	private Set<String> dirtyPlayerVars = new HashSet<>();
	private boolean dirtyGlobalVars = false;
	private File folder;
	
	// DEBUG INFO: level 2, loaded variable (name)
	// DEBUG INFO: level 1, # variables loaded
	public VariableManager(MagicJutsus plugin, ConfigurationSection section) {
		if (section != null) {
			for (String var : section.getKeys(false)) {
				ConfigurationSection varSection = section.getConfigurationSection(var);
				String type = section.getString(var + ".type", "global");
				double def = section.getDouble(var + ".default", 0);
				double min = section.getDouble(var + ".min", 0);
				double max = section.getDouble(var + ".max", Double.MAX_VALUE);
				boolean perm = section.getBoolean(var + ".permanent", true);
				
				Variable variable = VariableType.getType(type).newInstance();
				
				String scoreName = section.getString(var + ".scoreboard-title", null);
				String scorePos = section.getString(var + ".scoreboard-position", null);
				Objective objective = null;
				if (scoreName != null && scorePos != null) {
					String objName = "MSV_" + var;
					if (objName.length() > 16) objName = objName.substring(0, 16);
					objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objName);
					if (objective != null) {
						objective.unregister();
						objective = null;
					}
					objective = Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective(objName, objName, objName);
					objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', scoreName));
					if (scorePos.equalsIgnoreCase("nameplate")) objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
					else if (scorePos.equalsIgnoreCase("playerlist")) objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
					else objective.setDisplaySlot(DisplaySlot.SIDEBAR);
				}
				String bossBar = section.getString(var + ".boss-bar", null);
				boolean expBar = section.getBoolean(var + ".exp-bar", false);
				variable.init(def, min, max, perm, objective, bossBar, expBar);
				variable.loadExtraData(varSection);
				variables.put(var, variable);
				MagicJutsus.debug(2, "Loaded variable " + var);
			}
			MagicJutsus.debug(1, variables.size() + " variables loaded!");
		}
		variables.putAll(SpecialVariables.getSpecialVariables());
		
		if (!variables.isEmpty()) MagicJutsus.registerEvents(this);
		
		// Load vars
		folder = new File(plugin.getDataFolder(), "vars");
		if (!folder.exists()) folder.mkdir();
		loadGlobalVars();
		for (Player player : Bukkit.getOnlinePlayers()) {
			loadPlayerVars(player.getName(), Util.getUniqueId(player));
			loadBossBar(player);
			loadExpBar(player);
		}
		
		// Start save task
		MagicJutsus.scheduleRepeatingTask(() -> {
			if (dirtyGlobalVars) saveGlobalVars();
			if (!dirtyPlayerVars.isEmpty()) saveAllPlayerVars();
		}, TimeUtil.TICKS_PER_MINUTE, TimeUtil.TICKS_PER_MINUTE);
	}
	
	public int count() {
		return variables.size();
	}
	
	public void modify(String variable, Player player, double amount) {
		modify(variable, player.getName(), amount);
	}
	
	public void modify(String variable, String player, double amount) {
		Variable var = variables.get(variable);
		if (var == null) return;
		boolean changed = var.modify(player, amount);
		if (!changed) return;
		updateBossBar(var, player);
		updateExpBar(var, player);
		if (!var.permanent) return;
		if (var instanceof PlayerVariable) dirtyPlayerVars.add(player);
		else if (var instanceof GlobalVariable) dirtyGlobalVars = true;
	}
	
	public void multiplyBy(String variable, Player player, double amount) {
		set(variable, player, getValue(variable, player) * amount);
	}
	
	public void divideBy(String variable, Player player, double val) {
		set(variable, player, getValue(variable, player) / val);
	}
	
	public void set(String variable, Player player, double amount) {
		set(variable, player.getName(), amount);
	}
	
	public void set(String variable, String player, double amount) {
		Variable var = variables.get(variable);
		if (var == null) return;
		var.set(player, amount);
		updateBossBar(var, player);
		updateExpBar(var, player);
		if (!var.permanent) return;
		if (var instanceof PlayerVariable) dirtyPlayerVars.add(player);
		else if (var instanceof GlobalVariable) dirtyGlobalVars = true;
	}
	
	public void set(String variable, Player player, String amount) {
		set(variable, player.getName(), amount);
	}
	
	public void set(String variable, String player, String amount) {
		Variable var = variables.get(variable);
		if (var == null) return;
		var.parseAndSet(player, amount);
		updateBossBar(var, player);
		updateExpBar(var, player);
		if (!var.permanent) return;
		if (var instanceof PlayerVariable) dirtyPlayerVars.add(player);
		else if (var instanceof GlobalVariable) dirtyGlobalVars = true;
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
		if (var == null) return;
		var.reset(player);
		updateBossBar(var, player != null ? player.getName() : "");
		updateExpBar(var, player != null ? player.getName() : "");
		if (!var.permanent) return;
		if (var instanceof PlayerVariable) dirtyPlayerVars.add(player != null ? player.getName() : "");
		else if (var instanceof GlobalVariable) dirtyGlobalVars = true;
	}
	
	private void updateBossBar(Variable var, String player) {
		if (var.bossBar == null) return;
		if (var instanceof GlobalVariable) {
			double pct = var.getValue("") / var.maxValue;
			Util.forEachPlayerOnline(p -> MagicJutsus.getBossBarManager().setPlayerBar(p, var.bossBar, pct));
		} else if (var instanceof PlayerVariable) {
			Player p = PlayerNameUtils.getPlayerExact(player);
			if (p != null) MagicJutsus.getBossBarManager().setPlayerBar(p, var.bossBar, var.getValue(p) / var.maxValue);
		}
	}
	
	private void updateExpBar(Variable var, String player) {
		if (!var.expBar) return;
		if (var instanceof GlobalVariable) {
			double pct = var.getValue("") / var.maxValue;
			Util.forEachPlayerOnline(p -> MagicJutsus.getVolatileCodeHandler().setExperienceBar(p, (int) var.getValue(""), (float) pct));
		} else if (var instanceof PlayerVariable) {
			Player p = PlayerNameUtils.getPlayerExact(player);
			if (p != null) MagicJutsus.getVolatileCodeHandler().setExperienceBar(p, (int) var.getValue(p), (float) (var.getValue(p) / var.maxValue));
		}
	}
	
	private void loadGlobalVars() {
		File file = new File(folder, "GLOBAL.txt");
		if (file.exists()) {
			try {
				Scanner scanner = new Scanner(file);
				while (scanner.hasNext()) {
					String line = scanner.nextLine().trim();
					if (!line.isEmpty()) {
						String[] s = line.split("=", 2);
						Variable variable = variables.get(s[0]);
						if (variable instanceof GlobalVariable && variable.permanent) variable.parseAndSet("", s[1]);
					}
				}
				scanner.close();
			} catch (Exception e) {
				MagicJutsus.error("ERROR LOADING GLOBAL VARIABLES");
				MagicJutsus.handleException(e);
			}
		}
		
		dirtyGlobalVars = false;
	}
	
	private void saveGlobalVars() {
		File file = new File(folder, "GLOBAL.txt");
		if (file.exists()) file.delete();
		
		List<String> lines = new ArrayList<>();
		for (String variableName : variables.keySet()) {
			Variable variable = variables.get(variableName);
			if (variable instanceof GlobalVariable && variable.permanent) {
				String val = variable.getStringValue("");
				if (!val.equals(variable.defaultStringValue)) lines.add(variableName + '=' + Util.flattenLineBreaks(val));
			}
		}
		
		if (!lines.isEmpty()) {
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(file, false));
				for (String line : lines) {
					writer.write(line);
					writer.newLine();
				}
				writer.flush();
			} catch (Exception e) {
				MagicJutsus.error("ERROR SAVING GLOBAL VARIABLES");
				MagicJutsus.handleException(e);
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (Exception e) {
						// No op
					}
				}
			}
		}
		dirtyGlobalVars = false;
	}
	
	private void loadPlayerVars(String player, String uniqueId) {
		File file = new File(folder, "PLAYER_" + uniqueId + ".txt");
		if (!file.exists()) {
			File file2 = new File(folder, "PLAYER_" + player + ".txt");
			if (file2.exists()) file2.renameTo(file);
		}
		if (file.exists()) {
			try {
				Scanner scanner = new Scanner(file);
				while (scanner.hasNext()) {
					String line = scanner.nextLine().trim();
					if (!line.isEmpty()) {
						String[] s = line.split("=", 2);
						Variable variable = variables.get(s[0]);
						if (variable instanceof PlayerVariable && variable.permanent) variable.parseAndSet(player, s[1]);
					}
				}
				scanner.close();
			} catch (Exception e) {
				MagicJutsus.error("ERROR LOADING PLAYER VARIABLES FOR " + player);
				MagicJutsus.handleException(e);
			}
		}
		
		dirtyPlayerVars.remove(player);
	}
	
	private void savePlayerVars(String player, String uniqueId) {
		File file = new File(folder, "PLAYER_" + player + ".txt");
		if (file.exists()) file.delete();
		file = new File(folder, "PLAYER_" + uniqueId + ".txt");
		if (file.exists()) file.delete();
		
		List<String> lines = new ArrayList<>();
		for (String variableName : variables.keySet()) {
			Variable variable = variables.get(variableName);
			if (variable instanceof PlayerVariable && variable.permanent) {
				String val = variable.getStringValue(player);
				if (!val.equals(variable.defaultStringValue)) lines.add(variableName + '=' + Util.flattenLineBreaks(val));
			}
		}
		
		if (!lines.isEmpty()) {
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(file, false));
				for (String line : lines) {
					writer.write(line);
					writer.newLine();
				}
				writer.flush();				
			} catch (Exception e) {
				MagicJutsus.error("ERROR SAVING PLAYER VARIABLES FOR " + player);
				MagicJutsus.handleException(e);
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (Exception e) {
						DebugHandler.debugGeneral(e);
					}
				}
			}
		}
		
		dirtyPlayerVars.remove(player);
	}
	
	private void saveAllPlayerVars() {
		for (String playerName : new HashSet<>(dirtyPlayerVars)) {
			String uid = Util.getUniqueId(playerName);
			if (uid != null) savePlayerVars(playerName, uid);
		}
	}
	
	private void loadBossBar(Player player) {
		for (Variable var : variables.values()) {
			if (var.bossBar == null) continue;
			MagicJutsus.getBossBarManager().setPlayerBar(player, var.bossBar, var.getValue(player) / var.maxValue);
			break;
		}
	}
	
	private void loadExpBar(Player player) {
		for (Variable var : variables.values()) {
			if (!var.expBar) continue;
			MagicJutsus.getVolatileCodeHandler().setExperienceBar(player, (int) var.getValue(player), (float) (var.getValue(player) / var.maxValue));
			break;
		}
	}
	
	public void disable() {
		if (dirtyGlobalVars) saveGlobalVars();
		if (!dirtyPlayerVars.isEmpty()) saveAllPlayerVars();
		variables.clear();
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		loadPlayerVars(player.getName(), Util.getUniqueId(player));
		loadBossBar(player);
		MagicJutsus.scheduleDelayedTask(() -> loadExpBar(player), 10);
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		if (dirtyPlayerVars.contains(event.getPlayer().getName())) savePlayerVars(event.getPlayer().getName(), Util.getUniqueId(event.getPlayer()));
	}
	
	// DEBUG INFO: Debug log level 3, variable was modified for player by amount because of jutsu cast
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void variableModsCast(JutsuCastEvent event) {
		if (event.getJutsuCastState() != JutsuCastState.NORMAL) return;
		Multimap<String, VariableMod> varMods = event.getJutsu().getVariableModsCast();
		if (varMods == null || varMods.isEmpty()) return;
		LivingEntity caster = event.getCaster();
		if (!(caster instanceof Player)) return;
		Player player = (Player) caster;
		for (String var : varMods.keySet()) {
			Collection<VariableMod> mods = varMods.get(var);
			if (mods == null) continue;
			for (VariableMod mod : mods) {
				Variable variable = MagicJutsus.getVariableManager().getVariable(var);
				String str = null;
				if (variable instanceof PlayerStringVariable) str = mod.getValue();
				double amount = mod.getValue(player, null);

				if (amount == 0 && mod.isConstantValue()) {
					reset(var, player);
					continue;
				}
				VariableMod.Operation op = mod.getOperation();
				switch (op) {
					case ADD:
						modify(var, player, amount);
						break;
					case DIVIDE:
						divideBy(var, player, amount);
						break;
					case MULTIPLY:
						multiplyBy(var, player, amount);
						break;
					case SET:
						if (variable instanceof PlayerStringVariable) set(var, player, str);
						else set(var, player, amount);
						break;
				}
				MagicJutsus.debug(3, "Variable '" + var + "' for player '" + player.getName() + "' modified by " + amount + " as a result of jutsu cast '" + event.getJutsu().getName() + '\'');
			}
		}
	}
	
	// DEBUG INFO: Debug log level 3, variable was modified for player by amount because of jutsu casted
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void variableModsCasted(JutsuCastedEvent event) {
		if (event.getJutsuCastState() != JutsuCastState.NORMAL || event.getPostCastAction() == PostCastAction.ALREADY_HANDLED) return;
		Multimap<String, VariableMod> varMods = event.getJutsu().getVariableModsCasted();
		if (varMods == null || varMods.isEmpty()) return;
		LivingEntity caster = event.getCaster();
		if (!(caster instanceof Player)) return;
		Player player = (Player) caster;
		for (String var : varMods.keySet()) {
			Collection<VariableMod> mods = varMods.get(var);
			if (mods == null) continue;
			for (VariableMod mod : mods) {
				Variable variable = MagicJutsus.getVariableManager().getVariable(var);
				String str = null;
				if (variable instanceof PlayerStringVariable) str = mod.getValue();
				double amount = mod.getValue(player, null);

				if (amount == 0 && mod.isConstantValue()) {
					reset(var, player);
					continue;
				}
				VariableMod.Operation op = mod.getOperation();
				switch (op) {
					case ADD:
						modify(var, player, amount);
						break;
					case DIVIDE:
						divideBy(var, player, amount);
						break;
					case MULTIPLY:
						multiplyBy(var, player, amount);
						break;
					case SET:
						if (variable instanceof PlayerStringVariable) set(var, player, str);
						else set(var, player, amount);
						break;
				}
				MagicJutsus.debug(3, "Variable '" + var + "' for player '" + player.getName() + "' modified by " + amount + " as a result of jutsu casted '" + event.getJutsu().getName() + '\'');
			}
		}
	}
	
	// DEBUG INFO: Debug log level 3, variable was modified for player by amount because of jutsu target
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void variableModsTarget(JutsuTargetEvent event) {
		Multimap<String, VariableMod> varMods = event.getJutsu().getVariableModsTarget();
		if (varMods == null || varMods.isEmpty()) return;
		LivingEntity caster = event.getCaster();
		if (!(caster instanceof Player)) return;
		Player player = (Player) caster;
		Player target = event.getTarget() instanceof Player ? (Player) event.getTarget() : null;
		if (target == null) return;
		for (String var : varMods.keySet()) {
			Collection<VariableMod> mods = varMods.get(var);
			if (mods == null) continue;
			for (VariableMod mod : mods) {
				Variable variable = MagicJutsus.getVariableManager().getVariable(var);
				String str = null;
				if (variable instanceof PlayerStringVariable) str = mod.getValue();
				double amount = mod.getValue(player, target);

				if (amount == 0 && mod.isConstantValue()) {
					reset(var, target);
					continue;
				}
				VariableMod.Operation op = mod.getOperation();
				switch (op) {
					case ADD:
						modify(var, target, amount);
						break;
					case DIVIDE:
						divideBy(var, target, amount);
						break;
					case MULTIPLY:
						multiplyBy(var, target, amount);
						break;
					case SET:
						if (variable instanceof PlayerStringVariable) set(var, target, str);
						else set(var, target, amount);
						break;
				}
				MagicJutsus.debug(3, "Variable '" + var + "' for player '" + target.getName() + "' modified by " + amount + " as a result of jutsu target from '" + event.getJutsu().getName() + '\'');
			}
		}
	}

}
