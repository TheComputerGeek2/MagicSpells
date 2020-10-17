package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class SaturationCondition extends OperatorCondition {

	private float saturation;
	
	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.initialize(var);

		try {
			saturation = Float.parseFloat(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return saturation(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return saturation(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean saturation(LivingEntity livingEntity) {
		if (!(livingEntity instanceof Player)) return false;
		if (equals) return ((Player) livingEntity).getSaturation() == saturation;
		else if (moreThan) return ((Player) livingEntity).getSaturation() > saturation;
		else if (lessThan) return ((Player) livingEntity).getSaturation() < saturation;
		return false;
	}

}
