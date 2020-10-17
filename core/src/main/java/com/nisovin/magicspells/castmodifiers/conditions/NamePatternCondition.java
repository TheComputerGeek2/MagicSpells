package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.castmodifiers.Condition;

public class NamePatternCondition extends Condition {

	private String rawPattern;
	private Pattern compiledPattern;
	
	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		rawPattern = var;
		compiledPattern = Pattern.compile(rawPattern);
		// note, currently won't translate the & to the color code,
		// this will need to be done through regex unicode format 
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		if (!(target instanceof Player)) return false;
		return RegexUtil.matches(compiledPattern, target.getName()) || RegexUtil.matches(compiledPattern, ((Player) target).getDisplayName());
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
