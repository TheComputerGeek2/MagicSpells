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
	public boolean check(LivingEntity caster) {
		return onCooldown(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return onCooldown(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean onCooldown(LivingEntity target) {
		return spell.onCooldown(target);
	}

}
