package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class LastDamageTypeCondition extends Condition {

	private DamageCause cause;

	@Override
	public boolean initialize(String var) {
		for (DamageCause dc : DamageCause.values()) {
			if (dc.name().equalsIgnoreCase(var)) {
				cause = dc;
				return true;
			}
		}
		DebugHandler.debugBadEnumValue(DamageCause.class, var);
		return false;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkDamage(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkDamage(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean checkDamage(LivingEntity target) {
		EntityDamageEvent event = target.getLastDamageCause();
		return event != null && event.getCause() == cause;
	}

}
