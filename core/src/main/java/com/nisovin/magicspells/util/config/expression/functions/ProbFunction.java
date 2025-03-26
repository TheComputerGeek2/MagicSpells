package com.nisovin.magicspells.util.config.expression.functions;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.Token;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionParameter;

import com.nisovin.magicspells.util.Name;

/**
 * A random 0-1 chance to return x or y otherwise.
 */
@Name("prob")
@FunctionParameter(name = "chance")
@FunctionParameter(name = "x", isLazy = true)
@FunctionParameter(name = "y", isLazy = true)
public class ProbFunction extends AbstractFunction {

	private static final Random RANDOM = ThreadLocalRandom.current();

	@Override
	public EvaluationValue evaluate(Expression expression, Token functionToken, EvaluationValue... args) throws EvaluationException {
		double chance = args[0].getNumberValue().doubleValue();
		EvaluationValue result = RANDOM.nextDouble() < chance ? args[1] : args[2];
		return expression.evaluateSubtree(result.getExpressionNode());
	}

}
