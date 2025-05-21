package com.nisovin.magicspells.commands.parsers;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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

	public static <C, T> ParserDescriptor<C, Collection<T>> varargsParser(@NonNull ArgumentParser<C, T> parser) {
		return ParserDescriptor.of(new VarargsParser<>(parser, null), new TypeToken<>() {});
	}

	public static <C, T> ParserDescriptor<C, Collection<T>> varargsParser(@NonNull ArgumentParser<C, T> parser, @Nullable SuggestionProvider<C> suggestionProvider) {
		return ParserDescriptor.of(new VarargsParser<>(parser, suggestionProvider), new TypeToken<>() {});
	}

	private final ArgumentParser<C, T> parser;
	private final SuggestionProvider<C> suggestionProvider;

	public VarargsParser(@NonNull ArgumentParser<C, T> parser, @Nullable SuggestionProvider<C> suggestionProvider) {
		this.parser = parser;
		this.suggestionProvider = suggestionProvider;
	}

	@Override
	public @NonNull ArgumentParseResult<@NonNull Collection<T>> parse(@NonNull CommandContext<@NonNull C> context, @NonNull CommandInput input) {
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
	public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<C> context, @NonNull CommandInput input) {
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
