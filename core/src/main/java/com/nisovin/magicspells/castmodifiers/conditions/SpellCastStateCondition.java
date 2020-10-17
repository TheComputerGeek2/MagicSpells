package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.castmodifiers.IModifier;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

/**
 * Valid condition variable arguments are any of the following:
 * NORMAL
 * ON_COOLDOWN
 * MISSING_REAGENTS
 * CANT_CAST
 * NO_MAGIC_ZONE
 * WRONG_WORLD
 */
public class SpellCastStateCondition extends Condition implements IModifier {

	private SpellCastState state;

	@Override
	public boolean initialize(String var) {
		if (var == null) return false;
		try {
			state = SpellCastState.valueOf(var.trim().toUpperCase());
			return true;
		} catch (IllegalArgumentException badValueString) {
			MagicSpells.error("Invalid SpellCastState of \"" + var.trim() + "\" on this modifier var");
			return false;
		}
	}

	@Override
	public boolean apply(SpellCastEvent event) {
		return event.getSpellCastState() == state;
	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		return false;
	}

	@Override
	public boolean apply(SpellTargetEvent event) {
		return false;
	}

	@Override
	public boolean apply(SpellTargetLocationEvent event) {
		return false;
	}

	@Override
	public boolean apply(MagicSpellsGenericPlayerEvent event) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
