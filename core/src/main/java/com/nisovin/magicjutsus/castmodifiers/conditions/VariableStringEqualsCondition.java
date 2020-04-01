package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class VariableStringEqualsCondition extends Condition {

	private String variable;
	private String value;

	@Override
	public boolean setVar(String var) {
		String[] split = var.split(":",2);

		//Were two parts of this modifier created?
		if (split.length != 2) return false;

		variable = split[0]; //The variable that is being checked
		value = split[1]; //The value that the variable is being checked for

		//Variable cannot be null or empty.
		if (variable.isEmpty()) {
			MagicJutsus.error("No variable stated for comparison within this modifier!");
			return false;
		}
		//Translates "null" string to "".
		if(value.equals("null")) value = "";

		//If everything checks out, will continue.
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		if (!(target instanceof Player)) return false;
		return MagicJutsus.getVariableManager().getStringValue(variable, (Player) target).equals(value);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return check(livingEntity, livingEntity);
	}

}
