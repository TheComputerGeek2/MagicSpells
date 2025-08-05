package com.nisovin.magicspells.spells;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.MagicConfig;

public abstract class CommandSpell extends Spell {

	public CommandSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public boolean canBind(CastItem item) {
		return false;
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
	public abstract List<String> tabComplete(CommandSender sender, String[] args);

}
