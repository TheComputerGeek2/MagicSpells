package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.zones.NoMagicZoneManager;

public class InNoMagicZoneCondition extends Condition {

	private String zone;
	
	@Override
	public boolean initialize(String var) {
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
		NoMagicZoneManager man = MagicSpells.getNoMagicZoneManager();
		if (man == null) return false;
		return man.inZone(location, zone);
	}

}
