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
			String[] s = var.split(":");
			variable = s[0];
			value = s[1];
			return true;
		} catch (ArrayIndexOutOfBoundsException missingColon) {
			MagicSpells.error("You likely forgot to add a colon in this modifier var.");
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		return MagicSpells.getVariableManager().getStringValue(variable, player) == value;
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
