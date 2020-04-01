package com.nisovin.magicjutsus.castmodifiers.conditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;

public class VariableCondition extends OperatorCondition {

	private static Pattern variableNamePattern = Pattern.compile("[0-9a-zA-Z_]+");

	private String variable;
	private double value = 0;

	@Override
	public boolean setVar(String var) {
		Matcher matcher = variableNamePattern.matcher(var);
		String variableName = null;
		String number;

		while (matcher.find()) {
			String argText = matcher.group();
			variableName = argText;
			break;
		}

		if (variableName == null) return false;

		number = var.substring(variableName.length()).substring(1);

		if (number == null) return false;

		super.setVar(var.substring(variableName.length()));

		try {
			variable = variableName;
			value = Double.parseDouble(number);
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return variable(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return variable(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return variable(livingEntity);
	}

	private boolean variable(LivingEntity livingEntity) {
		if (!(livingEntity instanceof Player)) return false;
		if (equals) return MagicJutsus.getVariableManager().getValue(variable, (Player) livingEntity) == value;
		else if (moreThan) return MagicJutsus.getVariableManager().getValue(variable, (Player) livingEntity) > value;
		else if (lessThan) return MagicJutsus.getVariableManager().getValue(variable, (Player) livingEntity) < value;
		return false;
	}

}
