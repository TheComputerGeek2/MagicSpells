package com.nisovin.magicspells.util;

import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import org.bukkit.Input;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.grammars.*;

public class InputPredicate implements Predicate<Input> {

	private final Predicate<Input> predicate;

	public InputPredicate(Predicate<Input> predicate) {
		this.predicate = predicate;
	}

	@Override
	public boolean test(Input input) {
		return predicate.test(input);
	}

	public static InputPredicate fromString(@Nullable String inputString) {
		if (inputString == null || inputString.isEmpty()) return null;

		try {
			InputPredicateLexer lexer = new InputPredicateLexer(CharStreams.fromString(inputString));
			lexer.removeErrorListeners();
			lexer.addErrorListener(GrammarUtils.LEXER_LISTENER);

			InputPredicateParser parser = new InputPredicateParser(new CommonTokenStream(lexer));
			parser.removeErrorListeners();
			parser.addErrorListener(GrammarUtils.PARSER_LISTENER);

			InputPredicateVisitorImpl visitor = new InputPredicateVisitorImpl();
			Predicate<Input> predicate = visitor.visit(parser.parse());

			return new InputPredicate(predicate);
		} catch (Exception e) {
			MagicSpells.error("Encountered an error while parsing input predicate '" + inputString + "'");
			e.printStackTrace();

			return null;
		}
	}

}
