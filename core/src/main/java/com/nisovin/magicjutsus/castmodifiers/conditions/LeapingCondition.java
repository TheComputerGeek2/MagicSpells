package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.castmodifiers.Condition;
import com.nisovin.magicjutsus.jutsus.instant.LeapJutsu;

public class LeapingCondition extends Condition {

	private LeapJutsu leapJutsu;
	
	@Override
	public boolean setVar(String var) {
		Jutsu jutsu = MagicJutsus.getJutsuByInternalName(var);
		if (!(jutsu instanceof LeapJutsu)) return false;
		leapJutsu = (LeapJutsu) jutsu;
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return leapJutsu.isJumping(livingEntity);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return leapJutsu.isJumping(target);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
