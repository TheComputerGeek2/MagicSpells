package com.nisovin.magicspells.commands;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import io.leangen.geantyref.TypeToken;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.incendo.cloud.Command;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.processors.requirements.Requirement;
import org.incendo.cloud.processors.requirements.Requirements;
import org.incendo.cloud.processors.requirements.RequirementPostprocessor;
import org.incendo.cloud.bukkit.parser.selector.MultiplePlayerSelectorParser;

import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.commands.exceptions.InvalidCommandArgumentException;
import com.nisovin.magicspells.util.Util;

public class ManaCommands {

	private static final CloudKey<Requirements<CommandSourceStack, ManaRequirement>> MANA_REQUIREMENT_KEY = CloudKey.of("mana_is_enabled", new TypeToken<>() {});
	private static final CloudKey<Collection<Player>> PLAYERS_KEY = CloudKey.of("players", new TypeToken<>() {});
	private static final CloudKey<Integer> AMOUNT_KEY = CloudKey.of("amount", Integer.class);

	static void register(PaperCommandManager<CommandSourceStack> manager) {
		manager.registerCommandPostProcessor(RequirementPostprocessor.of(
			MANA_REQUIREMENT_KEY,
			(context, requirement) -> {
				CommandSender sender = context.sender().getSender();
				sender.sendMessage(Component.text("The mana system is not enabled.", NamedTextColor.RED));
			}
		));

		CommandComponent.Builder<CommandSourceStack, Collection<Player>> playersComponent = CommandComponent
			.builder(
				PLAYERS_KEY,
				MultiplePlayerSelectorParser.<CommandSourceStack>multiplePlayerSelectorParser()
					.mapSuccess(
						new TypeToken<>() {},
						(context, selector) -> CompletableFuture.completedFuture(selector.values())
					)
			);

		CommandComponent.Builder<CommandSourceStack, Collection<Player>> optionalPlayersComponent = CommandComponent
			.builder(
				PLAYERS_KEY,
				MultiplePlayerSelectorParser.<CommandSourceStack>multiplePlayerSelectorParser()
					.mapSuccess(
						new TypeToken<>() {},
						(context, selector) -> CompletableFuture.completedFuture(selector.values())
					)
			)
			.optional(DefaultValue.failableDynamic(context -> {
				CommandSourceStack stack = context.sender();

				CommandSender sender = Objects.requireNonNullElse(stack.getExecutor(), stack.getSender());
				if (!(sender instanceof Player player)) {
					return ArgumentParseResult.failure(new InvalidCommandArgumentException(
						"A player must be specified"
					));
				}

				return ArgumentParseResult.success(Collections.singleton(player));
			}));

		Command.Builder<CommandSourceStack> base = manager.commandBuilder("ms", "magicspells")
			.literal("mana")
			.meta(MANA_REQUIREMENT_KEY, Requirements.of(new ManaRequirement()));

		manager.command(manager.commandBuilder("mana")
			.commandDescription(Description.of("Display your mana."))
			.meta(MANA_REQUIREMENT_KEY, Requirements.of(new ManaRequirement()))
			.permission(Perm.COMMAND_MANA_SHOW)
			.handler(ManaCommands::show)
		);

		manager.command(base
			.literal("show")
			.commandDescription(Description.of("Display your mana."))
			.permission(Perm.COMMAND_MANA_SHOW)
			.handler(ManaCommands::show)
		);

		manager.command(base
			.literal("add")
			.argument(playersComponent.description(Description.of("Selector for the player(s) to add the mana to.")))
			.required(AMOUNT_KEY, IntegerParser.integerParser(), Description.of("Amount of mana to add."))
			.commandDescription(Description.of("Add to the mana of players."))
			.permission(Perm.COMMAND_MANA_ADD)
			.handler(ManaCommands::add)
		);

		manager.command(base
			.literal("set")
			.argument(playersComponent.description(Description.of("Selector for the player(s) to set the mana for.")))
			.required(AMOUNT_KEY, IntegerParser.integerParser(), Description.of("Amount to set the mana to."))
			.commandDescription(Description.of("Set the mana of players."))
			.permission(Perm.COMMAND_MANA_SET)
			.handler(ManaCommands::set)
		);

		manager.command(base
			.literal("setmax")
			.argument(playersComponent.description(Description.of("Selector for the player(s) to set the max mana for.")))
			.required(AMOUNT_KEY, IntegerParser.integerParser(), Description.of("Amount to set the max mana to."))
			.commandDescription(Description.of("Set the max mana of players."))
			.permission(Perm.COMMAND_MANA_SET_MAX)
			.handler(ManaCommands::setMax)
		);

		manager.command(base
			.literal("reset")
			.argument(optionalPlayersComponent.description(Description.of("Selector for the player(s) to reset mana for.")))
			.commandDescription(Description.of("Reset the mana of players."))
			.permission(Perm.COMMAND_MANA_RESET)
			.handler(ManaCommands::reset)
		);

		manager.command(base
			.literal("updaterank")
			.argument(optionalPlayersComponent.description(Description.of("Selector for the player(s) to update the mana rank for.")))
			.commandDescription(Description.of("Update players' mana ranks, if possible."))
			.permission(Perm.COMMAND_MANA_UPDATE_RANK)
			.handler(ManaCommands::updateRank)
		);
	}

