package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class RidingCondition extends Condition {

	private EntityType entityType;
	
	@Override
	public boolean setVar(String var) {
		if (var == null || var.isEmpty()) return true;
		entityType = Util.getEntityType(var);
		return entityType != null;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		Entity vehicle = target.getVehicle();
		if (vehicle == null) return false;
		return entityType == null || vehicle.getType() == entityType;
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
