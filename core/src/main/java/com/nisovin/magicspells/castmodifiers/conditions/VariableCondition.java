package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class VariableCondition extends OperatorCondition {

	private static Pattern VARIABLE_NAME_PATTERN = Pattern.compile("[0-9a-zA-Z_]+");

	private String variable;
	private String variableCompared;
	private double value = 0;

	@Override
	public boolean initialize(String var) {
		Matcher matcher = VARIABLE_NAME_PATTERN.matcher(var);
		String variableName;

		if (matcher.find()) variableName = matcher.group();
		else return false;

		String number = var.substring(variableName.length());
		super.initialize(number);

		variable = variableName;

		try {
			value = Double.parseDouble(number.substring(1));
			return true;
		} catch (NumberFormatException e) {

			variableCompared = number.substring(1);
			if (MagicSpells.getVariableManager().getVariable(variableCompared) != null) return true;
			else variableCompared = null;

			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return variableType(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return variableType(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return variableType(livingEntity);
	}

	private boolean variable(Player player, double v) {
		if (equals) return MagicSpells.getVariableManager().getValue(variable, player) == v;
		else if (moreThan) return MagicSpells.getVariableManager().getValue(variable, player) > v;
		else if (lessThan) return MagicSpells.getVariableManager().getValue(variable, player) < v;
		return false;
	}

	private boolean variableType(LivingEntity livingEntity) {
		if (!(livingEntity instanceof Player)) return false;
		if (variableCompared != null) return variable((Player) livingEntity, MagicSpells.getVariableManager().getValue(variableCompared, (Player) livingEntity));
		return variable((Player) livingEntity, value);
	}

}
