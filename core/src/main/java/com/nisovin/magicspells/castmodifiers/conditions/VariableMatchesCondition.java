package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class VariableMatchesCondition extends Condition {

	private String variable;
	
	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		variable = var;
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		if (!(livingEntity instanceof Player)) return false;
		// Check against normal (default)
		return Objects.equals(
			MagicSpells.getVariableManager().getStringValue(variable, (Player) livingEntity),
			MagicSpells.getVariableManager().getStringValue(variable, (String) null)
		);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		if (!(livingEntity instanceof Player)) return false;
		if (!(target instanceof Player)) return false;
		// Check against each other
		return Objects.equals(
			MagicSpells.getVariableManager().getStringValue(variable, (Player) livingEntity),
			MagicSpells.getVariableManager().getStringValue(variable, target.getName())
		);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		// Against defaults (only possible comparison here)
		return check(livingEntity);
	}

}
