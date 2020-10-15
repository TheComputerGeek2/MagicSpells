package com.nisovin.magicspells.variables;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.nisovin.magicspells.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.VariableMod;

public class VariableManager implements Listener {
	
	Map<String, Variable> variables = new HashMap<>();
	Set<String> dirtyPlayerVars = new HashSet<>();
	boolean dirtyGlobalVars = false;
	File folder;
	
	// DEBUG INFO: level 2, loaded variable (name)
	// DEBUG INFO: level 1, # variables loaded
	public VariableManager(MagicSpells plugin, ConfigurationSection section) {
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
					objective = Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective(objName, objName);
					objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', scoreName));
					if (scorePos.equalsIgnoreCase("nameplate")) {
						objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
					} else if (scorePos.equalsIgnoreCase("playerlist")) {
						objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
					} else {
						objective.setDisplaySlot(DisplaySlot.SIDEBAR);
					}
				}
				String bossBar = section.getString(var + ".boss-bar", null);
				boolean expBar = section.getBoolean(var + ".exp-bar", false);
				variable.init(def, min, max, perm, objective, bossBar, expBar);
				variable.loadExtraData(varSection);
				this.variables.put(var, variable);
				MagicSpells.debug(2, "Loaded variable " + var);
			}
			MagicSpells.debug(1, this.variables.size() + " variables loaded!");
		}
		this.variables.putAll(SpecialVariables.getSpecialVariables());
		
		if (!this.variables.isEmpty()) MagicSpells.registerEvents(this);
		
		// Load vars
		this.folder = new File(plugin.getDataFolder(), "vars");
		if (!this.folder.exists()) this.folder.mkdir();
		loadGlobalVars();
		for (Player player : Bukkit.getOnlinePlayers()) {
			loadPlayerVars(player.getName(), Util.getUniqueId(player));
			loadBossBar(player);
			loadExpBar(player);
		}
		
		// Start save task
		MagicSpells.scheduleRepeatingTask(new Runnable() {
			
			@Override
			public void run() {
				if (dirtyGlobalVars) saveGlobalVars();
				if (!dirtyPlayerVars.isEmpty()) saveAllPlayerVars();
			}
			
		}, TimeUtil.TICKS_PER_MINUTE, TimeUtil.TICKS_PER_MINUTE);
	}
	
	public int count() {
		return this.variables.size();
	}
	
	public void modify(String variable, Player player, double amount) {
		modify(variable, player.getName(), amount);
	}
	
	public void modify(String variable, String player, double amount) {
		Variable var = this.variables.get(variable);
		if (var != null) {
			boolean changed = var.modify(player, amount);
			if (changed) {
				updateBossBar(var, player);
				updateExpBar(var, player);
				if (var.permanent) {
					if (var instanceof PlayerVariable) {
						this.dirtyPlayerVars.add(player);
					} else if (var instanceof GlobalVariable) {
						this.dirtyGlobalVars = true;
					} else if (var instanceof GlobalStringVariable) {
						this.dirtyGlobalVars = true;
					}
				}
			}
		}
	}
	
	public void multiplyBy(String variable, Player player, double amount) {
		set(variable, player, getValue(variable, player) * amount);
	}
	
	public void divideBy(String variable, Player player, double val) {
		set(variable, player, getValue(variable, player)/val);
	}
	
	public void set(String variable, Player player, double amount) {
		set(variable, player.getName(), amount);
	}
	
	public void set(String variable, String player, double amount) {
		Variable var = this.variables.get(variable);
		if (var != null) {
			var.set(player, amount);
			updateBossBar(var, player);
			updateExpBar(var, player);
			if (var.permanent) {
				if (var instanceof PlayerVariable) {
					this.dirtyPlayerVars.add(player);
				} else if (var instanceof GlobalVariable) {
					this.dirtyGlobalVars = true;
				} else if (var instanceof GlobalStringVariable) {
					this.dirtyGlobalVars = true;
				}
			}
		}
	}
	
	public void set(String variable, Player player, String amount) {
		set(variable, player.getName(), amount);
	}
	
	public void set(String variable, String player, String amount) {
		Variable var = this.variables.get(variable);
		if (var != null) {
			var.parseAndSet(player, amount);
			updateBossBar(var, player);
			updateExpBar(var, player);
			if (var.permanent) {
				if (var instanceof PlayerVariable) {
					this.dirtyPlayerVars.add(player);
				} else if (var instanceof GlobalVariable) {
					this.dirtyGlobalVars = true;
				} else if (var instanceof GlobalStringVariable) {
					this.dirtyGlobalVars = true;
				}
			}
		}
	}
	
	public double getValue(String variable, Player player) {
		Variable var = this.variables.get(variable);
		if (var != null) return var.getValue(player);
		return 0D;
	}
	
	public String getStringValue(String variable, Player player) {
		Variable var = this.variables.get(variable);
		if (var != null) return var.getStringValue(player);
		return 0D + "";
	}
	
	public double getValue(String variable, String player) {
		Variable var = this.variables.get(variable);
		if (var != null) return var.getValue(player);
		return 0;
	}
	
	public String getStringValue(String variable, String player) {
		Variable var = this.variables.get(variable);
		if (var != null) return var.getStringValue(player);
		return 0D + "";
	}
	
	public Variable getVariable(String name) {
		return this.variables.get(name);
	}
	
	public void reset(String variable, Player player) {
		Variable var = this.variables.get(variable);
		if (var != null) {
			var.reset(player);
			updateBossBar(var, player != null ? player.getName() : "");
			updateExpBar(var, player != null ? player.getName() : "");
			if (var.permanent) {
				if (var instanceof PlayerVariable) {
					this.dirtyPlayerVars.add(player != null ? player.getName() : "");
				} else if (var instanceof GlobalVariable) {
					this.dirtyGlobalVars = true;
				} else if (var instanceof GlobalStringVariable) {
					this.dirtyGlobalVars = true;
				}
			}
		}
	}
	
	private void updateBossBar(Variable var, String player) {
		if (var.bossBar != null) {
			if (var instanceof GlobalVariable) {
				double pct = var.getValue("") / var.maxValue;
				Util.forEachPlayerOnline(p -> MagicSpells.getBossBarManager().setPlayerBar(p, var.bossBar, pct));
			} else if (var instanceof PlayerVariable) {
				Player p = PlayerNameUtils.getPlayerExact(player);
				if (p != null) MagicSpells.getBossBarManager().setPlayerBar(p, var.bossBar, var.getValue(p) / var.maxValue);
			}
		}
	}
	
	private void updateExpBar(Variable var, String player) {
		if (var.expBar) {
			if (var instanceof GlobalVariable) {
				double pct = var.getValue("") / var.maxValue;
				Util.forEachPlayerOnline(p -> MagicSpells.getVolatileCodeHandler().setExperienceBar(p, (int)var.getValue(""), (float)pct));
			} else if (var instanceof PlayerVariable) {
				Player p = PlayerNameUtils.getPlayerExact(player);
				if (p != null) MagicSpells.getVolatileCodeHandler().setExperienceBar(p, (int)var.getValue(p), (float)(var.getValue(p) / var.maxValue));
			}
		}
	}
	
	private void loadGlobalVars() {
		File file = new File(this.folder, "GLOBAL.txt");
		if (file.exists()) {
			try {
				Scanner scanner = new Scanner(file);
				while (scanner.hasNext()) {
					String line = scanner.nextLine().trim();
					if (!line.isEmpty()) {
						String[] s = line.split("=", 2);
						Variable variable = this.variables.get(s[0]);
						if (variable instanceof GlobalVariable && variable.permanent) {
							variable.parseAndSet("", s[1]);
						} else if (variable instanceof GlobalStringVariable && variable.permanent) {
							variable.parseAndSet("", s[1]);
						}
					}
				}
				scanner.close();
			} catch (Exception e) {
				MagicSpells.error("ERROR LOADING GLOBAL VARIABLES");
				MagicSpells.handleException(e);
			}
		}
		
		this.dirtyGlobalVars = false;
	}
	
	void saveGlobalVars() {
		File file = new File(folder, "GLOBAL.txt");
		if (file.exists()) file.delete();
		
		List<String> lines = new ArrayList<>();
		for (String variableName : this.variables.keySet()) {
			Variable variable = this.variables.get(variableName);
			if (variable instanceof GlobalVariable && variable.permanent) {
				String val = variable.getStringValue("");
				if (!val.equals(variable.defaultStringValue)) lines.add(variableName + '=' + Util.flattenLineBreaks(val));
			} else if (variable instanceof GlobalStringVariable && variable.permanent) {
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
		}
		this.dirtyGlobalVars = false;
	}
	
	private void loadPlayerVars(String player, String uniqueId) {
		File file = new File(this.folder, "PLAYER_" + uniqueId + ".txt");
		if (!file.exists()) {
			File file2 = new File(this.folder, "PLAYER_" + player + ".txt");
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
				MagicSpells.error("ERROR LOADING PLAYER VARIABLES FOR " + player);
				MagicSpells.handleException(e);
			}
		}
		
		this.dirtyPlayerVars.remove(player);
	}
	
	private void savePlayerVars(String player, String uniqueId) {
		File file = new File(this.folder, "PLAYER_" + player + ".txt");
		if (file.exists()) file.delete();
		file = new File(this.folder, "PLAYER_" + uniqueId + ".txt");
		if (file.exists()) file.delete();
		
		List<String> lines = new ArrayList<>();
		for (String variableName : this.variables.keySet()) {
			Variable variable = this.variables.get(variableName);
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
		}
		
		this.dirtyPlayerVars.remove(player);
	}
	
	void saveAllPlayerVars() {
		for (String playerName : new HashSet<>(this.dirtyPlayerVars)) {
			String uid = Util.getUniqueId(playerName);
			if (uid != null) savePlayerVars(playerName, uid);
		}
	}
	
	private void loadBossBar(Player player) {
		for (Variable var : this.variables.values()) {
			if (var.bossBar != null) {
				MagicSpells.getBossBarManager().setPlayerBar(player, var.bossBar, var.getValue(player) / var.maxValue);
				break;
			}
		}
	}
	
	void loadExpBar(Player player) {
		for (Variable var : this.variables.values()) {
			if (var.expBar) {
				MagicSpells.getVolatileCodeHandler().setExperienceBar(player, (int)var.getValue(player), (float)(var.getValue(player) / var.maxValue));
				break;
			}
		}
	}
	
	public void disable() {
		if (this.dirtyGlobalVars) saveGlobalVars();
		if (!this.dirtyPlayerVars.isEmpty()) saveAllPlayerVars();
		this.variables.clear();
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		loadPlayerVars(player.getName(), Util.getUniqueId(player));
		loadBossBar(player);
		MagicSpells.scheduleDelayedTask(() -> loadExpBar(player), 10);
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		if (this.dirtyPlayerVars.contains(event.getPlayer().getName())) savePlayerVars(event.getPlayer().getName(), Util.getUniqueId(event.getPlayer()));
	}
	
	// DEBUG INFO: Debug log level 3, variable was modified for player by amount because of spell cast
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void variableModsCast(SpellCastEvent event) {
		if (event.getSpellCastState() == SpellCastState.NORMAL) {
			Map<String, VariableMod> varMods = event.getSpell().getVariableModsCast();
			if (varMods != null && !varMods.isEmpty()) {
				Player player = event.getCaster();
				for (String var : varMods.keySet()) {
					VariableMod mod = varMods.get(var);
					double amount = mod.getValue(player, null);
					if (amount == 0 && mod.isConstantValue()) {
						reset(var, player);
					} else {
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
							set(var, player, amount);
							break;
						}
					}
					MagicSpells.debug(3, "Variable '" + var + "' for player '" + player.getName() + "' modified by " + amount + " as a result of spell cast '" + event.getSpell().getName() + '\'');
				}
			}
		}
	}
	
	// DEBUG INFO: Debug log level 3, variable was modified for player by amount because of spell casted
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void variableModsCasted(SpellCastedEvent event) {
		if (event.getSpellCastState() == SpellCastState.NORMAL && event.getPostCastAction() != PostCastAction.ALREADY_HANDLED) {
			Map<String, VariableMod> varMods = event.getSpell().getVariableModsCasted();
			if (varMods != null && !varMods.isEmpty()) {
				Player player = event.getCaster();
				for (String var : varMods.keySet()) {
					VariableMod mod = varMods.get(var);
					double amount = mod.getValue(player, null);
					if (amount == 0 && mod.isConstantValue()) {
						reset(var, player);
					} else {
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
							set(var, player, amount);
							break;
						}
					}
					MagicSpells.debug(3, "Variable '" + var + "' for player '" + player.getName() + "' modified by " + amount + " as a result of spell casted '" + event.getSpell().getName() + '\'');
				}
			}
		}
	}
	
	// DEBUG INFO: Debug log level 3, variable was modified for player by amount because of spell target
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void variableModsTarget(SpellTargetEvent event) {
		Map<String, VariableMod> varMods = event.getSpell().getVariableModsTarget();
		if (varMods != null && !varMods.isEmpty()) {
			Player player = event.getCaster();
			Player target = event.getTarget() instanceof Player ? (Player)event.getTarget() : null;
			if (player != null) {
				for (String var : varMods.keySet()) {
					VariableMod mod = varMods.get(var);
					double amount = mod.getValue(player, target);
					if (amount == 0 && mod.isConstantValue()) {
						reset(var, player);
					} else {
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
							set(var, player, amount);
							break;
						}
					}
					MagicSpells.debug(3, "Variable '" + var + "' for player '" + player.getName() + "' modified by " + amount + " as a result of spell target from '" + event.getSpell().getName() + '\'');
				}
			}
		}
	}
	
}
