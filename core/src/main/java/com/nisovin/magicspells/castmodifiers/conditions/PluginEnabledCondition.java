package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.compat.CompatBasics;

public class PluginEnabledCondition extends Condition {

	private String pluginName = null;
	
	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		var = var.trim();
		if (var.isEmpty()) return false;
		pluginName = var;
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkPlugin();
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkPlugin();
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return checkPlugin();
	}
	
	private boolean checkPlugin() {
		if (pluginName == null) return false;
		return CompatBasics.pluginEnabled(pluginName);
	}

}
