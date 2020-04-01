package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.castmodifiers.Condition;

public class MoonPhaseCondition extends Condition {

	private String phaseName = "";

	@Override
	public boolean setVar(String var) {
		phaseName = var.toLowerCase();
		return true;
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(target, target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		long time = location.getWorld().getFullTime();
		int phase = (int) ((time / 24000) % 8);
		// Check if the moon is "Full" or "New"
		if (phase == 0 && phaseName.equals("full")) return true;
		if (phase == 4 && phaseName.equals("new")) return true;
		// Include backwards compatability for servers that use the vague phases
		if ((phase == 1 || phase == 2 || phase == 3) && phaseName.equals("waning")) return true;
		if ((phase == 5 || phase == 6 || phase == 7) && phaseName.equals("waxing")) return true;
		// Specific phases https://minecraft.gamepedia.com/Moon#Phases
		if ((phase == 1) && phaseName.equals("waning_gibbous")) return true;
		if ((phase == 2) && phaseName.equals("last_quarter")) return true;
		if ((phase == 3) && phaseName.equals("waning_crescent")) return true;
		if ((phase == 5) && phaseName.equals("waxing_crescent")) return true;
		if ((phase == 6) && phaseName.equals("first_quarter")) return true;
		if ((phase == 7) && phaseName.equals("waxing_gibbous")) return true;
		return false;
	}

}
