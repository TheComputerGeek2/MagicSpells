package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class IsNPCCondition extends Condition {

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isNPC(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isNPC(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean isNPC(LivingEntity target) {
		return target.hasMetadata("NPC");
	}

}
