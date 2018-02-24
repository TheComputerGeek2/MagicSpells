package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class VariableStringEqualsCondition extends Condition {

	String variable;
	String value;

	@Override
	public boolean setVar(String var) {
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
		//Value cannot be null or empty.
		if (value.isEmpty()) {
			MagicSpells.error("This variable is being compared to nothing!");
			return false;
		}

		//If everything checks out, will continue.
		return true;
	}

	@Override
	public boolean check(Player player) {
		return MagicSpells.getVariableManager().getStringValue(variable, player).equals(value);
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		if (target instanceof Player) return check((Player)target);
		return check(player);
	}

	@Override
	public boolean check(Player player, Location location) {
		return check(player);
	}

}
