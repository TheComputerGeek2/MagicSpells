package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class ChanceCondition extends Condition {

	private double chance;

	@Override
	public boolean initialize(String var) {
		try {
			chance = Double.parseDouble(var) / 100D;
			return chance >= 0D && chance <= 1D;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return chance();
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return chance();
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return chance();
	}

	private boolean chance() {
		return chance != 0 && (chance == 1 || ThreadLocalRandom.current().nextDouble() < chance);
	}

}
