package com.nisovin.magicspells.util.config.expression.functions;

import java.math.BigDecimal;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.Token;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionParameter;

import com.nisovin.magicspells.util.Name;

/**
 * <ul>
 *   <li>If value < 0: return x</li>
 *   <li>If value == 0: return y</li>
 *   <li>If value > 0: return z</li>
 * </ul>
 */
@Name("select")
@FunctionParameter(name = "value")
@FunctionParameter(name = "x", isLazy = true)
@FunctionParameter(name = "y", isLazy = true)
@FunctionParameter(name = "z", isLazy = true)
public class SelectFunction extends AbstractFunction {

	@Override
	public EvaluationValue evaluate(Expression expression, Token functionToken, EvaluationValue... args) throws EvaluationException {
		return expression.evaluateSubtree(switch (args[0].getNumberValue().compareTo(BigDecimal.ZERO)) {
			case -1 -> args[1].getExpressionNode();
			case 0 -> args[2].getExpressionNode();
			case 1 -> args[3].getExpressionNode();
			default -> throw new IllegalStateException("never");
		});
	}

}
