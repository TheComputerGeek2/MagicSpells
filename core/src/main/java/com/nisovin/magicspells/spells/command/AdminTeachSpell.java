package com.nisovin.magicspells.spells.command;

import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collection;

import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.incendo.cloud.parser.aggregate.AggregateParser;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.commands.parsers.SpellParser;
import com.nisovin.magicspells.commands.parsers.VarargsParser;

// NOTE: THIS DOES NOT PERFORM MANY SAFETY CHECKS, IT IS MEANT TO BE FAST FOR ADMINS
// NOTE: THIS CURRENTLY ONLY CASTS FROM CONSOLE

public class AdminTeachSpell extends CommandSpell {

	private static final SuggestionProvider<CommandSourceStack> suggestions = AggregateParser
		.<CommandSourceStack>builder()
		.withComponent("player", PlayerParser.playerParser())
		.withComponent("spells", VarargsParser.varargsParser(new SpellParser<>()))
		.withDirectMapper(Object.class, ((commandContext, context) -> new Object()))
		.build()
		.suggestionProvider();

	public AdminTeachSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public CastResult cast(SpellData data) {
		return new CastResult(PostCastAction.ALREADY_HANDLED, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		// FIXME add more descriptive messages
		// Need at least name and one node
		if (args.length < 2) return false;
		Player targetPlayer = Bukkit.getPlayer(args[0]);
		if (targetPlayer == null) return false;
		
		Spellbook spellbook = MagicSpells.getSpellbook(targetPlayer);
		// No target, TODO add messages

		String[] nodes = Arrays.copyOfRange(args, 1, args.length);
		Set<String> nodeSet = new HashSet<>(Arrays.asList(nodes));
		
		new AdminTeachTask(sender, spellbook, nodeSet).runTaskAsynchronously(MagicSpells.plugin);
		
		// Format should be <target> <node> <node> <...>
		return true;
	}

	@Override
	public SuggestionProvider<CommandSourceStack> suggestionProvider() {
		return suggestions;
	}

	private static class AdminTeachTask extends BukkitRunnable {
		
		private final CommandSender sender;
		private final Spellbook spellbook;
		private final Set<String> nodeSet;
		
		private AdminTeachTask(CommandSender sender, Spellbook spellbook, Set<String> nodeSet) {
			this.sender = sender;
			this.spellbook = spellbook;
			this.nodeSet = nodeSet;
		}
		
		@Override
		public void run() {
			// TODO can the retrieval of MagicSpells::spells be done async or does that need to be done sync?
			final Collection<Spell> spellCollection = SpellUtil.getSpellsByPermissionNames(MagicSpells.spells(), nodeSet);
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, () -> {
				if (spellbook.getSpells() == null) {
					sender.sendMessage("Target spellbook was destroyed before changes could be applied.");
					return;
				}
				spellCollection.forEach(spellbook::addSpell);
				spellbook.save();
				sender.sendMessage("Spell granting complete");
			});
		}
		
	}
	
}
