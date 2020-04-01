package com.nisovin.magicjutsus.jutsus;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.MagicConfig;

public abstract class CommandJutsu extends Jutsu {

	public CommandJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
	}
	
	@Override
	public boolean canCastWithItem() {
		return false;
	}
	
	@Override
	public boolean canCastByCommand() {
		return true;
	}
	
	@Override
	public abstract boolean castFromConsole(CommandSender sender, String[] args);

	@Override
	public abstract List<String> tabComplete(CommandSender sender, String partial);
	
}
