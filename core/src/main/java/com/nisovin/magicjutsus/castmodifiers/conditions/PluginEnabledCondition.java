package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.castmodifiers.Condition;
import com.nisovin.magicjutsus.util.compat.CompatBasics;

public class PluginEnabledCondition extends Condition {

	private String pluginName = null;
	
	@Override
	public boolean setVar(String var) {
		if (var == null) return false;
		var = var.trim();
		if (var.isEmpty()) return false;
		this.pluginName = var;
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check();
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check();
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return check();
	}
	
	private boolean check() {
		if (pluginName == null) return false;
		return CompatBasics.pluginEnabled(pluginName);
	}

}
