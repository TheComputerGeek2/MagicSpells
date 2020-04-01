package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsu.JutsuCastState;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.events.ManaChangeEvent;
import com.nisovin.magicjutsus.castmodifiers.Condition;
import com.nisovin.magicjutsus.castmodifiers.IModifier;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;
import com.nisovin.magicjutsus.events.MagicJutsusGenericPlayerEvent;

/**
 * Valid condition variable arguments are any of the following:
 * NORMAL
 * ON_COOLDOWN
 * MISSING_REAGENTS
 * CANT_CAST
 * NO_MAGIC_ZONE
 * WRONG_WORLD
 */
public class JutsuCastStateCondition extends Condition implements IModifier {

	private JutsuCastState state;

	@Override
	public boolean setVar(String var) {
		if (var == null) return false;
		try {
			state = JutsuCastState.valueOf(var.trim().toUpperCase());
			return true;
		} catch (IllegalArgumentException badValueString) {
			MagicJutsus.error("Invalid JutsuCastState of \"" + var.trim() + "\" on this modifier var");
			return false;
		}
	}

	@Override
	public boolean apply(JutsuCastEvent event) {
		return event.getJutsuCastState() == state;
	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		return false;
	}

	@Override
	public boolean apply(JutsuTargetEvent event) {
		return false;
	}

	@Override
	public boolean apply(JutsuTargetLocationEvent event) {
		return false;
	}

	@Override
	public boolean apply(MagicJutsusGenericPlayerEvent event) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
