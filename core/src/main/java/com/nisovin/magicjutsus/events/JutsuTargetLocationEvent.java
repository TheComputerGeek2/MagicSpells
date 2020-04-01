package com.nisovin.magicjutsus.events;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;

public class JutsuTargetLocationEvent extends JutsuEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

	private Location target;
	private float power;
	private boolean cancelled = false;
	
	public JutsuTargetLocationEvent(Jutsu jutsu, LivingEntity caster, Location target, float power) {
		super(jutsu, caster);
		this.target = target;
		this.power = power;
	}
	
	/**
	 * Gets the location that is being targeted by the jutsu.
	 * @return the targeted living entity
	 */
	public Location getTargetLocation() {
		return target;
	}
	
	/**
	 * Sets the jutsu's target to the provided location.
	 * @param target the new target
	 */
	public void setTargetLocation(Location target) {
		this.target = target;
	}
	
	/**
	 * Gets the current power level of the jutsu. Jutsus start at a power level of 1.0.
	 * @return the power level
	 */
	public float getPower() {
		return power;
	}
	
	/**
	 * Sets the power level for the jutsu being cast.
	 * @param power the power level
	 */
	public void setPower(float power) {
		this.power = power;
	}
	
	/**
	 * Increases the power lever for the jutsu being cast by the given multiplier.
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

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
