package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class VariableStringEqualsCondition extends Condition {

	private String variable;
	private String value;
	private boolean isVariable;

	@Override
	public boolean initialize(String var) {
		String[] split = var.split(":",2);

		//Were two parts of this modifier created?
		if (split.length != 2) return false;

		variable = split[0]; //The variable that is being checked
		value = split[1]; //The value that the variable is being checked for

		//Variable cannot be null or empty.
		if (variable.isEmpty()) {
			MagicSpells.error("No variable stated for comparison within this modifier!");
			return false;
		}

		isVariable = MagicSpells.getVariableManager().getVariables().containsKey(value);
		//Translates "null" string to empty.
		if (!isVariable && value.equals("null")) value = "";

		//If everything checks out, will continue.
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isEqualFor(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isEqualFor(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return isEqualFor(caster);
	}

	private boolean isEqualFor(LivingEntity target) {
		if (!(target instanceof Player player)) return false;
		return Objects.equals(
				MagicSpells.getVariableManager().getStringValue(variable, player),
				isVariable ? MagicSpells.getVariableManager().getStringValue(value, player) : value
		);
	}

}
