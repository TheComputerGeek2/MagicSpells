package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.castmodifiers.Condition;

@Name("displayname")
public class DisplayNameCondition extends Condition {

	private String displayName;

	@Override
	public boolean initialize(@NotNull String var) {
		if (var.isEmpty()) return false;
		displayName = Util.getStringFromComponent(Util.getMiniMessage(var));
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
		if (!(target instanceof Player pl)) return false;
		return Util.getStringFromComponent(pl.displayName()).equals(displayName);
	}

}
