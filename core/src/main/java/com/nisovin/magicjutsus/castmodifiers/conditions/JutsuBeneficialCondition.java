package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.events.ManaChangeEvent;
import com.nisovin.magicjutsus.castmodifiers.Condition;
import com.nisovin.magicjutsus.castmodifiers.IModifier;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;
import com.nisovin.magicjutsus.events.MagicJutsusGenericPlayerEvent;

public class JutsuBeneficialCondition extends Condition implements IModifier {

	@Override
	public boolean apply(JutsuCastEvent event) {
		return checkJutsu(event.getJutsu());
	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		return false;
	}

	@Override
	public boolean apply(JutsuTargetEvent event) {
		return checkJutsu(event.getJutsu());
	}

	@Override
	public boolean apply(JutsuTargetLocationEvent event) {
		return checkJutsu(event.getJutsu());
	}

	@Override
	public boolean apply(MagicJutsusGenericPlayerEvent event) {
		return false;
	}
	
	private boolean checkJutsu(Jutsu jutsu) {
		if (jutsu == null) return false;
		return jutsu.isBeneficial();
	}

	@Override
	public boolean setVar(String var) {
		return true;
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
