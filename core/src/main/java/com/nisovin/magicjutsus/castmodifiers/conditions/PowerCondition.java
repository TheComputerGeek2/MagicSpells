package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.events.ManaChangeEvent;
import com.nisovin.magicjutsus.castmodifiers.IModifier;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;
import com.nisovin.magicjutsus.events.MagicJutsusGenericPlayerEvent;

public class PowerCondition extends OperatorCondition implements IModifier {

	private float power;

	@Override
	public boolean setVar(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.setVar(var);

		try {
			power = Float.parseFloat(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean apply(JutsuCastEvent event) {
		return power(event.getPower());
	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		// No power to check
		return false;
	}

	@Override
	public boolean apply(JutsuTargetEvent event) {
		return power(event.getPower());
	}

	@Override
	public boolean apply(JutsuTargetLocationEvent event) {
		return power(event.getPower());
	}

	@Override
	public boolean apply(MagicJutsusGenericPlayerEvent event) {
		// No power to check
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	public boolean power(float jutsuPower) {
		if (equals) return jutsuPower == power;
		else if (moreThan) return jutsuPower > power;
		else if (lessThan) return jutsuPower < power;
		return false;
	}

}
