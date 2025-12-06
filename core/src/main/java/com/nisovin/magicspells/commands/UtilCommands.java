package com.nisovin.magicspells.commands;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Collection;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import org.incendo.cloud.Command;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.bukkit.data.SingleEntitySelector;
import org.incendo.cloud.bukkit.parser.selector.SingleEntitySelectorParser;

import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Entity;
import org.bukkit.command.CommandSender;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.commands.exceptions.InvalidCommandArgumentException;

public class UtilCommands {

	private static final CloudKey<SingleEntitySelector> TARGET_ENTITY_KEY = CloudKey.of("entity", SingleEntitySelector.class);

	static void register(@NotNull PaperCommandManager<CommandSourceStack> manager) {
		Command.Builder<CommandSourceStack> base = manager
			.commandBuilder("ms", "magicspells")
			.literal("util");

		manager.command(base
			.literal("listgoals")
			.required(TARGET_ENTITY_KEY, SingleEntitySelectorParser.singleEntitySelectorParser())
			.commandDescription(Description.of("List a mob's goals."))
			.permission(Perm.COMMAND_UTIL_LIST_GOALS)
			.handler(UtilCommands::listGoals)
		);
	}

	private static void listGoals(CommandContext<CommandSourceStack> context) {
		SingleEntitySelector selector = context.get(TARGET_ENTITY_KEY);

		CommandSourceStack stack = context.sender();
		CommandSender sender = Objects.requireNonNullElse(stack.getExecutor(), stack.getSender());

		Entity entity = selector.single();
		if (!(entity instanceof Mob mob))
			throw new InvalidCommandArgumentException("Invalid entity specified - target is not a mob");

		Collection<Goal<@NotNull Mob>> goals = Bukkit.getMobGoals().getAllGoals(mob);
		if (goals.isEmpty()) {
			sender.sendMessage(Util.getMessageText(Component.text("Target has no goals")));
			return;
		}

		TextComponent.Builder message = Component.text()
			.append(Component.text("Goals for "))
			.append(entity.name().hoverEvent(entity))
			.append(Component.text(":"));

		for (Goal<@NotNull Mob> goal : goals) {
			GoalKey<@NotNull Mob> key = goal.getKey();

			message.appendNewline();
			message.append(Component.text("  - " + key.getEntityClass().getSimpleName() + ": "));
			message.append(Component.text(key.getNamespacedKey().toString()));
			message.appendSpace();
			message.append(
				Component.text(
					goal.getTypes()
						.stream()
						.map(Enum::name)
						.collect(Collectors.joining(", ", "[", "]"))
				)
			);
		}

		sender.sendMessage(Util.getMessageText(message));
	}

}
