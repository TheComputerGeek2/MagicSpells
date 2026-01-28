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
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import com.nisovin.magicspells.util.Angle;
import com.nisovin.magicspells.util.Rotation;
import com.nisovin.magicspells.commands.exceptions.InvalidCommandArgumentException;

/**
 * Parser that parses a {@link Rotation} from two floats.
 *
 * @param <C> Command sender type
 */
public final class RotationParser<C> implements ArgumentParser<C, Rotation>, BlockingSuggestionProvider.Strings<C> {

	private static final Range<Integer> SUGGESTION_RANGE = Range.intRange(Integer.MIN_VALUE, Integer.MAX_VALUE);

	private final AngleParser<C> angleParser = new AngleParser<>();

	/**
	 * Creates a new rotation parser.
	 *
	 * @param <C> command sender type
	 * @return the created parser
	 */
	public static <C> @NotNull ParserDescriptor<C, Rotation> rotationParser() {
		return ParserDescriptor.of(new RotationParser<>(), Rotation.class);
	}

	/**
	 * Returns a {@link CommandComponent.Builder} using {@link #rotationParser()} as the parser.
	 *
	 * @param <C> the command sender type
	 * @return the component builder
	 */
	public static <C> CommandComponent.@NotNull Builder<C, Rotation> rotationComponent() {
		return CommandComponent.<C, Rotation>builder().parser(rotationParser());
	}

	@Override
	public @NotNull ArgumentParseResult<@NotNull Rotation> parse(final @NotNull CommandContext<C> commandContext, final @NotNull CommandInput commandInput) {
		if (commandInput.remainingTokens() < 2) {
			return ArgumentParseResult.failure(new InvalidCommandArgumentException(
				"Invalid rotation specified. Required format is '<yaw> <pitch>'"
			));
		}

		Angle[] angles = new Angle[2];
		for (int i = 0; i < 2; i++) {
			if (commandInput.peekString().isEmpty()) {
				return ArgumentParseResult.failure(new InvalidCommandArgumentException(
					"Invalid rotation specified. Required format is '<yaw> <pitch>'"
				));
			}

			ArgumentParseResult<Angle> angle = this.angleParser.parse(commandContext, commandInput);
			if (angle.failure().isPresent()) {
				return ArgumentParseResult.failure(angle.failure().get());
			}

			angles[i] = angle.parsedValue().orElseThrow(NullPointerException::new);
		}

		return ArgumentParseResult.success(Rotation.of(angles[0], angles[1]));
	}

	@Override
	public @NotNull Iterable<@NotNull String> stringSuggestions(final @NotNull CommandContext<C> commandContext, final @NotNull CommandInput input) {
		final CommandInput inputCopy = input.copy();

		int idx = input.cursor();
		for (int i = 0; i < 2; i++) {
			input.skipWhitespace();
			idx = input.cursor();

			if (!input.hasRemainingInput()) break;

			ArgumentParseResult<Angle> angle = this.angleParser.parse(commandContext, input);

			if (angle.failure().isPresent() || !input.hasRemainingInput()) break;
		}
		input.cursor(idx);

		if (input.hasRemainingInput() && input.peek() == '~') input.moveCursor(1);

		String prefix = inputCopy.difference(input, true);
		return IntegerParser.getSuggestions(SUGGESTION_RANGE, input).stream().map(string -> prefix + string).collect(Collectors.toList());
	}

}
