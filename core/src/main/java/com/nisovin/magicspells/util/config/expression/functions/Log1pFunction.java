package com.nisovin.magicspells.util.config.expression.functions;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.Token;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionParameter;

import com.nisovin.magicspells.util.Name;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

@Name("log1p")
@FunctionParameter(name = "value")
public class Log1pFunction extends AbstractFunction {

	@Override
	public EvaluationValue evaluate(Expression expression, Token token, EvaluationValue... args) throws EvaluationException {
		double x = args[0].getNumberValue().doubleValue();
		return expression.convertDoubleValue(AccurateMath.log1p(x));
	}

}
