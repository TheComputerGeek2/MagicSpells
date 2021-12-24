package com.nisovin.magicspells.events;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;

public class SpellTargetLocationEvent extends SpellEvent implements Cancellable {

	private Location target;
	private String[] args;
	private float power;
	private boolean cancelled = false;

	public SpellTargetLocationEvent(Spell spell, LivingEntity caster, Location target, float power, String[] args) {
		super(spell, caster);
		this.target = target;
		this.power = power;
		this.args = args;
	}

	public SpellTargetLocationEvent(Spell spell, LivingEntity caster, Location target, float power) {
		super(spell, caster);
		this.target = target;
		this.power = power;
		this.args = null;
	}

	/**
	 * Gets the location that is being targeted by the spell.
	 * @return the targeted living entity
	 */
	public Location getTargetLocation() {
		return target;
	}

	/**
	 * Sets the spell's target to the provided location.
	 * @param target the new target
	 */
	public void setTargetLocation(Location target) {
		this.target = target;
	}

	/**
	 * Gets the current power level of the spell. Spells start at a power level of 1.0.
	 * @return the power level
	 */
	public float getPower() {
		return power;
	}

	/**
	 * Sets the power level for the spell being cast.
	 * @param power the power level
	 */
	public void setPower(float power) {
		this.power = power;
	}

	/**
	 * Gets the current spell arguments.
	 * @return the spell arguments
	 */
	public String[] getSpellArgs() {
		return args;
	}

	/**
	 * Increases the power lever for the spell being cast by the given multiplier.
	 * @param power the power level multiplier
	 */
	public void increasePower(float power) {
		this.power *= power;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

}
