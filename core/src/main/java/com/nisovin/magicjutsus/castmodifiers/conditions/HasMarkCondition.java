package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.castmodifiers.Condition;
import com.nisovin.magicjutsus.jutsus.instant.MarkJutsu;

public class HasMarkCondition extends Condition {

	private MarkJutsu jutsu;
	
	@Override
	public boolean setVar(String var) {
		Jutsu s = MagicJutsus.getJutsuByInternalName(var);
		if (s == null) return false;
		if (!(s instanceof MarkJutsu)) return false;
		jutsu = (MarkJutsu) s;
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		if (!(livingEntity instanceof Player)) return false;
		return jutsu.getMarks().containsKey(livingEntity.getName().toLowerCase());
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
