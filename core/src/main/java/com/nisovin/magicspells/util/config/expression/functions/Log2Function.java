package com.nisovin.magicspells.util.config.expression.functions;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.Token;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionParameter;

import com.nisovin.magicspells.util.Name;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

@Name("log2")
@FunctionParameter(name = "value")
public class Log2Function extends AbstractFunction {

	private static final Double LOG2 = AccurateMath.log(2);

	@Override
	public EvaluationValue evaluate(Expression expression, Token token, EvaluationValue... args) throws EvaluationException {
		double x = args[0].getNumberValue().doubleValue();
		return expression.convertDoubleValue(AccurateMath.log(x) / LOG2);
	}

}
