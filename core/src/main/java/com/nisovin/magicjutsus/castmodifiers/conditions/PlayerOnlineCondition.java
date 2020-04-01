package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class PlayerOnlineCondition extends Condition {
	
	private String name;
	
	@Override
	public boolean setVar(String var) {
		name = var;
		return true;
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return PlayerNameUtils.getPlayerExact(name) != null;
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(livingEntity);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return check(livingEntity);
	}

}
