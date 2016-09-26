package com.nisovin.magicspells.util;

import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;

public class VariableMod {
	
	
	public static enum VariableOwner {
		CASTER, TARGET
	}
	
	public static enum Operation {
		SET, ADD, MULTIPLY, DIVIDE;
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
			default:
				return ADD;
			}
		}
	}
	
	private VariableOwner variableOwner = null;
	private String modifyingVariableName = null;
	private Operation op = null;
	private double constantModifier;
	private static final Pattern operationMatcher = Pattern.compile("^(=|\\+|\\*|\\/)");
	
	private boolean negate = false;
	
	public VariableMod(String data) {
		op = Operation.fromPrefix(data);
		data = operationMatcher.matcher(data).replaceFirst("");
		if (data.startsWith("-")) {
			data = data.substring(1);
			negate = true;
		}
		if (!RegexUtil.matches(RegexUtil.DOUBLE_PATTERN, data)) {
			//if it isn't a double, then let's match it as a variable
			if (data.contains(":")) {
				//then there is an explicit statement of who's variable it is
				String[] dataSplits = data.split(":");
				if (dataSplits[0].toLowerCase().equals("target")) {
					variableOwner = VariableOwner.TARGET;
				} else {
					variableOwner = VariableOwner.CASTER;
				}
				
			}
		} else {
			constantModifier = Double.parseDouble(data);
		}
		
	}
	
	public double getValue(Player caster, Player target) {
		if (modifyingVariableName != null) {
			if (variableOwner == VariableOwner.CASTER ) {
				return MagicSpells.getVariableManager().getValue(modifyingVariableName, caster)* (negate ? -1: 1);
			} else { //variable owner == target
				return MagicSpells.getVariableManager().getValue(modifyingVariableName, target)* (negate ? -1: 1);
			}	
		} else {
			return constantModifier * (negate ? -1: 1);
		}
	}
	
	public boolean isConstantValue() {
		return modifyingVariableName == null;
	}
	
	public Operation getOperation() {
		return op;
	}
	
}
