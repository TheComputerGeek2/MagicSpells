package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
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
	public boolean check(LivingEntity livingEntity) {
		if (!(livingEntity instanceof Player)) return false;
		return spell.getMarks().containsKey(livingEntity.getName().toLowerCase());
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
