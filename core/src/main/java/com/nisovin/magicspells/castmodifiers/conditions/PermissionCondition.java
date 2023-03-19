package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class PermissionCondition extends Condition {

	private String perm;

	@Override
	public boolean initialize(String var) {
		perm = var;
		return true;
	}
	
	@Override
	public boolean check(LivingEntity caster) {
		return hasPermission(caster);
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return hasPermission(target);
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean hasPermission(LivingEntity target) {
		return target.hasPermission(perm);
	}

}
