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
	public boolean check(LivingEntity livingEntity) {
		return leapSpell.isJumping(livingEntity);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return leapSpell.isJumping(target);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
