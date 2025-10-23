package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.castmodifiers.Condition;

@Name("isnpc")
public class IsNPCCondition extends Condition {

	@Override
	public boolean initialize(@NotNull String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isNPC(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isNPC(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean isNPC(LivingEntity target) {
		return target.hasMetadata("NPC") || target.getType() == EntityType.MANNEQUIN;
	}

}
