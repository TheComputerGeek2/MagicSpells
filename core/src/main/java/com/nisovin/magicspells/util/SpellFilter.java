package com.nisovin.magicspells.util;

import java.util.*;
import java.util.function.Predicate;

import com.google.common.collect.SetMultimap;
import com.google.common.collect.ImmutableSet;

import org.antlr.v4.runtime.*;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.spellfilter.SpellFilterLexer;
import com.nisovin.magicspells.util.spellfilter.SpellFilterParser;
import com.nisovin.magicspells.util.spellfilter.SpellFilterVisitorImpl;

public class SpellFilter {

	private static final ANTLRErrorListener LEXER_LISTENER = new BaseErrorListener() {

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
			throw new RuntimeException("Lexer error on line " + line + " position " + charPositionInLine + ": " + msg, e);
		}

	};

	private static final ANTLRErrorListener PARSER_LISTENER = new BaseErrorListener() {

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
			throw new RuntimeException("Parser error on line " + line + " position " + charPositionInLine + ": " + msg, e);
		}

	};

	private final Set<Spell> spells;

	private SpellFilter() {
		spells = null;
	}

	private SpellFilter(Set<Spell> spells) {
		this.spells = spells;
	}

	public SpellFilter(List<String> allowedSpells, List<String> blacklistedSpells, List<String> allowedTags, List<String> disallowedTags) {
		if ((allowedSpells == null || allowedSpells.isEmpty()) &&
			(blacklistedSpells == null || blacklistedSpells.isEmpty()) &&
			(allowedTags == null || allowedTags.isEmpty()) &&
			(disallowedTags == null || disallowedTags.isEmpty())
		) {
			spells = null;
			return;
		}

		SetMultimap<String, Spell> spellsByTag = MagicSpells.getSpellsByTag();
		Set<Spell> spells = new HashSet<>(MagicSpells.getSpellsOrdered());

		if (allowedTags != null && !allowedTags.isEmpty()) {
			Set<Spell> toRetain = new HashSet<>();

			for (String tag : allowedTags) {
				Set<Spell> taggedSpells = spellsByTag.get(tag);
				if (taggedSpells.isEmpty()) {
					MagicSpells.error("Unused tag '" + tag + "' found in spell filter.");
					continue;
				}

				toRetain.addAll(taggedSpells);
			}

			spells.retainAll(toRetain);
		}

		if (disallowedTags != null && !disallowedTags.isEmpty()) {
			for (String tag : disallowedTags) {
				Set<Spell> taggedSpells = spellsByTag.get(tag);
				if (taggedSpells.isEmpty()) {
					MagicSpells.error("Unused tag '" + tag + "' found in spell filter.");
					continue;
				}

				spells.removeAll(taggedSpells);
			}
		}

		if (blacklistedSpells != null && !blacklistedSpells.isEmpty()) {
			for (String spellName : blacklistedSpells) {
			    Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell == null) {
					MagicSpells.error("Invalid spell '" + spellName + "' found in spell filter.");
					continue;
				}

				spells.remove(spell);
			}
		}

		if (allowedSpells != null && !allowedSpells.isEmpty()) {
			for (String spellName : allowedSpells) {
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell == null) {
					MagicSpells.error("Invalid spell '" + spellName + "' found in spell filter.");
					continue;
				}

				spells.add(spell);
			}
		}

		this.spells = ImmutableSet.copyOf(spells);
	}

	public boolean check(Spell spell) {
		return spell != null && (spells == null || spells.contains(spell));
	}

	public boolean isEmpty() {
		return spells == null;
	}

	/**
	 * Create a {@link SpellFilter} instance out of a configuration section.
	 * @param config Reads from the following keys: "spells", "denied-spells", "spell-tags", and "denied-spell-tags".
	 * @param path Path for the keys to be read from. If the path is set to something like "filter", the keys will
	 *             be read from the passed config section under the "filter" section.
	 */
	public static SpellFilter fromConfig(ConfigurationSection config, String path) {
		if (!path.isEmpty() && !path.endsWith(".")) path += ".";

		List<String> spells = config.getStringList(path + "spells");
		List<String> deniedSpells = config.getStringList(path + "denied-spells");
		List<String> spellTags = config.getStringList(path + "spell-tags");
		List<String> deniedSpellTags = config.getStringList(path + "denied-spell-tags");

		return new SpellFilter(spells, deniedSpells, spellTags, deniedSpellTags);
	}

	/**
	 * Create a {@link SpellFilter} instance out of a configuration section supporting a legacy format.
	 * @param config Reads from the normal keys as well as the following keys:
	 *               "allowed-spells", "disallowed-spells","allowed-spell-tags", and "disallowed-spell-tags".
	 * @param path Path for the keys to be read from. If the path is set to something like "filter", the keys will
	 *             be read from the passed config section under the "filter" section.
	 */
	public static SpellFilter fromLegacyConfig(ConfigurationSection config, String path) {
		if (!path.isEmpty() && !path.endsWith(".")) path += ".";

		List<String> spells = mergeLists(config, path, "spells", "allowed-spells");
		List<String> deniedSpells = mergeLists(config, path, "denied-spells", "disallowed-spells");
		List<String> spellTags = mergeLists(config, path, "spell-tags", "allowed-spell-tags");
		List<String> deniedSpellTags = mergeLists(config, path, "denied-spell-tags", "disallowed-spell-tags");

		return new SpellFilter(spells, deniedSpells, spellTags, deniedSpellTags);
	}

	private static List<String> mergeLists(ConfigurationSection config, String path, String key, String legacyKey) {
		List<String> list = config.getStringList(path + key);
		list.addAll(config.getStringList(path + legacyKey));
		return list;
	}

	/**
	 * Create a {@link SpellFilter} instance out of a formatted string.
	 * @param string Follows format: allowedSpell, !disallowedSpell, tag:allowedTag, !tag:disallowedTag
	 */
	public static SpellFilter fromLegacyString(String string) {
		string = string.trim();

		if (!string.contains(",") && !string.startsWith("tag:") && !string.startsWith("!tag:"))
			return fromString(string);

		List<String> spells = new ArrayList<>();
		List<String> deniedSpells = new ArrayList<>();
		List<String> spellTags = new ArrayList<>();
		List<String> deniedSpellTags = new ArrayList<>();

		for (String s : string.split(",")) {
			boolean denied = false;
			s = s.trim();

			if (s.startsWith("!")) {
				s = s.substring(1);
				denied = true;
			}

			if (s.toLowerCase().startsWith("tag:")) {
				s = s.substring(4);
				if (denied) deniedSpellTags.add(s);
				else spellTags.add(s);
			} else if (s.startsWith("#")) {
				s = s.substring(1);
				if (denied) deniedSpellTags.add(s);
				else spellTags.add(s);
			} else {
				if (denied) deniedSpells.add(s);
				else spells.add(s);
			}
		}

		return new SpellFilter(spells, deniedSpells, spellTags, deniedSpellTags);
	}

	public static SpellFilter fromString(String string) {
		string = string.trim();

		if (string.isEmpty() || string.equals("*")) return new SpellFilter();

		try {
			SpellFilterLexer lexer = new SpellFilterLexer(CharStreams.fromString(string));
			lexer.removeErrorListeners();
			lexer.addErrorListener(LEXER_LISTENER);

			SpellFilterParser parser = new SpellFilterParser(new CommonTokenStream(lexer));
			parser.removeErrorListeners();
			parser.addErrorListener(PARSER_LISTENER);

			SpellFilterVisitorImpl visitor = new SpellFilterVisitorImpl(string);
			Predicate<Spell> predicate = visitor.visit(parser.parse());

			Set<Spell> spells = MagicSpells.getSpellsOrdered()
				.stream()
				.filter(predicate)
				.collect(ImmutableSet.toImmutableSet());

			if (spells.isEmpty())
				MagicSpells.error("Spell filter '" + string + "' matches no spells.");

			return new SpellFilter(spells);
		} catch (Exception e) {
			MagicSpells.error("Encountered an error while parsing spell filter '" + string + "'");
			e.printStackTrace();

			return new SpellFilter();
		}
	}

}
