package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import com.nisovin.magicspells.util.Util;

import com.nisovin.magicspells.castmodifiers.Condition;

public class CustomNameCondition extends Condition {

	private String name;
	private boolean isVar;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		name = Util.colorize(var);
		if (name.contains("%var:") || name.contains("%playervar")) isVar = true;
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return checkName(livingEntity, target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean checkName(LivingEntity livingEntity, LivingEntity target) {
		if (!(livingEntity instanceof Player)) return false;
		String checkedName = name;
		if (isVar) checkedName = Util.doVarReplacementAndColorize((Player) livingEntity, checkedName);
		else checkedName = checkedName.replace("__", " ");

		String targetName = target.getCustomName();
		return targetName != null && !targetName.isEmpty() && checkedName.equalsIgnoreCase(targetName);
	}

}
