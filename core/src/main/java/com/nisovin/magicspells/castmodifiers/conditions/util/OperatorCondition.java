package com.nisovin.magicspells.castmodifiers.conditions.util;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OperatorCondition extends Condition {

	public boolean equals;
	public boolean moreThan;
	public boolean lessThan;

	@Override
	public boolean initialize(String var) {
		switch (var.charAt(0)) {
			case '=':
			case ':':
				equals = true;
				break;
			case '>':
				moreThan = true;
				break;
			case '<':
				lessThan = true;
				break;
			default:
				return false;
		}

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
