package com.nisovin.magicspells.commands.parsers;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.concurrent.CompletableFuture;

import io.leangen.geantyref.TypeToken;

import com.google.common.collect.Iterables;

import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.incendo.cloud.execution.preprocessor.CommandPreprocessingContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.commands.CastCommands;
import com.nisovin.magicspells.commands.exceptions.InvalidCommandArgumentException;

public class SpellCastArgumentsParser implements ArgumentParser.FutureArgumentParser<CommandSourceStack, String[]>, SuggestionProvider<CommandSourceStack> {

	private static final CloudKey<Iterable<Suggestion>> PARSE_SUGGESTIONS = CloudKey.of("__parse_suggestions__", new TypeToken<>() {});
	private static final List<Suggestion> POWER_SUGGESTIONS = List.of(Suggestion.suggestion("-p"), Suggestion.suggestion("--power"));

	public static ParserDescriptor<CommandSourceStack, String[]> spellCastArgumentsParser() {
		return ParserDescriptor.of(new SpellCastArgumentsParser(), String[].class);
	}

	private final StringParser<CommandSourceStack> stringParser = new StringParser<>(StringParser.StringMode.QUOTED);

	@SuppressWarnings("unchecked")
	@Override
	public @NotNull CompletableFuture<@NotNull ArgumentParseResult<String[]>> parseFuture(@NotNull CommandContext<CommandSourceStack> context, @NotNull CommandInput input) {
		List<String> spellArguments = new ArrayList<>();

		int preParseCursor = input.cursor();
		while (input.hasRemainingInput()) {
			String value = input.peekString();
			if (value.isEmpty() || value.equals("-p") || (value.startsWith("--") && "--power".startsWith(value)))
				break;

			ArgumentParseResult<String> result = stringParser.parse(context, input);
			if (result.parsedValue().isPresent()) {
				spellArguments.add(result.parsedValue().get());
				input.skipWhitespace();
				continue;
			}

			Optional<Throwable> failure = result.failure();
			if (failure.isPresent() && failure.get() instanceof StringParser.StringParseException stringParseException) {
				return ArgumentParseResult.failureFuture(
					new InvalidCommandArgumentException("Invalid spell cast argument '" + stringParseException.input() + "'")
				);
			}

			return ArgumentParseResult.failureFuture(new InvalidCommandArgumentException("Invalid spell cast arguments"));
		}

		// Fail parsing if there would be suggestions.
		if (input.input().endsWith(" ")) {
			int parsedCursor = input.cursor();

			SuggestionProvider<CommandSourceStack> provider = context.get(CastCommands.SPELL_KEY).suggestionProvider();
			if (provider != SuggestionProvider.<CommandSourceStack>noSuggestions()) {
				return provider.suggestionsFuture(context, input.cursor(preParseCursor))
					.thenApply(result -> {
						if (Iterables.isEmpty(result)) {
							input.cursor(parsedCursor);

							context.store(PARSE_SUGGESTIONS, context.hasPermission(Perm.COMMAND_CAST_POWER) ?
								POWER_SUGGESTIONS : Collections.emptyList()
							);

							return ArgumentParseResult.success(spellArguments.toArray(new String[0]));
						}

						List<Suggestion> suggestions = MagicSpells
							.getCommandManager()
							.suggestionProcessor()
							.process(
								CommandPreprocessingContext.of(context, input.cursor(preParseCursor)),
								StreamSupport.stream(result.spliterator(), false)
							)
							.toList();

						context.store(PARSE_SUGGESTIONS, context.hasPermission(Perm.COMMAND_CAST_POWER) ?
							Iterables.concat(suggestions, POWER_SUGGESTIONS) : suggestions
						);

						if (suggestions.isEmpty()) {
							input.cursor(parsedCursor);
							return ArgumentParseResult.success(spellArguments.toArray(new String[0]));
						}

						return ArgumentParseResult.failure(
							new InvalidCommandArgumentException("Spell cast arguments should not contain trailing whitespace")
						);
					});
			}
		}

		return ArgumentParseResult.successFuture(spellArguments.toArray(new String[0]));
	}

	@Override
	public @NotNull CompletableFuture<@NotNull ? extends Iterable<@NotNull ? extends Suggestion>> suggestionsFuture(@NotNull CommandContext<CommandSourceStack> context, @NotNull CommandInput input) {
		if (context.contains(PARSE_SUGGESTIONS))
			return CompletableFuture.completedFuture(context.get(PARSE_SUGGESTIONS));

		return context.get(CastCommands.SPELL_KEY)
			.suggestionProvider()
			.suggestionsFuture(context, input)
			.thenApply(result -> context.hasPermission(Perm.COMMAND_CAST_POWER) ?
				Iterables.concat(result, POWER_SUGGESTIONS) : result
			);
	}

}
