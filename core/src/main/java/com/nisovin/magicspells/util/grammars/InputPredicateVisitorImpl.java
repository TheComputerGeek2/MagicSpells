package com.nisovin.magicspells.util.grammars;

import java.util.function.Predicate;

import org.bukkit.Input;

import static com.nisovin.magicspells.util.grammars.InputPredicateParser.*;

public class InputPredicateVisitorImpl extends InputPredicateBaseVisitor<Predicate<Input>> {

	@Override
	public Predicate<Input> visitParse(ParseContext ctx) {
		return ctx.expr.accept(this);
	}

	@Override
	public Predicate<Input> visitParenthesis(ParenthesisContext ctx) {
		return ctx.expr.accept(this);
	}

	@Override
	public Predicate<Input> visitNot(NotContext ctx) {
		return ctx.expr.accept(this).negate();
	}

	@Override
	public Predicate<Input> visitAnd(AndContext ctx) {
		return AndPredicate.and(ctx.left.accept(this), ctx.right.accept(this));
	}

	@Override
	public Predicate<Input> visitXor(XorContext ctx) {
		return XorPredicate.xor(ctx.left.accept(this), ctx.right.accept(this));
	}

	@Override
	public Predicate<Input> visitOr(OrContext ctx) {
		return OrPredicate.or(ctx.left.accept(this), ctx.right.accept(this));
	}

	@Override
	public Predicate<Input> visitInput(InputContext ctx) {
		return switch (ctx.input.token.getType()) {
			case InputPredicateLexer.FORWARD -> Input::isForward;
			case InputPredicateLexer.BACKWARD -> Input::isBackward;
			case InputPredicateLexer.LEFT -> Input::isLeft;
			case InputPredicateLexer.RIGHT -> Input::isRight;
			case InputPredicateLexer.JUMP -> Input::isJump;
			case InputPredicateLexer.SNEAK -> Input::isSneak;
			case InputPredicateLexer.SPRINT -> Input::isSprint;
			default -> throw new IllegalStateException("Unexpected value: " + ctx.start.getType());
		};
	}

}
