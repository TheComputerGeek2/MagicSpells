package com.nisovin.magicjutsus.teams;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.nisovin.magicjutsus.util.compat.EventUtil;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.events.MagicJutsusLoadedEvent;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;

public class MagicJutsusTeams extends JavaPlugin implements Listener {

	private boolean useCache;
	private boolean clearCacheOnDeath;
	
	private List<Team> teams;
	private Map<String, Team> teamNames;
	private Map<String, Team> playerTeams;
	
	@Override
	public void onEnable() {
		// Setup containers
		this.teams = new ArrayList<>();
		this.teamNames = new HashMap<>();
		this.playerTeams = new HashMap<>();
		
		// Get config
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) saveDefaultConfig();
		reloadConfig();
		Configuration config = getConfig();
		
		// Get config
		this.useCache = config.getBoolean("use-cache", true);
		this.clearCacheOnDeath = config.getBoolean("clear-cache-on-death", false);
		
		// Setup teams
		MagicJutsus.debug(1, "Loading teams...");
		Set<String> teamKeys = config.getConfigurationSection("teams").getKeys(false);
		for (String name : teamKeys) {
			Team team = new Team(config.getConfigurationSection("teams." + name), name);
			this.teams.add(team);
			this.teamNames.put(name, team);
			MagicJutsus.debug(2, "    Team " + name + " loaded");
		}
		for (Team team : this.teams) {
			team.initialize(this);
		}
		getCommand("magicjutsusteams").setExecutor(new MagicJutsusTeamsCommand(this));
		
		// Register events
		EventUtil.register(this, this);
	}
	
	@Override
	public void onDisable() {
		this.teams = null;
		this.teamNames = null;
		this.playerTeams = null;
		getCommand("magicjutsusteams").setExecutor(null);
		HandlerList.unregisterAll((Plugin)this);
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onJutsuTarget(JutsuTargetEvent event) {
		LivingEntity caster = event.getCaster();
		if (caster == null) return;
		if (!(event.getTarget() instanceof Player)) return;
		
		boolean beneficial = event.getJutsu().isBeneficial();
		if (!canTarget(caster, (Player)event.getTarget())) {
			if (!beneficial) event.setCancelled(true);
		} else {
			if (beneficial) event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onMagicJutsusLoad(MagicJutsusLoadedEvent event) {
		onDisable();
		onEnable();
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (this.useCache) {
			this.playerTeams.remove(event.getPlayer().getName());
		}
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (this.useCache && this.clearCacheOnDeath) {
			this.playerTeams.remove(event.getEntity().getName());
		}
	}
	
	public Team getTeam(LivingEntity livingEntity) {
		String playerName = livingEntity.getName();
		if (this.useCache) {
			Team team = this.playerTeams.get(playerName);
			if (team != null) return team;
		}
		for (Team team : this.teams) {
			if (team.inTeam(livingEntity)) {
				if (this.useCache) this.playerTeams.put(playerName, team);
				return team;
			}
		}
		return null;
	}
	
	public boolean canTarget(LivingEntity caster, Player target) {
		Team casterTeam = getTeam(caster);
		Team targetTeam = getTeam(target);
		
		// Allow targeting if one of the players is not in a team
		if (casterTeam == null || targetTeam == null) return true;
		
		// If same team, check friendly fire
		if (casterTeam == targetTeam) return casterTeam.allowFriendlyFire();
		
		// Otherwise check if can target
		return casterTeam.canTarget(targetTeam);
	}
	
	public Team getTeamByName(String name) {
		return this.teamNames.get(name);
	}
	
	public Set<String> getTeamNames() {
		return new TreeSet<>(this.teamNames.keySet());
	}
	
}
