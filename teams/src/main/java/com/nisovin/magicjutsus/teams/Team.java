package com.nisovin.magicjutsus.teams;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;

public class Team {

	private String name;
	private String permissionNode;
	private boolean friendlyFire;
	
	private List<String> canTargetNames;
	private List<Team> canTargetTeams;
	private List<String> cantTargetNames;
	private List<Team> cantTargetTeams;
	
	public Team(ConfigurationSection config, String name) {
		this.name = name;
		this.permissionNode = config.getString("permission", "magicjutsus.team." + name);
		this.friendlyFire = config.getBoolean("friendly-fire", false);
		this.canTargetNames = config.getStringList("can-target");
		this.cantTargetNames = config.getStringList("cant-target");
	}
	
	public void initialize(MagicJutsusTeams plugin) {
		if (this.canTargetNames != null && !this.canTargetNames.isEmpty()) {
			this.canTargetTeams = new ArrayList<>();
			for (String name : this.canTargetNames) {
				Team team = plugin.getTeamByName(name);
				if (team != null) {
					this.canTargetTeams.add(team);
				} else {
					MagicJutsus.error("Invalid team defined in can-target list");
				}
			}
			if (this.canTargetTeams.isEmpty()) {
				this.canTargetTeams = null;
			}
		}
		this.canTargetNames = null;
		if (this.cantTargetNames != null && !this.cantTargetNames.isEmpty()) {
			this.cantTargetTeams = new ArrayList<>();
			for (String name : this.cantTargetNames) {
				Team team = plugin.getTeamByName(name);
				if (team != null) {
					this.cantTargetTeams.add(team);
				} else {
					MagicJutsus.error("Invalid team defined in cant-target list");
				}
			}
			if (this.cantTargetTeams.isEmpty()) {
				this.cantTargetTeams = null;
			}
		}
		this.cantTargetNames = null;
	}
	
	public boolean inTeam(LivingEntity livingEntity) {
		return livingEntity.hasPermission(this.permissionNode);
	}
	
	public boolean allowFriendlyFire() {
		return this.friendlyFire;
	}
	
	public boolean canTarget(Team team) {
		if (this.canTargetTeams != null && !this.canTargetTeams.contains(team)) return false;
		if (this.cantTargetTeams != null && this.cantTargetTeams.contains(team)) return false;
		return true;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getPermission() {
		return this.permissionNode;
	}
	
	public Set<String> getCanTarget() {
		return new TreeSet<>(this.canTargetNames);
	}
	
	public Set<String> getCantTarget() {
		return new TreeSet<>(this.cantTargetNames);
	}
	
}
