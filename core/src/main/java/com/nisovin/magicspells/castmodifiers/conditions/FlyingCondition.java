package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class FlyingCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isFlying(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isFlying(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean isFlying(LivingEntity target) {
		return target instanceof Player pl && pl.isFlying();
	}

}
