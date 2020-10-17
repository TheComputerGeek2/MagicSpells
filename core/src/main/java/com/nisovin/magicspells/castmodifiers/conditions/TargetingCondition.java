package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.castmodifiers.Condition;

public class TargetingCondition extends Condition {

	private Set<EntityType> allowedTypes;
	private boolean anyType = false;
	private boolean targetingCaster = false;
	
	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) {
			anyType = true;
			return true;
		}
		if (var.equalsIgnoreCase("caster")) {
			targetingCaster = true;
			return true;
		}
		
		String[] entityTypes = var.split(",");
		allowedTypes = new HashSet<>();
		for (String type: entityTypes) {
			EntityType entityType = Util.getEntityType(type);
			if (entityType != null) allowedTypes.add(entityType);
		}
		return !allowedTypes.isEmpty();
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		if (!(target instanceof Creature)) return false;
		LivingEntity creatureTarget = ((Creature) target).getTarget();
		if (creatureTarget != null) {
			if (anyType) return true;
			if (targetingCaster && creatureTarget.equals(livingEntity)) return true;
			if (allowedTypes.contains(creatureTarget.getType())) return true;
		}
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
