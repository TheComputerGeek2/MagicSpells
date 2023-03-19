package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.spells.instant.MarkSpell;

public class HasMarkCondition extends Condition {

	private MarkSpell spell;
	
	@Override
	public boolean initialize(String var) {
		Spell s = MagicSpells.getSpellByInternalName(var);
		if (s == null) return false;
		if (!(s instanceof MarkSpell)) return false;

		spell = (MarkSpell) s;
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return hasMark(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return hasMark(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean hasMark(LivingEntity target) {
		return spell.getMarks().containsKey(target.getUniqueId());
	}

}
