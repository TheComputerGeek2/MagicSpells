package com.nisovin.magicspells.castmodifiers.conditions;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Objects;

public class VariableMatchesCondition extends Condition {

	String variable;
	
	@Override
	public boolean setVar(String var) {
		if (var == null || var.isEmpty()) return false;
		variable = var;
		return true;
	}

	@Override
	public boolean check(Player player) {
		// Check against normal (default)
		return Objects.equals(
			MagicSpells.getVariableManager().getStringValue(variable, player),
			MagicSpells.getVariableManager().getStringValue(variable, (String)null)
		);
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		// Check against each other
		return Objects.equals(
			MagicSpells.getVariableManager().getStringValue(variable, player),
			MagicSpells.getVariableManager().getStringValue(variable, target instanceof Player ? target.getName() : null)
		);
	}

	@Override
	public boolean check(Player player, Location location) {
		// Against defaults (only possible comparison here)
		return check(player);
	}

}
