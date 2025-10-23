package com.nisovin.magicspells.castmodifiers.conditions;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.InputPredicate;
import com.nisovin.magicspells.castmodifiers.Condition;

@Name("input")
public class InputCondition extends Condition {

	private InputPredicate predicate;

	@Override
	public boolean initialize(@NotNull String var) {
		predicate = InputPredicate.fromString(var);
		return predicate != null;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return caster instanceof Player player && predicate.test(player.getCurrentInput());
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return check(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

}
