package com.nisovin.magicjutsus.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.JutsuReagents;
import com.nisovin.magicjutsus.Jutsu.JutsuCastState;
/**
 * The event that is called whenever a player attempts to cast a jutsu.
 * This event is called just before the effects of the jutsu are performed.
 * Cancelling this event will prevent the jutsu from casting.
 *
 */
public class JutsuCastEvent extends JutsuEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private JutsuCastState state;
    private boolean stateChanged;
	private float cooldown;
	private JutsuReagents reagents;
	private boolean reagentsChanged;
	private float power;
	private int castTime;
	private String[] args;
	private boolean cancelled = false;
	
	public JutsuCastEvent(Jutsu jutsu, LivingEntity caster, JutsuCastState state, float power, String[] args, float cooldown, JutsuReagents reagents, int castTime) {
		super(jutsu, caster);
		this.state = state;
		this.cooldown = cooldown;
		this.reagents = reagents;
		this.power = power;
		this.castTime = castTime;
		this.args = args;
		stateChanged = false;
		reagentsChanged = false;
	}
	
	/**
	 * Gets the current jutsu cast state.
	 * @return the jutsu cast state
	 */
	public JutsuCastState getJutsuCastState() {
		return state;
	}
	
	/**
	 * Changes the jutsu cast state.
	 * @param state the new jutsu cast state
	 */
	public void setJutsuCastState(JutsuCastState state) {
		this.state = state;
		stateChanged = true;
	}
	
	/**
	 * Checks whether the jutsu cast state has been changed.
	 * @return true if it has been changed
	 */
	public boolean hasJutsuCastStateChanged() {
		return stateChanged;
	}
	
	/**
	 * Gets the cooldown that will be triggered after the jutsu is cast.
	 * @return the cooldown
	 */
	public float getCooldown() {
		return cooldown;
	}
	
	/**
	 * Sets the cooldown that will be triggered after the jutsu is cast.
	 * @param cooldown the cooldown to set
	 */
	public void setCooldown(float cooldown) {
		this.cooldown = cooldown;
	}
	
	/**
	 * Gets the reagents that will be charged after the jutsu is cast. This can be modified.
	 * @return the reagents
	 */
	public JutsuReagents getReagents() {
		return reagents;
	}
	
	/**
	 * Changes the jutsu's required cast reagents.
	 * @param reagents the new reagents
	 */
	public void setReagents(JutsuReagents reagents) {
		this.reagents = reagents;
		reagentsChanged = true;
	}
	
	/**
	 * Gets whether the jutsu's reagents have been changed by this event.
	 * @return true if reagents are changed
	 */
	public boolean haveReagentsChanged() {
		return reagentsChanged;
	}
	
	/**
	 * Sets whether the reagents have been changed. If a plugin changes the reagents list,
	 * this should be called and set to true.
	 * @param reagentsChanged whether reagents have been changed
	 */
	public void setReagentsChanged(boolean reagentsChanged) {
		this.reagentsChanged = reagentsChanged;
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
	
	/**
	 * Gets the cast time for this jutsu cast, in server ticks.
	 * @return the cast time
	 */
	public int getCastTime() {
		return castTime;
	}
	
	/**
	 * Sets the cast time for this jutsu cast, in server ticks.
	 * @param castTime the new cast time
	 */
	public void setCastTime(int castTime) {
		this.castTime = castTime;
	}
	
	/**
	 * Gets the arguments passed to the jutsu if the jutsu was cast by command.
	 * @return the args, or null if there were none
	 */
	public String[] getJutsuArgs() {
		return args;
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
