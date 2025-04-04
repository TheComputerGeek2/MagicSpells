package com.nisovin.magicspells.util.config.expression.functions;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.Token;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionParameter;

import com.nisovin.magicspells.util.Name;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

/**
 * Calculates the logarithm to an arbitrary base using the identity log(value, base) = ln(value)/ln(base)
 */
@Name("logb")
@FunctionParameter(name = "value")
@FunctionParameter(name = "base")
public class LogbFunction extends AbstractFunction {

	@Override
	public EvaluationValue evaluate(Expression expression, Token functionToken, EvaluationValue... args) throws EvaluationException {
		double value = AccurateMath.log(args[0].getNumberValue().doubleValue());
		double base = AccurateMath.log(args[1].getNumberValue().doubleValue());
		return expression.convertDoubleValue(value / base);
	}

}
