package com.nisovin.magicspells.castmodifiers;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public abstract class Condition {

	public abstract boolean initialize(String var);

	public abstract boolean check(LivingEntity livingEntity);

	public abstract boolean check(LivingEntity livingEntity, LivingEntity target);

	public abstract boolean check(LivingEntity livingEntity, Location location);

}
