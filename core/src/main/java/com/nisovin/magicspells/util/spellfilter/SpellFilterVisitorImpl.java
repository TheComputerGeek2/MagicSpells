package com.nisovin.magicspells.util.spellfilter;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;

public class SpellFilterVisitorImpl extends SpellFilterBaseVisitor<Predicate<Spell>> {

	private final String originalText;

	public SpellFilterVisitorImpl(String originalText) {
		this.originalText = originalText;
	}

	@Override
	public Predicate<Spell> visitParse(SpellFilterParser.ParseContext ctx) {
		return ctx.expr.accept(this);
	}

	@Override
	public Predicate<Spell> visitParenthesis(SpellFilterParser.ParenthesisContext ctx) {
		return ctx.expr.accept(this);
	}

	@Override
	public Predicate<Spell> visitNot(SpellFilterParser.NotContext ctx) {
		Predicate<Spell> expr = ctx.expr.accept(this);
		return spell -> !expr.test(spell);
	}

	@Override
	public Predicate<Spell> visitAnd(SpellFilterParser.AndContext ctx) {
		Predicate<Spell> left = ctx.left.accept(this);
		Predicate<Spell> right = ctx.right.accept(this);

		if (left instanceof AndPredicate(var leftList) && right instanceof AndPredicate(var rightList)) {
			leftList.addAll(rightList);
			return left;
		}

		if (left instanceof AndPredicate(var list)) {
			list.add(right);
			return left;
		}

		if (right instanceof AndPredicate(var list)) {
			list.add(left);
			return right;
		}

		List<Predicate<Spell>> list = new ArrayList<>();
		list.add(left);
		list.add(right);

		return new AndPredicate(list);
	}

	@Override
	public Predicate<Spell> visitXor(SpellFilterParser.XorContext ctx) {
		Predicate<Spell> left = ctx.left.accept(this);
		Predicate<Spell> right = ctx.right.accept(this);

		if (left instanceof XorPredicate(var leftList) && right instanceof XorPredicate(var rightList)) {
			leftList.addAll(rightList);
			return left;
		}

		if (left instanceof XorPredicate(var list)) {
			list.add(right);
			return left;
		}

		if (right instanceof XorPredicate(var list)) {
			list.add(left);
			return right;
		}

		List<Predicate<Spell>> list = new ArrayList<>();
		list.add(left);
		list.add(right);

		return new XorPredicate(list);
	}

	@Override
	public Predicate<Spell> visitOr(SpellFilterParser.OrContext ctx) {
		Predicate<Spell> left = ctx.left.accept(this);
		Predicate<Spell> right = ctx.right.accept(this);

		if (left instanceof OrPredicate(var leftList) && right instanceof OrPredicate(var rightList)) {
			leftList.addAll(rightList);
			return left;
		}

		if (left instanceof OrPredicate(var list)) {
			list.add(right);
			return left;
		}

		if (right instanceof OrPredicate(var list)) {
			list.add(left);
			return right;
		}

		List<Predicate<Spell>> list = new ArrayList<>();
		list.add(left);
		list.add(right);

		return new OrPredicate(list);
	}

	@Override
	public Predicate<Spell> visitTag(SpellFilterParser.TagContext ctx) {
		String tag = ctx.tag.getText();

		Set<Spell> spells = MagicSpells.getSpellsByTag().get(tag);
		if (spells.isEmpty()) {
			MagicSpells.error("Unused tag '" + tag + "' at line " + ctx.start.getLine() + " position " + ctx.start.getCharPositionInLine() + " of spell filter '" + originalText + "'.");
			return spell -> false;
		}

		return spells::contains;
	}

	@Override
	public Predicate<Spell> visitSpell(SpellFilterParser.SpellContext ctx) {
		String spellName = ctx.spell.getText();

		Spell s = MagicSpells.getSpellByInternalName(spellName);
		if (s == null) {
			MagicSpells.error("Invalid spell '" + spellName + "' at line " + ctx.spell.getLine() + " position " + ctx.spell.getCharPositionInLine() + " of spell filter '" + originalText + "'.");
			return spell -> false;
		}

		return spell -> s == spell;
	}

	private record AndPredicate(List<Predicate<Spell>> predicates) implements Predicate<Spell> {

		@Override
		public boolean test(Spell spell) {
			for (Predicate<Spell> predicate : predicates)
				if (!predicate.test(spell))
					return false;

			return true;
		}

	}

	private record OrPredicate(List<Predicate<Spell>> predicates) implements Predicate<Spell> {

		@Override
		public boolean test(Spell spell) {
			for (Predicate<Spell> predicate : predicates)
				if (predicate.test(spell))
					return true;

			return false;
		}

	}

	private record XorPredicate(List<Predicate<Spell>> predicates) implements Predicate<Spell> {

		@Override
		public boolean test(Spell spell) {
			boolean bool = predicates.getFirst().test(spell);

			for (int i = 1; i < predicates.size(); i++)
				bool ^= predicates.get(i).test(spell);

			return bool;
		}

	}

}
