package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class VarStringEqualsCondition extends Condition {

	String variable;
	String value;
	
	@Override
	public boolean setVar(String var) {
		String[] string = var.split(":",2);
		variable = string[0]; //The variable that is being checked
		value = string[1]; //The string that the variable is being checked for

		//Is there a variable to be checked? Required!
		if (vartocheck.equals("")) {
			MagicSpells.error("No variable stated for comparison within this modifier!");
			return false;
		}
		//Is there something to compare the variable to? " " should still work.
		if (strcompare.equals("")) {
			MagicSpells.error("The stated variable in this modifier isn't being compared to anything!");
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
