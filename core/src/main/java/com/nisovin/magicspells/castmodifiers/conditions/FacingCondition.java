package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class FacingCondition extends Condition {

	private String direction;
	
	@Override
	public boolean initialize(String var) {
		direction = var;
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return getDirection(livingEntity.getLocation()).equals(direction);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return getDirection(target.getLocation()).equals(direction);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return getDirection(location).equals(direction);
	}
	
	private String getDirection(Location loc) {
        float y = loc.getYaw();
        if (y < 0) y += 360;
        y %= 360;
        if (y <= 45 || y >= 315) return "south";
        if (y >= 45 && y <= 135) return "west";
        if (y >= 135 && y <= 225) return "north";
		return "east";
   }

}
