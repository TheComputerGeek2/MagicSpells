package com.nisovin.magicspells.util.grammars;

import java.util.Set;
import java.util.function.Predicate;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import static com.nisovin.magicspells.util.grammars.SpellFilterParser.*;

public class SpellFilterVisitorImpl extends SpellFilterBaseVisitor<Predicate<Spell>> {

	private final String originalText;

	public SpellFilterVisitorImpl(String originalText) {
		this.originalText = originalText;
	}

	@Override
	public Predicate<Spell> visitParse(ParseContext ctx) {
		return ctx.expr.accept(this);
	}

	@Override
	public Predicate<Spell> visitParenthesis(ParenthesisContext ctx) {
		return ctx.expr.accept(this);
	}

	@Override
	public Predicate<Spell> visitNot(NotContext ctx) {
		return ctx.expr.accept(this).negate();
	}

	@Override
	public Predicate<Spell> visitAnd(AndContext ctx) {
		return AndPredicate.and(ctx.left.accept(this), ctx.right.accept(this));
	}

	@Override
	public Predicate<Spell> visitXor(XorContext ctx) {
		return XorPredicate.xor(ctx.left.accept(this), ctx.right.accept(this));
	}

	@Override
	public Predicate<Spell> visitOr(OrContext ctx) {
		return OrPredicate.or(ctx.left.accept(this), ctx.right.accept(this));
	}

	@Override
	public Predicate<Spell> visitTag(TagContext ctx) {
		String tag = ctx.tag.getText();

		Set<Spell> spells = MagicSpells.getSpellsByTag().get(tag);
		if (spells.isEmpty()) {
			MagicSpells.error("Unused tag '" + tag + "' at line " + ctx.start.getLine() + " position " + ctx.start.getCharPositionInLine() + " of spell filter '" + originalText + "'.");
			return spell -> false;
		}

		return spells::contains;
	}

	@Override
	public Predicate<Spell> visitSpell(SpellContext ctx) {
		String spellName = ctx.spell.getText();

		Spell s = MagicSpells.getSpellByInternalName(spellName);
		if (s == null) {
			MagicSpells.error("Invalid spell '" + spellName + "' at line " + ctx.spell.getLine() + " position " + ctx.spell.getCharPositionInLine() + " of spell filter '" + originalText + "'.");
			return spell -> false;
		}

		return spell -> s == spell;
	}

}
