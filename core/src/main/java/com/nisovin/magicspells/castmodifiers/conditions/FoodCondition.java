package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class FoodCondition extends OperatorCondition {

	private int food = 0;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.initialize(var);

		try {
			food = Integer.parseInt(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return food(livingEntity);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return food(target);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean food(LivingEntity livingEntity) {
		if (!(livingEntity instanceof Player)) return false;
		if (equals) return ((Player) livingEntity).getFoodLevel() == food;
		else if (moreThan) return ((Player) livingEntity).getFoodLevel() > food;
		else if (lessThan) return ((Player) livingEntity).getFoodLevel() < food;
		return false;
	}

}
