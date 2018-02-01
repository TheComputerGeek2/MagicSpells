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
		try {
			String s = var;
			int colonposition = var.indexOf(":");
			
			//Checks if there is a colon to begin with.
			if (colonposition == -1) {
				MagicSpells.error("Improper varstring comparison. Use : to compare the string of a variable.");
				throw new Exception("Invalid modifier format!");
			}
			
			if (colonposition == 0) {
				MagicSpells.error("No variable stated in this modifier!");
				throw new Exception("Comparative variable missing!");
			}
			    
			variable = var.substring(0,colonposition);
			value = var.substring(colonposition);
			
			//If there is no value to compare, close immediately.
			if (value.equals("")) {
				MagicSpells.error("You forgot to add a string to compare the variable to in this modifier.");
				throw new Exception("Nothing to compare the variable to!");
			}
			
			//If everything checks out, will continue.
			return true;
		} 
		catch (Exception e) {
			//If any exceptions are thrown, will return false.
			return false;
		}
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
