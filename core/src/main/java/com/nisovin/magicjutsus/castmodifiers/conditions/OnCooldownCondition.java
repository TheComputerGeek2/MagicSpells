package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class OnCooldownCondition extends Condition {

	private Jutsu jutsu;
	
	@Override
	public boolean setVar(String var) {
		jutsu = MagicJutsus.getJutsuByInternalName(var);
		return jutsu != null;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return jutsu.onCooldown(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return jutsu.onCooldown(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
