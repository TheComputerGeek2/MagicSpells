package com.nisovin.magicjutsus.castmodifiers.conditions;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.events.ManaChangeEvent;
import com.nisovin.magicjutsus.castmodifiers.Condition;
import com.nisovin.magicjutsus.castmodifiers.IModifier;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;
import com.nisovin.magicjutsus.events.MagicJutsusGenericPlayerEvent;

public class JutsuTagCondition extends Condition implements IModifier {

	private String tag;

	@Override
	public boolean setVar(String var) {
		if (var == null) return false;
		tag = var.trim();
		return true;
	}
	
	@Override
	public boolean apply(JutsuCastEvent event) {
		return checkJutsu(event.getJutsu());
	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		return false;
	}

	@Override
	public boolean apply(JutsuTargetEvent event) {
		return checkJutsu(event.getJutsu());
	}

	@Override
	public boolean apply(JutsuTargetLocationEvent event) {
		return checkJutsu(event.getJutsu());
	}

	@Override
	public boolean apply(MagicJutsusGenericPlayerEvent event) {
		return false;
	}
	
	private boolean checkJutsu(Jutsu jutsu) {
		if (jutsu == null) return false;
		Set<String> tags = jutsu.getTags();
		return checkWithTags(tags);
	}
	
	private boolean checkWithTags(Set<String> tags) {
		return tag != null && tags.contains(tag);
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
