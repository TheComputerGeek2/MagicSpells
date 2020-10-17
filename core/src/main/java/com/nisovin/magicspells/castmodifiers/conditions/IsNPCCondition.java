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
	public boolean check(LivingEntity livingEntity) {
		return livingEntity.hasMetadata("NPC");
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return target.hasMetadata("NPC");
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
