package com.nisovin.magicjutsus.teams;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

public class CreateTeamSubCommand implements TeamsSubCommand {

	private MagicJutsusTeams plugin;
	
	public CreateTeamSubCommand(MagicJutsusTeams plugin) {
		this.plugin = plugin;
	}
	// teams.<teamname> is the section to make
	
	@Override
	public boolean process(CommandSender sender, String[] args) {
		// magicjutsusteams create <name>
		if (args.length >= 2) {
			String name = args[1];
			if (plugin.getTeamByName(name) != null) {
				sender.sendMessage('\'' + name + "' is already a team!");
				return true;
			}
			sender.sendMessage("Creating team '" + name + "' with membership permission 'magicjutsus.team." + name + '\'');
			if (makeTeam(name)) sender.sendMessage("Team successfully created! Use /cast reload to apply the changes");
			return true;
		}
		return false;
	}
	
	private boolean makeTeam(String name) {
		ConfigurationSection newTeamSec = plugin.getConfig().createSection("teams." + name);
		newTeamSec.set("permission", "magicjutsus.team." + name);
		newTeamSec.set("friendly-fire", false);
		plugin.saveConfig();
		return true;
	}

}
