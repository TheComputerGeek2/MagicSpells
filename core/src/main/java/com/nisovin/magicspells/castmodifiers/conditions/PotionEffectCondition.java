package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class PotionEffectCondition extends OperatorCondition {

	private PotionEffectType effectType;
	private int value;
	private boolean matchOnlyType;

	@Override
	public boolean initialize(String var) {
		String[] splits = var.split(":");
		if (splits.length > 1) {
			if (splits[1].length() < 2 || !super.initialize(splits[1])) return false;
			try {
				value = Integer.parseInt(splits[1].substring(1));
			} catch (NumberFormatException ignored) {
				return false;
			}
		} else matchOnlyType = true;

		effectType = Util.getPotionEffectType(splits[0]);
		return effectType != null;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkPotionEffects(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkPotionEffects(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean checkPotionEffects(LivingEntity target) {
		for (PotionEffect effect : target.getActivePotionEffects()) {
			if (effect.getType() != effectType) continue;
			if (matchOnlyType) return true;

			int amplifier = effect.getAmplifier();
			if (equals) return amplifier == value;
			else if (moreThan) return amplifier > value;
			else if (lessThan) return amplifier < value;
		}
		return false;
	}

}
