package com.nisovin.magicspells.commands.parsers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import io.leangen.geantyref.TypeToken;

import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.suggestion.SuggestionProvider;

public class VarargsParser<C, T> implements ArgumentParser<C, Collection<T>>, SuggestionProvider<C> {

	public static <C, T> ParserDescriptor<C, Collection<T>> varargsParser(@NotNull ArgumentParser<C, T> parser) {
		return ParserDescriptor.of(new VarargsParser<>(parser, null), new TypeToken<>() {});
	}

	public static <C, T> ParserDescriptor<C, Collection<T>> varargsParser(@NotNull ArgumentParser<C, T> parser, @Nullable SuggestionProvider<C> suggestionProvider) {
		return ParserDescriptor.of(new VarargsParser<>(parser, suggestionProvider), new TypeToken<>() {});
	}

	private final ArgumentParser<C, T> parser;
	private final SuggestionProvider<C> suggestionProvider;

	public VarargsParser(@NotNull ArgumentParser<C, T> parser, @Nullable SuggestionProvider<C> suggestionProvider) {
		this.parser = parser;
		this.suggestionProvider = suggestionProvider;
	}

	@Override
	public @NotNull ArgumentParseResult<@NotNull Collection<T>> parse(@NotNull CommandContext<@NotNull C> context, @NotNull CommandInput input) {
		List<T> arguments = new ArrayList<>();

		while (input.hasRemainingInput()) {
			ArgumentParseResult<T> result = parser.parse(context, input);
			if (result.parsedValue().isPresent()) {
				arguments.add(result.parsedValue().get());
				input.skipWhitespace();

				continue;
			}

			return ArgumentParseResult.failure(result.failure().get());
		}

		return ArgumentParseResult.success(arguments);
	}

	@Override
	public @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestionsFuture(@NotNull CommandContext<C> context, @NotNull CommandInput input) {
		CommandInput inputCopy = input.copy();

		int idx = input.cursor();
		while (input.hasRemainingInput()) {
			input.skipWhitespace();
			idx = input.cursor();

			if (!input.hasRemainingInput()) break;

			ArgumentParseResult<T> result = parser.parse(context, input);
			if (result.failure().isPresent() || !input.hasRemainingInput()) break;
		}
		input.cursor(idx);

		String prefix = inputCopy.difference(input, true);

		return (suggestionProvider != null ? suggestionProvider : parser.suggestionProvider())
			.suggestionsFuture(context, input)
			.thenApply(suggestions -> {
				List<Suggestion> prefixed = new ArrayList<>();

				for (Suggestion suggestion : suggestions)
					prefixed.add(suggestion.withSuggestion(prefix + suggestion.suggestion()));

				return prefixed;
			});
	}

}
