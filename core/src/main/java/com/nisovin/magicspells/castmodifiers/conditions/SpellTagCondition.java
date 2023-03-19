package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.ModifierResult;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.castmodifiers.IModifier;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

public class SpellTagCondition extends Condition implements IModifier {

	private String tag;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		tag = var.trim();
		return true;
	}
	
	@Override
	public boolean apply(SpellCastEvent event) {
		return checkSpell(event.getSpell());
	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		return false;
	}

	@Override
	public boolean apply(SpellTargetEvent event) {
		return checkSpell(event.getSpell());
	}

	@Override
	public boolean apply(SpellTargetLocationEvent event) {
		return checkSpell(event.getSpell());
	}

	@Override
	public boolean apply(MagicSpellsGenericPlayerEvent event) {
		return false;
	}

	@Override
	public ModifierResult apply(LivingEntity caster, SpellData data) {
		return new ModifierResult(data, false);
	}

	@Override
	public ModifierResult apply(LivingEntity caster, LivingEntity target, SpellData data) {
		return new ModifierResult(data, false);
	}

	@Override
	public ModifierResult apply(LivingEntity caster, Location target, SpellData data) {
		return new ModifierResult(data, false);
	}

	private boolean checkSpell(Spell spell) {
		if (spell == null) return false;
		Set<String> tags = spell.getTags();
		return checkWithTags(tags);
	}
	
	private boolean checkWithTags(Set<String> tags) {
		return tag != null && tags.contains(tag);
	}

	@Override
	public boolean check(LivingEntity caster) {
		return false;
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return false;
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

}
