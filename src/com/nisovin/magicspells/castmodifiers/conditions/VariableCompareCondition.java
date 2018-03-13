package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class VariableCompareCondition extends Condition {

	public String variable;
	int op = 0;
	String firstVariable;
	String secondVariable;

	@Override
	public boolean setVar(String var) {
		this.variable = var;
		if(var.contains(":")) {
			// Find out if its comparing dual values
			String[] split = var.split(":",2);
			firstVariable = split[0]; //The variable that is being checked
			secondVariable = split[1]; //The string that the variable is being checked against
			op = 1;
			return true;
		}
		if(var.contains("<")) {
			// Find out if its a more than equation
			String[] split = var.split("<",2);
			firstVariable = split[0];
			secondVariable = split[1];
			op = 2;
			return true;
		}
		if(var.contains(">")) {
			// Find out if its a less than equation
			String[] split = var.split(">",2);
			firstVariable = split[0];
			secondVariable = split[1];
			op = 3;
			return true;
		}

		// Someone didn't read the GitHub commit
		MagicSpells.error("You must use either <, >, or : to split the variables");
		return false;
	}

	@Override
	public boolean check(Player player) {
		// Get variable values
		String value = MagicSpells.getVariableManager().getStringValue(firstVariable, player);
		String valueSecond = MagicSpells.getVariableManager().getStringValue(secondVariable, player);
		double valueDouble = 0;
		double valueDoubleSecond = 0;

		// Will it require the string to be a double?
		if(op == 2 || op == 3) {
			// Parse the string into a double so it can be read correctly
			valueDouble = Double.parseDouble(value);
			valueDoubleSecond = Double.parseDouble(valueSecond);
		}else if(op == 1) {
			return (value.equals(valueSecond));
		}

		// Do the actual comparison
		if(op == 2) {
			return Double.compare(valueDouble,valueDoubleSecond) < 0;
		}
		if(op == 3) {
			return Double.compare(valueDouble,valueDoubleSecond) > 0;
		}
		throw new IllegalStateException(op + " should never be reached!");
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		// Someone didn't read the GitHub commit x2
		MagicSpells.error("VariableCompare cannot be used in target-modifiers, use VariableMatches");
		return false;
	}

	@Override
	public boolean check(Player player, Location location) {
		// Against defaults (only possible comparison here)
		return check(player);
	}
}
