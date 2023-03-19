package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.spells.instant.LeapSpell;

public class LeapingCondition extends Condition {

	private LeapSpell leapSpell;
	
	@Override
	public boolean initialize(String var) {
		Spell spell = MagicSpells.getSpellByInternalName(var);
		if (!(spell instanceof LeapSpell)) return false;
		leapSpell = (LeapSpell) spell;
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isLeaping(caster);
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isLeaping(target);
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean isLeaping(LivingEntity target) {
		return leapSpell.isJumping(target);
	}

}
