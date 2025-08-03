package com.nisovin.magicspells.util.config.expression;

import java.util.*;
import java.util.Map.Entry;

import com.ezylang.evalex.operators.OperatorIfc;
import com.ezylang.evalex.functions.FunctionIfc;
import com.ezylang.evalex.functions.trigonometric.*;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.operators.AbstractOperator;
import com.ezylang.evalex.config.ExpressionConfiguration;
import com.ezylang.evalex.functions.basic.CeilingFunction;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.expression.functions.*;

public class ExpressionDictionary {

	private static final double TAU = 2 * AccurateMath.PI;
	private static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;

	private final Map<String, FunctionIfc> FUNCTIONS = new HashMap<>();
	private final Map<String, OperatorIfc> OPERATORS = new HashMap<>();
	private final Map<String, Object> CONSTANTS = new HashMap<>();

	public ExpressionDictionary() {
		initialize();
	}

	/**
	 * @param functionClass must be annotated with {@link Name}.
	 */
	public void addFunction(Class<? extends AbstractFunction> functionClass) {
		Name name = functionClass.getAnnotation(Name.class);
		if (name == null) throw new IllegalStateException("Missing 'Name' annotation from AbstractFunction class: " + functionClass.getName());

		try {
			AbstractFunction function = functionClass.getDeclaredConstructor().newInstance();
			FUNCTIONS.put(name.value(), function);
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
		}
	}

	/**
	 * @param opClass must be annotated with {@link Name}.
	 */
	public void addOperator(Class<? extends AbstractOperator> opClass) {
		Name name = opClass.getAnnotation(Name.class);
		if (name == null) throw new IllegalStateException("Missing 'Name' annotation from AbstractOperator class: " + opClass.getName());

		try {
			AbstractOperator op = opClass.getDeclaredConstructor().newInstance();
			OPERATORS.put(name.value(), op);
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
		}
	}

	public void addConstant(String name, Object value) {
		CONSTANTS.put(name, value);
	}

	@SuppressWarnings("unchecked")
	private static <K, V> Entry<K, V>[] getEntries(Map<K, V> map) {
		return map.entrySet().toArray(new Entry[0]);
	}

	public Entry<String, FunctionIfc>[] getFunctions() {
		return getEntries(FUNCTIONS);
	}

	public Entry<String, OperatorIfc>[] getOperators() {
		return getEntries(OPERATORS);
	}

	public Map<String, Object> getConstants() {
		return Collections.unmodifiableMap(CONSTANTS);
	}

	public ExpressionConfiguration getExpressionConfiguration() {
		return ExpressionConfiguration.builder()
			.powerOfPrecedence(OperatorIfc.OPERATOR_PRECEDENCE_POWER_HIGHER) // exp4j backward compat
			.build()
			.withAdditionalFunctions(
				// overrides for exp4j backward compat
				Map.entry("acos", new AcosRFunction()),
				Map.entry("acot", new AcotRFunction()),
				Map.entry("asin", new AsinRFunction()),
				Map.entry("atan2", new Atan2RFunction()),
				Map.entry("atan", new AtanRFunction()),
				Map.entry("cos", new CosRFunction()),
				Map.entry("cot", new CotRFunction()),
				Map.entry("csc", new CscRFunction()),
				Map.entry("sec", new SecRFunction()),
				Map.entry("sin", new SinRFunction()),
				Map.entry("tan", new TanRFunction()),
				// exp4j backward compat
				Map.entry("ceil", new CeilingFunction()),
				Map.entry("todegree", new DegFunction()),
				Map.entry("toradian", new RadFunction())
			)
			.withAdditionalFunctions(getFunctions())
			.withAdditionalOperators(getOperators());
	}

	private void initialize() {
		// exp4j backward compat
		addFunction(CbrtFunction.class);
		addFunction(ExpFunction.class);
		addFunction(Expm1Function.class);
		addFunction(Log1pFunction.class);
		addFunction(Log2Function.class);
		addFunction(LogbFunction.class);
		addFunction(PowFunction.class);
		addFunction(ProbFunction.class);
		addFunction(RandFunction.class);
		addFunction(SelectFunction.class);
		addFunction(SignumFunction.class);

		addConstant("φ", GOLDEN_RATIO);
		addConstant("π", AccurateMath.PI);

		// extra
		addConstant("tau", TAU);
		addConstant("phi", GOLDEN_RATIO);
	}

}
