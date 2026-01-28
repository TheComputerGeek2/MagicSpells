package com.nisovin.magicspells.commands.parsers;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

import org.incendo.cloud.type.range.Range;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.standard.FloatParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import com.nisovin.magicspells.util.Angle;

/**
 * Parser that parses an {@link Angle} from a float.
 *
 * @param <C> Command sender type
 */
public final class AngleParser<C> implements ArgumentParser<C, Angle>, BlockingSuggestionProvider.Strings<C> {

	private static final Range<Integer> SUGGESTION_RANGE = Range.intRange(Integer.MIN_VALUE, Integer.MAX_VALUE);

	/**
	 * Creates a new angle parser.
	 *
	 * @param <C> command sender type
	 * @return the created parser
	 */
	public static <C> @NotNull ParserDescriptor<C, Angle> angleParser() {
		return ParserDescriptor.of(new AngleParser<>(), Angle.class);
	}

	/**
	 * Returns a {@link CommandComponent.Builder} using {@link #angleParser()} as the parser.
	 *
	 * @param <C> the command sender type
	 * @return the component builder
	 */
	public static <C> CommandComponent.@NotNull Builder<C, Angle> angleComponent() {
		return CommandComponent.<C, Angle>builder().parser(angleParser());
	}

	@Override
	public @NotNull ArgumentParseResult<@NotNull Angle> parse(final @NotNull CommandContext<@NotNull C> commandContext, final @NotNull CommandInput commandInput) {
		final String input = commandInput.skipWhitespace().peekString();

		final boolean relative;
		if (commandInput.peek() == '~') {
			relative = true;
			commandInput.moveCursor(1);
		} else {
			relative = false;
		}

		final float angle;
		try {
			final boolean empty = commandInput.peekString().isEmpty() || commandInput.peek() == ' ';
			angle = empty ? 0 : commandInput.readFloat();
		} catch (final Exception e) {
			return ArgumentParseResult.failure(new FloatParser.FloatParseException(input, new FloatParser<>(FloatParser.DEFAULT_MINIMUM, FloatParser.DEFAULT_MAXIMUM), commandContext));
		}

		return ArgumentParseResult.success(new Angle(angle, relative));
	}

	@Override
	public @NotNull Iterable<@NotNull String> stringSuggestions(final @NotNull CommandContext<C> commandContext, final @NotNull CommandInput input) {
		String prefix;
		if (input.hasRemainingInput() && input.peek() == '~') {
			prefix = "~";
			input.moveCursor(1);
		} else {
			prefix = "";
		}

		return IntegerParser.getSuggestions(SUGGESTION_RANGE, input).stream().map(string -> prefix + string).collect(Collectors.toList());
	}

}
