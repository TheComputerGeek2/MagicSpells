package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class BuffActiveCondition extends Condition {

	private BuffJutsu buff;

	@Override
	public boolean setVar(String var) {
		Jutsu jutsu = MagicJutsus.getJutsuByInternalName(var);
		if (jutsu instanceof BuffJutsu) {
			buff = (BuffJutsu) jutsu;
			return true;
		}
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return buff.isActiveAndNotExpired(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
