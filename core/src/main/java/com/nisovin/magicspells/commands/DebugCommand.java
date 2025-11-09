package com.nisovin.magicspells.commands;

import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;

import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.standard.IntegerParser;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.Util;

public class DebugCommand {

	private static final CloudKey<Integer> LEVEL_KEY = CloudKey.of("level", Integer.class);

	public static void register(PaperCommandManager<CommandSourceStack> manager) {
		manager.command(manager.commandBuilder("ms", "magicspells")
			.literal("debug")
			.commandDescription(Description.of("Toggle debug mode, or set current debug level."))
			.optional(LEVEL_KEY, IntegerParser.integerParser(), Description.of("Enable debug, and set debug level to this value."))
			.permission(Perm.COMMAND_DEBUG)
			.handler(DebugCommand::debug)
		);
	}

	private static void debug(CommandContext<CommandSourceStack> context) {
		Integer level = context.getOrDefault(LEVEL_KEY, null);
		if (level == null) {
			MagicSpells.setDebugLevel(MagicSpells.getDebugLevelOriginal());
			MagicSpells.setDebug(!MagicSpells.isDebug());
			sendOutput(context);
			return;
		}

		MagicSpells.setDebugLevel(level);
		MagicSpells.setDebug(true);
		sendOutput(context);
	}

	private static void sendOutput(CommandContext<CommandSourceStack> context) {
		boolean debug = MagicSpells.isDebug();
		int level = MagicSpells.getDebugLevel();

		ComponentLike status;
		if (debug) {
			status = Component.text()
				.append(Component.text("enabled", NamedTextColor.GREEN))
				.append(Component.text(" (level: "))
				.append(Component.text(level, NamedTextColor.GREEN))
				.append(Component.text(")"));
		} else {
			status = Component.text("disabled", NamedTextColor.RED);
		}

		CommandSender sender = context.sender().getSender();
		sender.sendMessage(Util.getMessageText(
			Component.text()
				.append(Component.text("Debug is now "))
				.append(status)
				.append(Component.text("."))
		));
	}

}
