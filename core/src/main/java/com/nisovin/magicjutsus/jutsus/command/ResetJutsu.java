package com.nisovin.magicjutsus.jutsus.command;

import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;

// PLANNED OPTIONS
// get jutsubook(s)
//     remove certain jutsus
//     remove all jutsus
//     remove bindings
// variables
//     remove certain variables
//     remove all variables
// MarkJutsu
//     remove certain
//     remove all
// KeybindJutsu
//     reset
// Jutsubook jutsus
//     remove by player/world/all

public class ResetJutsu extends CommandJutsu {
	
	public ResetJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
	}
	
	// Arg format should be <player[,player[,player...]]>|all

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
		
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}
	
}
