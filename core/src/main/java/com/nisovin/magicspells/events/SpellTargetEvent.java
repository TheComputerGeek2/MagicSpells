package com.nisovin.magicspells.events;

import org.bukkit.event.Cancellable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellData;

/**
 * This event is fired whenever a TargetedSpell is trying to target an entity.
 * Cancelling this event will prevent the spell from targeting the entity.
 *
 */
public class SpellTargetEvent extends SpellEvent implements Cancellable {

	private SpellData spellData;

	private boolean cancelled = false;
	private boolean castCancelled = false;

	public SpellTargetEvent(Spell spell, SpellData spellData) {
		super(spell, spellData.caster());

		this.spellData = spellData;
	}

	public SpellTargetEvent(Spell spell, SpellData spellData, LivingEntity target) {
		this(spell, spellData.target(target));
	}

	@Deprecated
	public SpellTargetEvent(Spell spell, LivingEntity caster, LivingEntity target, float power) {
		super(spell, caster);

		this.spellData = new SpellData(caster, target, power, null);
	}

	@Override
	public LivingEntity getCaster() {
		return spellData.caster();
	}

	/**
	 * Gets the living entity that is being targeted by the spell.
	 * @return the targeted living entity
	 */
	public LivingEntity getTarget() {
		return spellData.target();
	}
	
	/**
	 * Sets the spell's target to the provided living entity.
	 * @param target the new target
	 */
	public void setTarget(LivingEntity target) {
		spellData = spellData.target(target);
	}
	
	/**
	 * Gets the current power level of the spell. Spells start at a power level of 1.0.
	 * @return the power level
	 */
	public float getPower() {
		return spellData.power();
	}
	
	/**
	 * Sets the power level for the spell being cast.
	 * @param power the power level
	 */
	public void setPower(float power) {
		spellData = spellData.power(power);
	}

	/**
	 * Gets the current spell arguments.
	 * @return the spell arguments
	 */
	public String[] getSpellArgs() {
		return spellData.args();
	}

	/**
	 * Increases the power lever for the spell being cast by the given multiplier.
	 * @param power the power level multiplier
	 */
	public void increasePower(float power) {
		spellData = spellData.power(spellData.power() * power);
	}

	/**
	 * Gets the spell data for the associated cast.
	 * @return the spell data
	 */
	public SpellData getSpellData() {
		return spellData;
	}

	/**
	 * Sets the spell's {@link SpellData} to the provided value.
	 * @param data the new data
	 */
	public void setSpellData(SpellData data) {
		spellData = data;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;		
	}

	public boolean isCastCancelled() {
		return castCancelled;
	}

	public void setCastCancelled(boolean castCancelled) {
		this.castCancelled = castCancelled;
		cancelled = true;
	}

}
