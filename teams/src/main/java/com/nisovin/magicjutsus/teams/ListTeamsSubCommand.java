package com.nisovin.magicjutsus.teams;

import java.util.Set;

import org.bukkit.command.CommandSender;

public class ListTeamsSubCommand implements TeamsSubCommand {

	private MagicJutsusTeams plugin;
	
	public ListTeamsSubCommand(MagicJutsusTeams plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean process(CommandSender sender, String[] args) {
		Set<String> names = this.plugin.getTeamNames();
		sender.sendMessage("Team Names");
		for (String name: names) {
			sender.sendMessage(name);
		}
		return true;
	}

}
