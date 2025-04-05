package com.nisovin.magicspells.util.config.expression.functions;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.Token;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionParameter;

import com.nisovin.magicspells.util.Name;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

@Name("pow")
@FunctionParameter(name = "base")
@FunctionParameter(name = "exponent")
public class PowFunction extends AbstractFunction {

	@Override
	public EvaluationValue evaluate(Expression expression, Token functionToken, EvaluationValue... args) throws EvaluationException {
		double base = args[0].getNumberValue().doubleValue();
		double exponent = args[1].getNumberValue().doubleValue();
		return expression.convertDoubleValue(AccurateMath.pow(base, exponent));
	}

}
