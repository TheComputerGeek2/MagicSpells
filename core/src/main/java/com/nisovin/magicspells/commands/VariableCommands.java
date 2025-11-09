package com.nisovin.magicspells.commands;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import net.kyori.adventure.text.Component;

import org.incendo.cloud.Command;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;

import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.VariableMod;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.util.managers.VariableManager;
import com.nisovin.magicspells.variables.variabletypes.GlobalVariable;
import com.nisovin.magicspells.variables.variabletypes.GlobalStringVariable;
import com.nisovin.magicspells.commands.exceptions.InvalidCommandArgumentException;

public class VariableCommands {

	private static final CloudKey<SinglePlayerSelector> TARGET_PLAYER_KEY = CloudKey.of("player", SinglePlayerSelector.class);
	private static final CloudKey<String> VARIABLE_MOD_KEY = CloudKey.of("variable mod", String.class);
	private static final CloudKey<String> VARIABLE_KEY = CloudKey.of("variable", String.class);

	static void register(@NotNull PaperCommandManager<CommandSourceStack> manager) {
		Command.Builder<CommandSourceStack> base = manager.commandBuilder("ms", "magicspells").literal("variable");

		var variableComponent = CommandComponent.<CommandSourceStack, String>builder()
			.key(VARIABLE_KEY)
			.parser(StringParser.stringParser())
			.suggestionProvider(SuggestionProvider.suggestingStrings(
				() -> MagicSpells.getVariableManager().getVariables().keySet().iterator()
			));

		manager.command(base
			.literal("show")
			.argument(variableComponent.description(Description.of("The variable to show.")))
			.optional(TARGET_PLAYER_KEY, SinglePlayerSelectorParser.singlePlayerSelectorParser())
			.permission(Perm.COMMAND_VARIABLE_SHOW)
			.handler(VariableCommands::show)
		);

		Command.Builder<CommandSourceStack> modify = base
			.literal("modify")
			.argument(variableComponent.description(Description.of("The variable to modify.")))
			.permission(Perm.COMMAND_VARIABLE_MODIFY);

		manager.command(modify
			.literal("*")
			.required(
				VARIABLE_MOD_KEY,
				StringParser.greedyStringParser(),
				Description.of("A variable modifier.")
			)
			.meta(HelpCommand.FILTER_FROM_HELP, true)
			.handler(VariableCommands::modify)
		);

		manager.command(modify
			.literal("-")
			.required(VARIABLE_MOD_KEY, StringParser.greedyStringParser())
			.meta(HelpCommand.FILTER_FROM_HELP, true)
			.handler(VariableCommands::modify)
		);

		manager.command(modify
			.required(
				TARGET_PLAYER_KEY,
				SinglePlayerSelectorParser.singlePlayerSelectorParser(),
				Description.of("The player whose variable you wish to modify. Use * or - for global variables.")
			)
			.required(
				VARIABLE_MOD_KEY,
				StringParser.greedyStringParser(),
				Description.of("The variable modifier.")
			)
			.handler(VariableCommands::modify)
		);
	}

	private static void show(CommandContext<CommandSourceStack> context) {
		String variableName = context.get(VARIABLE_KEY);

		Variable variable = MagicSpells.getVariableManager().getVariable(variableName);
		if (variable == null)
			throw new InvalidCommandArgumentException("No matching variable found: '" + variableName + "'");

		SinglePlayerSelector selector = context.getOrDefault(TARGET_PLAYER_KEY, null);
		Player player = selector == null ? null : selector.single();

		if (player == null && !(variable instanceof GlobalVariable || variable instanceof GlobalStringVariable)) {
			CommandSourceStack stack = context.sender();
			CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), stack.getSender());
			if (!(executor instanceof Player sender))
				throw new InvalidCommandArgumentException("Player must be specified for non-global variables");

			player = sender;
		}

		String name = player == null ? null : player.getName();
		String currentValue = variable.getStringValue(name);

		String message = (player == null ? "Variable" : name + "'s variable") + " value for " + variableName + " is: " + currentValue;
		context.sender().getSender().sendMessage(Util.getMessageText(Component.text(message)));
	}

	private static void modify(CommandContext<CommandSourceStack> context) {
		VariableManager variableManager = MagicSpells.getVariableManager();
		String variableName = context.get(VARIABLE_KEY);

		Variable variable = variableManager.getVariable(variableName);
		if (variable == null)
			throw new InvalidCommandArgumentException("No matching variable found: '" + variableName + "'");

		SinglePlayerSelector selector = context.getOrDefault(TARGET_PLAYER_KEY, null);
		Player player = selector == null ? null : selector.single();

		if (player == null && !(variable instanceof GlobalVariable || variable instanceof GlobalStringVariable))
			throw new InvalidCommandArgumentException("Player must be specified for non-global variables");

		String name = player == null ? null : player.getName();

		String variableModString = context.get(VARIABLE_MOD_KEY);
		VariableMod variableMod;
		try {
			variableMod = new VariableMod(variableModString);
		} catch (Exception e) {
			throw new InvalidCommandArgumentException("Invalid variable mod '" + variableModString + "'", e);
		}

		String oldValue = variable.getStringValue(name);
		variableManager.processVariableMods(variable, variableMod, player, new SpellData(player));
		String newValue = variable.getStringValue(name);

		String message = (player == null ? "Variable" : name + "'s variable") + " value for " + variableName +
			" was modified from '" + oldValue + "' to '" + newValue + "'.";

		context.sender().getSender().sendMessage(Util.getMessageText(Component.text(message)));
	}

}
