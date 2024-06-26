package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.castmodifiers.Condition;

@Name("name")
public class NameCondition extends Condition {

	private String name;
	
	@Override
	public boolean initialize(@NotNull String var) {
		if (var.isEmpty()) return false;
		name = Util.getPlainString(Util.getMiniMessage(var));
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkName(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkName(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean checkName(LivingEntity target) {
		return Util.getPlainString(target.name()).equals(name);
	}

}
