package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.spells.targeted.LevitateSpell;

@Name("levitating")
public class LevitatingCondition extends Condition {

	private LevitateSpell levitateSpell;

	@Override
	public boolean initialize(@NotNull String var) {
		Spell spell = MagicSpells.getSpellByInternalName(var);
		if (!(spell instanceof LevitateSpell levitate)) return false;
		levitateSpell = levitate;
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return levitateSpell.isLevitating(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		for (LevitateSpell.Levitator levitator : levitateSpell.getLevitating().values()) {
			if (!levitator.getData().caster().equals(caster)) continue;
			if (!levitator.getData().target().equals(target)) continue;
			return true;
		}
		return false;
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

}
