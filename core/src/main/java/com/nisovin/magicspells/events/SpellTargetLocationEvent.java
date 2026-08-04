package com.nisovin.magicspells.events;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellData;

public class SpellTargetLocationEvent extends SpellEvent implements Cancellable {

	private SpellData spellData;

	private boolean cancelled = false;
	private boolean castCancelled = false;

	public SpellTargetLocationEvent(Spell spell, SpellData spellData) {
		super(spell, spellData.caster());

		this.spellData = spellData;
	}

	public SpellTargetLocationEvent(Spell spell, SpellData spellData, Location target) {
		this(spell, spellData.location(target));
	}

	@Deprecated
	public SpellTargetLocationEvent(Spell spell, LivingEntity caster, Location target, float power) {
		super(spell, caster);

		spellData = new SpellData(caster, target, power, null);
	}

	@Override
	public LivingEntity getCaster() {
		return spellData.caster();
	}

	/**
	 * Gets the location that is being targeted by the spell.
	 * @return the targeted living entity
	 */
	public Location getTargetLocation() {
		return spellData.location();
	}

	/**
	 * Sets the spell's target to the provided location.
	 * @param target the new target
	 */
	public void setTargetLocation(Location target) {
		spellData = spellData.location(target);
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
		if (castCancelled) cancelled = true;
	}

}
