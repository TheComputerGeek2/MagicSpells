package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.castmodifiers.Condition;

public class BuffActiveCondition extends Condition {

	private BuffSpell buff;

	@Override
	public boolean initialize(String var) {
		Spell spell = MagicSpells.getSpellByInternalName(var);
		if (spell instanceof BuffSpell) {
			buff = (BuffSpell) spell;
			return true;
		}
		return false;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return active(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return active(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean active(LivingEntity target) {
		return buff.isActiveAndNotExpired(target);
	}

}
