package com.nisovin.magicspells.events;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.Spell.PostCastAction;

/** 
 * The event that is called whenever a player casts a spell. 
 * This event is called after the spell is done and everything has been handled.
 *
 */
public class SpellCastedEvent extends SpellEvent {

	private final SpellCastState state;
	private final float cooldown;
	private final SpellReagents reagents;
	private final PostCastAction action;
	private final SpellData spellData;

	@Deprecated
	public SpellCastedEvent(Spell spell, LivingEntity caster, SpellCastState state, float power, String[] args, float cooldown, SpellReagents reagents, PostCastAction action) {
		super(spell, caster);

		this.state = state;
		this.cooldown = cooldown;
		this.reagents = reagents;
		this.action = action;
		this.spellData = new SpellData(caster, power, args);
	}

	public SpellCastedEvent(Spell spell, SpellCastState state, PostCastAction action, SpellData data, float cooldown, SpellReagents reagents) {
		super(spell, data.caster());

		this.state = state;
		this.cooldown = cooldown;
		this.reagents = reagents;
		this.action = action;
		this.spellData = data;
	}

	public SpellCastedEvent(SpellCastEvent castEvent, PostCastAction action, SpellData data) {
		this(castEvent.getSpell(), castEvent.getSpellCastState(), action, data, castEvent.getCooldown(), castEvent.getReagents());
	}

	public SpellCastedEvent(SpellCastEvent castEvent, CastResult result) {
		this(castEvent.getSpell(), castEvent.getSpellCastState(), result.action(), result.data(), castEvent.getCooldown(), castEvent.getReagents());
	}

	@Override
	public LivingEntity getCaster() {
		return spellData.caster();
	}

	/**
	 * Gets the current spell cast state.
	 * @return the spell cast state
	 */
	public SpellCastState getSpellCastState() {
		return state;
	}
	
	/**
	 * Gets the cooldown that was triggered by the spell.
	 * @return the cooldown
	 */
	public float getCooldown() {
		return cooldown;
	}
	
	/**
	 * Gets the reagents that were charged.
	 * @return the reagents
	 */
	public SpellReagents getReagents() {
		return reagents;
	}
	
	/**
	 * Gets the power level of the spell. Spells start at a power level of 1.0.
	 * @return the power level
	 */
	public float getPower() {
		return spellData.power();
	}
	
	/**
	 * Gets the arguments passed to the spell if the spell was cast by command.
	 * @return the args, or null if there were none
	 */
	public String[] getSpellArgs() {
		return spellData.args();
	}
	
	/**
	 * Gets the post cast action that was executed for the spell cast.
	 * @return action
	 */
	public PostCastAction getPostCastAction() {
		return action;
	}

	/**
	 * Gets the spell data associated with this spell cast.
	 * @return the spell data
	 */
	public SpellData getSpellData() {
		return spellData;
	}

}
