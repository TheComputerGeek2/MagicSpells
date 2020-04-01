package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.castmodifiers.Condition;
import com.nisovin.magicjutsus.zones.NoMagicZoneManager;

public class InNoMagicZoneCondition extends Condition {

	private String zone;
	
	@Override
	public boolean setVar(String var) {
		if (var == null) return false;
		zone = var;
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
		NoMagicZoneManager man = MagicJutsus.getNoMagicZoneManager();
		if (man == null) return false;
		return man.inZone(location, zone);
	}

}
