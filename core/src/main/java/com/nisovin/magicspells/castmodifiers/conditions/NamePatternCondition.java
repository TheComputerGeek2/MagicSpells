package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.castmodifiers.Condition;

public class NamePatternCondition extends Condition {

	private Pattern compiledPattern;
	
	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		compiledPattern = Pattern.compile(var);
		// note, currently won't translate the & to the color code,
		// this will need to be done through regex unicode format 
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return namePattern(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return namePattern(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean namePattern(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		return RegexUtil.matches(compiledPattern, pl.getName()) || RegexUtil.matches(compiledPattern, Util.getLegacyFromComponent(pl.displayName()));

	}

}
