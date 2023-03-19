package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.spells.targeted.LoopSpell;
import com.nisovin.magicspells.spells.targeted.LoopSpell.Loop;

public class OwnedLoopActiveCondition extends Condition {

	private LoopSpell loopSpell;

	@Override
	public boolean initialize(String var) {
		Spell spell = MagicSpells.getSpellByInternalName(var);
		if (spell instanceof LoopSpell loop) {
			loopSpell = loop;
			return true;
		}

		return false;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkLoop(caster, caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkLoop(caster, target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean checkLoop(LivingEntity caster, LivingEntity target) {
		Collection<Loop> loops = loopSpell.getActiveLoops().get(target.getUniqueId());
		for (Loop loop : loops) {
			if (caster.equals(loop.getCaster())) return true;
		}
		return false;
	}

}
