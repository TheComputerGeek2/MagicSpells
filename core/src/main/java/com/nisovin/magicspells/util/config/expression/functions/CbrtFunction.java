package com.nisovin.magicspells.util.config.expression.functions;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.Token;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionParameter;

import com.nisovin.magicspells.util.Name;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

@Name("cbrt")
@FunctionParameter(name = "value")
public class CbrtFunction extends AbstractFunction {

	@Override
	public EvaluationValue evaluate(Expression expression, Token functionToken, EvaluationValue... args) throws EvaluationException {
		double x = args[0].getNumberValue().doubleValue();
		return expression.convertDoubleValue(AccurateMath.cbrt(x));
	}

}
