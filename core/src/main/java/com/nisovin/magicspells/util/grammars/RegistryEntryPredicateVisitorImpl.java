package com.nisovin.magicspells.util.grammars;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;
import java.util.NoSuchElementException;

import net.kyori.adventure.key.InvalidKeyException;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.RegistryAccess;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.RegistryEntryPredicate;
import com.nisovin.magicspells.util.grammars.RegistryEntryPredicateParser.*;

public class RegistryEntryPredicateVisitorImpl<T extends Keyed> extends RegistryEntryPredicateBaseVisitor<Predicate<T>> {

	private final Registry<@NotNull T> registry;
	private final RegistryKey<T> registryKey;
	private final String originalText;

	public RegistryEntryPredicateVisitorImpl(@NotNull String originalText, @NotNull RegistryKey<T> registryKey) {
		this.originalText = originalText;
		this.registryKey = registryKey;

		registry = RegistryAccess.registryAccess().getRegistry(registryKey);
	}

	@Override
	public Predicate<T> visitParse(ParseContext ctx) {
		return ctx.expr.accept(this);
	}

	@Override
	public Predicate<T> visitParenthesis(ParenthesisContext ctx) {
		return ctx.expr.accept(this);
	}

	@Override
	public Predicate<T> visitNot(NotContext ctx) {
		return ctx.expr.accept(this).negate();
	}

	@Override
	public Predicate<T> visitAnd(AndContext ctx) {
		return AndPredicate.and(ctx.left.accept(this), ctx.right.accept(this));
	}

	@Override
	public Predicate<T> visitXor(XorContext ctx) {
		return XorPredicate.xor(ctx.left.accept(this), ctx.right.accept(this));
	}

	@Override
	public Predicate<T> visitOr(OrContext ctx) {
		return OrPredicate.or(ctx.left.accept(this), ctx.right.accept(this));
	}

	@SuppressWarnings({"UnstableApiUsage", "PatternValidation"})
	@Override
	public Predicate<T> visitTag(TagContext ctx) {
		String text = ctx.tag.getText();

		try {
			TagKey<T> tagKey = registryKey.tagKey(text);
			Tag<@NotNull T> tag = registry.getTag(tagKey);

			return entry -> tag.contains(TypedKey.create(registryKey, entry.key()));
		} catch (InvalidKeyException | NoSuchElementException e) {
			MagicSpells.error("Invalid tag '" + text + "' at line " + ctx.start.getLine() + " position " + ctx.start.getCharPositionInLine() + " of '" + RegistryEntryPredicate.getRegistryName(registryKey) + "' filter '" + originalText + "'.");
			return entry -> false;
		}
	}

	@Override
	public Predicate<T> visitEntry(EntryContext ctx) {
		String text = ctx.entry.getText();

		NamespacedKey key = NamespacedKey.fromString(text);
		if (key == null || registry.get(key) == null) {
			MagicSpells.error("Invalid entry '" + text + "' at line " + ctx.start.getLine() + " position " + ctx.start.getCharPositionInLine() + " of '" + RegistryEntryPredicate.getRegistryName(registryKey) + "' filter '" + originalText + "'.");
			return entry -> false;
		}

		return entry -> key.equals(entry.key());
	}

}
