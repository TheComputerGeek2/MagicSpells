package com.nisovin.magicjutsus.events;

import org.bukkit.event.HandlerList;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.JutsuReagents;
import com.nisovin.magicjutsus.Jutsu.JutsuCastState;
import com.nisovin.magicjutsus.Jutsu.PostCastAction;

/** 
 * The event that is called whenever a player casts a jutsu.
 * This event is called after the jutsu is done and everything has been handled.
 *
 */
public class JutsuCastedEvent extends JutsuEvent {
	
    private static final HandlerList handlers = new HandlerList();

    private JutsuCastState state;
	private float cooldown;
	private JutsuReagents reagents;
	private float power;
	private String[] args;
	private PostCastAction action;
	
	public JutsuCastedEvent(Jutsu jutsu, LivingEntity caster, JutsuCastState state, float power, String[] args, float cooldown, JutsuReagents reagents, PostCastAction action) {
		super(jutsu, caster);
		this.state = state;
		this.cooldown = cooldown;
		this.reagents = reagents;
		this.power = power;
		this.args = args;
		this.action = action;
	}
	
	/**
	 * Gets the current jutsu cast state.
	 * @return the jutsu cast state
	 */
	public JutsuCastState getJutsuCastState() {
		return state;
	}
	
	/**
	 * Gets the cooldown that was triggered by the jutsu.
	 * @return the cooldown
	 */
	public float getCooldown() {
		return cooldown;
	}
	
	/**
	 * Gets the reagents that were charged.
	 * @return the reagents
	 */
	public JutsuReagents getReagents() {
		return reagents;
	}
	
	/**
	 * Gets the power level of the jutsu. Jutsus start at a power level of 1.0.
	 * @return the power level
	 */
	public float getPower() {
		return power;
	}
	
	/**
	 * Gets the arguments passed to the jutsu if the jutsu was cast by command.
	 * @return the args, or null if there were none
	 */
	public String[] getJutsuArgs() {
		return args;
	}
	
	/**
	 * Gets the post cast action that was executed for the jutsu cast.
	 * @return
	 */
	public PostCastAction getPostCastAction() {
		return action;
	}

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
