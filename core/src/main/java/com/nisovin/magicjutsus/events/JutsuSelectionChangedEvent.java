package com.nisovin.magicjutsus.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.util.CastItem;

public class JutsuSelectionChangedEvent extends JutsuEvent {

    private static final HandlerList handlers = new HandlerList();
    
    private CastItem castItem;
    private Jutsubook jutsubook;
    
	public JutsuSelectionChangedEvent(Jutsu jutsu, Player caster, CastItem castItem, Jutsubook jutsubook) {
		super(jutsu, caster);
		this.castItem = castItem;
		this.jutsubook = jutsubook;
	}
	
	public CastItem getCastItem() {
		return castItem;
	}
	
	public Jutsubook getJutsubook() {
		return jutsubook;
	}
	
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
