package com.nisovin.magicspells.commands;

import org.incendo.cloud.Command;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;

public class HelpCommand {

	public static CloudKey<Boolean> FILTER_FROM_HELP = CloudKey.of("__filter_from_help__", Boolean.class);

	public static void register(PaperCommandManager<CommandSourceStack> manager) {
		MinecraftHelp<CommandSourceStack> minecraftHelp = MinecraftHelp.<CommandSourceStack>builder()
			.commandManager(manager)
			.audienceProvider(CommandSourceStack::getSender)
			.commandPrefix("/magicspells help")
			.commandFilter(command -> !command.commandMeta().contains(FILTER_FROM_HELP))
			.build();

		Command.Builder<CommandSourceStack> base = manager.commandBuilder("ms", "magicspells");

		manager.command(base
			.meta(FILTER_FROM_HELP, true)
			.permission(Perm.COMMAND_HELP)
			.handler(context -> minecraftHelp.queryCommands("", context.sender()))
		);

		Command<CommandSourceStack> helpCommand = base
			.literal("help")
			.optional(
				"command",
				StringParser.greedyStringParser(),
				DefaultValue.constant(""),
				Description.of("Shows syntax and usage for the queried command.")
			)
			.commandDescription(Description.of("Display command help."))
			.permission(Perm.COMMAND_HELP)
			.handler(context -> minecraftHelp.queryCommands(context.get("command"), context.sender()))
			.build();

		manager.command(helpCommand);

		manager.command(base
			.literal("?")
			.meta(FILTER_FROM_HELP, true)
			.proxies(helpCommand)
		);
	}

}
