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
		if (var == null || var.isEmpty()) return false;
		zone = var;
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkZone(caster.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkZone(target.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return checkZone(location);
	}

	private boolean checkZone(Location location) {
		NoMagicZoneManager manager = MagicSpells.getNoMagicZoneManager();
		if (manager == null) return false;
		return manager.inZone(location, zone);
	}

}
