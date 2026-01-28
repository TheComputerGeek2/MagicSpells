package com.nisovin.magicspells.commands;

import java.util.Objects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;

import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.MagicXpHandler;

public class MagicXpCommand {

	public static void register(PaperCommandManager<CommandSourceStack> manager) {
		Command<CommandSourceStack> magicXpCommand = manager.commandBuilder("ms", "magicspells")
			.literal("magicxp")
			.commandDescription(Description.of("Display your magic xp."))
			.permission(Perm.COMMAND_MAGICXP)
			.handler(MagicXpCommand::magicXp)
			.build();

		manager.command(magicXpCommand);

		manager.command(manager.commandBuilder("magicxp")
			.commandDescription(Description.of("Display your magic xp."))
			.proxies(magicXpCommand)
		);
	}

	private static void magicXp(CommandContext<CommandSourceStack> context) {
		CommandSourceStack stack = context.sender();
		CommandSender sender = stack.getSender();

		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), sender);
		if (!(executor instanceof Player player)) {
			sender.sendMessage(Component.text("You must be a player in order to perform this command.", NamedTextColor.RED));
			return;
		}

		MagicXpHandler xpHandler = MagicSpells.getMagicXpHandler();
		if (xpHandler == null) {
			sender.sendMessage(Component.text("The magic xp system is not enabled.", NamedTextColor.RED));
			return;
		}

		xpHandler.showXpInfo(player);
	}

}
