package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.castmodifiers.Condition;

public class PermissionCondition extends Condition {

	private String perm;

	@Override
	public boolean setVar(String var) {
		perm = var;
		return true;
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return livingEntity.hasPermission(perm);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return target.hasPermission(perm);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
