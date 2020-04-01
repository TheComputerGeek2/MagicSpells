package com.nisovin.magicjutsus.castmodifiers.conditions;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class ChanceCondition extends Condition {

	private int chance;
	private Random random;
	
	@Override
	public boolean setVar(String var) {
		random = new Random();
		try {
			chance = Integer.parseInt(var);
			if (chance < 1 || chance > 100) return false;
			return true;
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return random.nextInt(100) < chance;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(target);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return check(livingEntity);
	}

}
