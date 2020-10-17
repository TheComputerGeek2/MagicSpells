package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.castmodifiers.IModifier;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class PowerCondition extends OperatorCondition implements IModifier {

	private float power;

	@Override
	public boolean initialize(String var) {
		if (var.length() < 2) {
			return false;
		}

		super.initialize(var);

		try {
			power = Float.parseFloat(var.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean apply(SpellCastEvent event) {
		return power(event.getPower());
	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		// No power to check
		return false;
	}

	@Override
	public boolean apply(SpellTargetEvent event) {
		return power(event.getPower());
	}

	@Override
	public boolean apply(SpellTargetLocationEvent event) {
		return power(event.getPower());
	}

	@Override
	public boolean apply(MagicSpellsGenericPlayerEvent event) {
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

	public boolean power(float spellPower) {
		if (equals) return spellPower == power;
		else if (moreThan) return spellPower > power;
		else if (lessThan) return spellPower < power;
		return false;
	}

}
