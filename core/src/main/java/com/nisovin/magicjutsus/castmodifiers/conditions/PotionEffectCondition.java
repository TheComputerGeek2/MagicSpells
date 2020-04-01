package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class PotionEffectCondition extends Condition {

	private PotionEffectType effectType;
	private int value;
	
	@Override
	public boolean setVar(String var) {
		if (var.contains(":")) {
			String[] s = var.split(":");
			effectType = Util.getPotionEffectType(s[0]);
			try {
				value = Integer.parseInt(s[1]);
			} catch (Exception e) {
				return false;
			}
		} else {
			effectType = Util.getPotionEffectType(var);
			value = -1;
		}
		return effectType != null;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		boolean has = target.hasPotionEffect(effectType);
		if (value == -1) return has;
		if (has) {
			for (PotionEffect effect : target.getActivePotionEffects()) {
				if (effect.getType() == effectType) return effect.getAmplifier() == value;
			}
		}
		return false;
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}	
	
}
