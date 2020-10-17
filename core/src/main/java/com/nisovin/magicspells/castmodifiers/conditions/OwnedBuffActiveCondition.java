package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.castmodifiers.Condition;

public class OwnedBuffActiveCondition extends Condition {

	private BuffSpell buff;

	@Override
	public boolean initialize(String var) {
		Spell spell = MagicSpells.getSpellByInternalName(var);
		if (!(spell instanceof BuffSpell)) return false;
		buff = (BuffSpell) spell;
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return isOwned(caster, caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return isOwned(caster, target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean isOwned(LivingEntity caster, LivingEntity target) {
		if (target == null) return false;
		return buff.isActiveAndNotExpired(target) && buff.getLastCaster(target).equals(caster);
	}

}
