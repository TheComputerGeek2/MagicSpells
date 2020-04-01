package com.nisovin.magicjutsus.events;

import org.bukkit.event.Event;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import com.nisovin.magicjutsus.Jutsu;

public class JutsuForgetEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    
	private Jutsu jutsu;
	private Player forgetter;
	private boolean cancelled;

	public JutsuForgetEvent(Jutsu jutsu, Player forgetter) {
		this.jutsu = jutsu;
		this.forgetter = forgetter;
	}
	
	public Player getForgetter() {
		return forgetter;
	}
	
	public Jutsu getJutsu() {
		return jutsu;
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
