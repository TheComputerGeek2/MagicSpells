package com.nisovin.magicjutsus.events;

import org.bukkit.event.Event;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import com.nisovin.magicjutsus.Jutsu;

/**
 * This event is fired whenever a player is about to learn a jutsu, either from
 * the teach jutsu, a jutsubook, a tome, or from an external plugin calling the
 * MagicJutsus.teachJutsu method.
 *
 */
public class JutsuLearnEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

	private Jutsu jutsu;
	private Player learner;
	private LearnSource source;
	private Object teacher;
	private boolean cancelled;
	
	public JutsuLearnEvent(Jutsu jutsu, Player learner, LearnSource source, Object teacher) {
		this.jutsu = jutsu;
		this.learner = learner;
		this.source = source;
		this.teacher = teacher;
		this.cancelled = false;
	}

	/**
	 * Gets the jutsu that is going to be learned
	 * @return the learned jutsu
	 */
	public Jutsu getJutsu() {
		return jutsu;
	}
	
	/**
	 * Gets the player that will be learning the jutsu
	 * @return the learning player
	 */
	public Player getLearner() {
		return learner;
	}
	
	/**
	 * Gets the source of the learning (teach, jutsubook, tome, other)
	 * @return the source
	 */
	public LearnSource getSource() {
		return source;
	}
	
	/**
	 * Gets the object that is teaching the jutsu
	 * @return the player/console for teach, the block for jutsubook, or the book item for tome, or null
	 */
	public Object getTeacher() {
		return teacher;
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
	
	public enum LearnSource {
		
		TEACH,
		JUTSUBOOK,
		TOME,
		MAGIC_XP,
		OTHER
		
	}

}
