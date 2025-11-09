package com.nisovin.magicspells.commands;

import net.kyori.adventure.text.Component;

import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;

public class ProfileReportCommand {

	public static void register(PaperCommandManager<CommandSourceStack> manager) {
		manager.command(manager.commandBuilder("ms", "magicspells")
			.literal("profilereport")
			.commandDescription(Description.of("Save profile report to a file."))
			.permission(Perm.COMMAND_PROFILE_REPORT)
			.handler(ProfileReportCommand::profileReport)
		);
	}

	private static void profileReport(CommandContext<CommandSourceStack> context) {
		MagicSpells.profilingReport();

		context.sender().getSender().sendMessage(Util.getMessageText(Component.text("Created profiling report.")));
	}

}
