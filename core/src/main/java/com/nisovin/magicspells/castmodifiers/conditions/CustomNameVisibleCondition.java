package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.castmodifiers.Condition;

@Name("customnamevisible")
public class CustomNameVisibleCondition extends Condition {

	@Override
	public boolean initialize(@NotNull String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return nameVisible(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return nameVisible(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean nameVisible(LivingEntity target) {
		return target.isCustomNameVisible();
	}

}
