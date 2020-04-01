package com.nisovin.magicjutsus.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.magicjutsus.MagicJutsus;

/**
 * This event is fired whenever MagicJutsus begins loading, either after the server first starts,
 * after a server reload (/reload), or after an internal reload (/cast reload).
 *
 */
public class MagicJutsusLoadingEvent extends Event {
	
    private static final HandlerList handlers = new HandlerList();

    private MagicJutsus plugin;
    
    public MagicJutsusLoadingEvent(MagicJutsus plugin) {
    	this.plugin = plugin;
    }
    
    /**
     * Gets the instance of the MagicJutsus plugin
     * @return plugin instance
     */
    public MagicJutsus getPlugin() {
    	return plugin;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
    
}
