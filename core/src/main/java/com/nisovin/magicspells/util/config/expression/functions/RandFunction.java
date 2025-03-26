package com.nisovin.magicspells.util.config.expression.functions;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.Token;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionParameter;

import com.nisovin.magicspells.util.Name;

/**
 * Generate a random number from min to max.
 */
@Name("rand")
@FunctionParameter(name = "min")
@FunctionParameter(name = "max")
public class RandFunction extends AbstractFunction {

	private static final Random RANDOM = ThreadLocalRandom.current();

	@Override
	public EvaluationValue evaluate(Expression expression, Token functionToken, EvaluationValue... args) {
		double min = args[0].getNumberValue().doubleValue();
		double max = args[1].getNumberValue().doubleValue();
		return expression.convertDoubleValue(RANDOM.nextDouble() * (max - min) + min);
	}

}
