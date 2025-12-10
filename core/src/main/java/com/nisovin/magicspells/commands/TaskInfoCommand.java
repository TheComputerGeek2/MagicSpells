package com.nisovin.magicspells.commands;

import org.bukkit.Bukkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.volatilecode.VolatileCodeHandle;

public class TaskInfoCommand {

	public static void register(PaperCommandManager<CommandSourceStack> manager) {
		manager.command(manager.commandBuilder("ms", "magicspells")
			.literal("taskinfo")
			.commandDescription(Description.of("Display info for the tasks currently scheduled by MagicSpells."))
			.permission(Perm.COMMAND_TASKINFO)
			.handler(TaskInfoCommand::taskInfo)
		);
	}

	private static void taskInfo(CommandContext<CommandSourceStack> context) {
		long msTasks = Bukkit.getScheduler().getPendingTasks().stream()
			.filter(task -> MagicSpells.getInstance().equals(task.getOwner()))
			.count();

		int effectLibTasks = MagicSpells.getEffectManager().getEffects().size();

		VolatileCodeHandle handler = MagicSpells.getVolatileCodeHandler();
		MagicSpells instance = MagicSpells.getInstance();

		context.sender().getSender().sendMessage(Util.getMessageText(
			Component.text()
				.content("Tasks:")
				.appendNewline()
				.append(Component.text(" * Bukkit Scheduler - ").append(number(msTasks)))
				.appendNewline()
				.append(Component.text(" * Entity Scheduler - ").append(number(handler.countEntitySchedulerTasks())))
				.appendNewline()
				.append(Component.text(" * Global Region Scheduler - ").append(number(handler.countGlobalRegionSchedulerTasks())))
				.appendNewline()
				.append(Component.text(" * EffectLib - ").append(number(effectLibTasks)))
		));
	}

	private static Component number(long number) {
		if (number < 0) return Component.text("?", NamedTextColor.RED);
		return Component.text(number, number > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY);
	}

}
