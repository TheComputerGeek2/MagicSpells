package com.nisovin.magicspells.util;

import java.util.function.BinaryOperator;
import java.util.regex.Pattern;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;

public class VariableMod {
	
	public enum VariableOwner {
		
		CASTER,
		TARGET
		
	}
	
	public enum Operation {
		
		SET((a, b) -> b),
		ADD((a, b) -> a + b),
		MULTIPLY((a, b) -> a * b),
		DIVIDE((a, b) -> a/b),
		POWER((a, b) -> FastMath.pow(a, b))
		
		;
		
		private final BinaryOperator<Double> operator;
		
		Operation(BinaryOperator<Double> operator) {
			this.operator = operator;
		}
		
		public double applyTo(double arg1, double arg2) {
			return this.operator.apply(arg1, arg2);
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
	private Operation op = null;
	private double constantModifier;
	private static final Pattern operationMatcher = Pattern.compile("^(=|\\+|\\*|\\/|^)");
	
	private boolean negate = false;
	
	public VariableMod(String data) {
		this.op = Operation.fromPrefix(data);
		data = operationMatcher.matcher(data).replaceFirst("");
		if (data.startsWith("-")) {
			data = data.substring(1);
			this.negate = true;
		}
		if (!RegexUtil.matches(RegexUtil.DOUBLE_PATTERN, data)) {
			// If it isn't a double, then let's match it as a variable
			String varName = data;
			if (data.contains(":")) {
				// Then there is an explicit statement of who's variable it is
				String[] dataSplits = data.split(":");
				if (dataSplits[0].toLowerCase().equals("target")) {
					this.variableOwner = VariableOwner.TARGET;
				} else {
					this.variableOwner = VariableOwner.CASTER;
				}
				varName = dataSplits[1];
			}
			this.modifyingVariableName = varName;
		} else {
			this.constantModifier = Double.parseDouble(data);
		}
		
	}
	
	public double getValue(Player caster, Player target) {
		int negationFactor = this.getNegationFactor();
		if (this.modifyingVariableName != null) {
			Player variableHolder = this.variableOwner == VariableOwner.CASTER ? caster : target;
			return MagicSpells.getVariableManager().getValue(this.modifyingVariableName, variableHolder) * negationFactor;
		}
		return this.constantModifier * negationFactor;
	}
	
	public double getValue(Player caster, Player target, double baseValue) {
		double secondValue = this.getValue(caster, target);
		return this.getOperation().applyTo(baseValue, secondValue);
	}
	
	public boolean isConstantValue() {
		return this.modifyingVariableName == null;
	}
	
	public Operation getOperation() {
		return this.op;
	}
	
	public VariableOwner getVariableOwner() {
		return this.variableOwner;
	}
	
	private int getNegationFactor() {
		return this.negate ? -1 : 1;
	}
	
}
