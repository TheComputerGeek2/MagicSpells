package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class OnCooldownCondition extends Condition {

	private Spell spell;
	
	@Override
	public boolean initialize(String var) {
		spell = MagicSpells.getSpellByInternalName(var);
		return spell != null;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return spell.onCooldown(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return spell.onCooldown(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
