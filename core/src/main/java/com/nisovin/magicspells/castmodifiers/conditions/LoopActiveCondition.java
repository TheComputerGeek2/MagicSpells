package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.spells.targeted.LoopSpell;

public class LoopActiveCondition extends Condition {

	private LoopSpell loop;

	@Override
	public boolean initialize(String var) {
		Spell spell = MagicSpells.getSpellByInternalName(var);
		if (spell instanceof LoopSpell loopSpell) {
			loop = loopSpell;
			return true;
		}

		return false;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return loopActive(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return loopActive(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean loopActive(LivingEntity target) {
		return loop.isActive(target);
	}

}
