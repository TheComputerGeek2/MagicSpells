package com.nisovin.magicspells.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import org.bukkit.Keyed;

import io.papermc.paper.registry.RegistryKey;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.grammars.GrammarUtils;
import com.nisovin.magicspells.util.grammars.RegistryEntryPredicateLexer;
import com.nisovin.magicspells.util.grammars.RegistryEntryPredicateParser;
import com.nisovin.magicspells.util.grammars.RegistryEntryPredicateVisitorImpl;

public final class RegistryEntryPredicate {

	public static <T extends Keyed> Predicate<T> fromString(@NotNull RegistryKey<T> registryKey, @Nullable String string) {
		if (string == null || string.isEmpty()) return null;

		try {
			RegistryEntryPredicateLexer lexer = new RegistryEntryPredicateLexer(CharStreams.fromString(string));
			lexer.removeErrorListeners();
			lexer.addErrorListener(GrammarUtils.LEXER_LISTENER);

			RegistryEntryPredicateParser parser = new RegistryEntryPredicateParser(new CommonTokenStream(lexer));
			parser.removeErrorListeners();
			parser.addErrorListener(GrammarUtils.PARSER_LISTENER);

			RegistryEntryPredicateVisitorImpl<@NotNull T> visitor = new RegistryEntryPredicateVisitorImpl<>(string, registryKey);
			return visitor.visit(parser.parse());
		} catch (Exception e) {
			MagicSpells.error("Encountered an error while parsing " + getRegistryName(registryKey) + " predicate '" + string + "'");
			e.printStackTrace();

			return null;
		}
	}

	public static String getRegistryName(@NotNull RegistryKey<?> key) {
		String name = key.key().value();
		return name.replace('_', ' ');
	}

}