	private static void show(CommandContext<CommandSourceStack> context) {
		CommandSourceStack stack = context.sender();
		CommandSender sender = stack.getSender();

		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), sender);
		if (!(executor instanceof Player player)) {
			sender.sendMessage(Component.text("You must be a player in order to perform this command.", NamedTextColor.RED));
			return;
		}

		MagicSpells.getManaHandler().showMana(player, true);
	}

	private static void add(CommandContext<CommandSourceStack> context) {
		Collection<Player> players = context.get(PLAYERS_KEY);
		int amount = context.get(AMOUNT_KEY);

		ManaHandler handler = MagicSpells.getManaHandler();
		players.forEach(player -> handler.addMana(player, amount, ManaChangeReason.OTHER));

		context.sender().getSender().sendMessage(Util.getMessageText(
			Component.text(getName(players) + "mana was modified by " + amount + ".")
		));
	}

	private static void set(CommandContext<CommandSourceStack> context) {
		Collection<Player> players = context.get(PLAYERS_KEY);
		int amount = context.get(AMOUNT_KEY);

		ManaHandler handler = MagicSpells.getManaHandler();
		players.forEach(player -> handler.setMana(player, amount, ManaChangeReason.OTHER));

		context.sender().getSender().sendMessage(Util.getMessageText(
			Component.text(getName(players) + "mana was set to " + amount + ".")
		));
	}

	private static void setMax(CommandContext<CommandSourceStack> context) {
		Collection<Player> players = context.get(PLAYERS_KEY);
		int amount = context.get(AMOUNT_KEY);

		ManaHandler handler = MagicSpells.getManaHandler();
		players.forEach(player -> handler.setMaxMana(player, amount));

		context.sender().getSender().sendMessage(Util.getMessageText(
			Component.text(getName(players) + "max mana was set to " + amount + ".")
		));
	}

	private static void reset(CommandContext<CommandSourceStack> context) {
		Collection<Player> players = context.get(PLAYERS_KEY);

		ManaHandler handler = MagicSpells.getManaHandler();
		players.forEach(handler::createManaBar);

		context.sender().getSender().sendMessage(Util.getMessageText(
			Component.text(getName(players) + "mana was reset.")
		));
	}

	private static void updateRank(CommandContext<CommandSourceStack> context) {
		Collection<Player> players = context.get(PLAYERS_KEY);

		ManaHandler handler = MagicSpells.getManaHandler();
		boolean updated = players.stream().anyMatch(handler::updateManaRankIfNecessary);

		if (!updated) {
			context.sender().getSender().sendMessage(Util.getMessageText(
				Component.text(players.size() == 1 ?
					players.iterator().next().getName() + "'s mana rank was not updated." :
					"No players' mana ranks were updated."
				)
			));

			return;
		}

		context.sender().getSender().sendMessage(Util.getMessageText(
			Component.text(getName(players) + "mana rank was updated.")
		));
	}

	private static String getName(Collection<Player> players) {
		return players.size() == 1 ? players.iterator().next().getName() + "'s " : "The selected players' ";
	}

	private static class ManaRequirement implements Requirement<CommandSourceStack, ManaRequirement> {

		@Override
		public boolean evaluateRequirement(@NotNull CommandContext<CommandSourceStack> commandContext) {
			return MagicSpells.isManaSystemEnabled();
		}

	}

}
