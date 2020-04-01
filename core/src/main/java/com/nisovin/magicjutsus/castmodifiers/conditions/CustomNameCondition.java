package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.castmodifiers.Condition;

public class CustomNameCondition extends Condition {

	private String name;
	private boolean isVar;

	@Override
	public boolean setVar(String var) {
		if (var == null || var.isEmpty()) return false;
		name = ChatColor.translateAlternateColorCodes('&', var);
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
		if (isVar) checkedName = MagicJutsus.doVariableReplacements((Player) livingEntity, checkedName);

		String targetName = target.getCustomName();
		if (!isVar) checkedName = checkedName.replace("__", " ");
		return targetName != null && !targetName.isEmpty() && checkedName.equalsIgnoreCase(targetName);
	}

}
