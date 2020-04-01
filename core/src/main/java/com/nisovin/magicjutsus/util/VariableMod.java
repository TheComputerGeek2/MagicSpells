package com.nisovin.magicjutsus.util;

import java.util.regex.Pattern;
import java.util.function.BinaryOperator;

import org.bukkit.entity.Player;

import com.nisovin.magicjutsus.MagicJutsus;

import org.apache.commons.math3.util.FastMath;

public class VariableMod {
	
	public enum VariableOwner {
		
		CASTER,
		TARGET
		
	}
	
	public enum Operation {
		
		SET((a, b) -> b),
		ADD((a, b) -> a + b),
		MULTIPLY((a, b) -> a * b),
		DIVIDE((a, b) -> a / b),
		POWER((a, b) -> FastMath.pow(a, b));

		private final BinaryOperator<Double> operator;
		
		Operation(BinaryOperator<Double> operator) {
			this.operator = operator;
		}
		
		public double applyTo(double arg1, double arg2) {
			return operator.apply(arg1, arg2);
		}
		
		static Operation fromPrefix(String s) {
			char c = s.charAt(0);
			switch (c) {
				case '=':
					return SET;
				case '+':
					return ADD;
				case '*':
					return MULTIPLY;
				case '/':
					return DIVIDE;
				case '^':
					return POWER;
				default:
					return ADD;
			}
		}
		
	}
	
	private VariableOwner variableOwner = VariableOwner.CASTER;
	private String modifyingVariableName = null;
	private String value;
	private Operation op;
	private double constantModifier;
	private static final Pattern operationMatcher = Pattern.compile("^(=|\\+|\\*|\\/|^)");
	
	private boolean negate = false;
	
	public VariableMod(String data) {
		op = Operation.fromPrefix(data);
		data = operationMatcher.matcher(data).replaceFirst("");
		if (data.startsWith("-")) {
			data = data.substring(1);
			negate = true;
		}

		value = data;

		if (RegexUtil.matches(RegexUtil.DOUBLE_PATTERN, data)) constantModifier = Double.parseDouble(data);
		else {
			// If it isn't a double, then let's match it as a variable
			String varName = data;
			if (data.contains(":")) {
				// Then there is an explicit statement of who's variable it is
				String[] dataSplits = data.split(":");
				if (dataSplits[0].toLowerCase().equals("target")) variableOwner = VariableOwner.TARGET;
				varName = dataSplits[1];
			}
			modifyingVariableName = varName;
		}
	}
	
	public double getValue(Player caster, Player target) {
		int negationFactor = getNegationFactor();
		if (modifyingVariableName != null) {
			Player variableHolder = variableOwner == VariableOwner.CASTER ? caster : target;
			return MagicJutsus.getVariableManager().getValue(modifyingVariableName, variableHolder) * negationFactor;
		}
		return constantModifier * negationFactor;
	}
	
	public double getValue(Player caster, Player target, double baseValue) {
		double secondValue = getValue(caster, target);
		return getOperation().applyTo(baseValue, secondValue);
	}

	public String getValue() {
		return value;
	}
	
	public boolean isConstantValue() {
		return modifyingVariableName == null;
	}
	
	public Operation getOperation() {
		return op;
	}
	
	public VariableOwner getVariableOwner() {
		return variableOwner;
	}
	
	private int getNegationFactor() {
		return negate ? -1 : 1;
	}
	
}
