package com.nisovin.magicjutsus.events;

import org.bukkit.event.Event;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.nisovin.magicjutsus.mana.ManaChangeReason;

/**
 * This event is fired whenever a player's mana value is changed.
 *
 */
public class ManaChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    
	private Player player;
	private int oldAmt;
	private int newAmt;
	private int maxMana;
	private ManaChangeReason reason;
	
	public ManaChangeEvent(Player player, int oldAmt, int newAmt, int maxMana, ManaChangeReason reason) {		
		this.player = player;
		this.oldAmt = oldAmt;
		this.newAmt = newAmt;
		this.maxMana = maxMana;
		this.reason = reason;
	}
	
	/**
	 * Gets the player whose mana is being modified
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}
	
	/**
	 * Gets the amount of mana the player currently has.
	 * @return mana amount
	 */
	public int getOldAmount() {
		return oldAmt;
	}
	
	/**
	 * Gets the amount of mana the player will have.
	 * @return mana amount
	 */
	public int getNewAmount() {
		return newAmt;
	}
	
	/**
	 * Sets the amount of mana the player will have.
	 * @param mana amount
	 */
	public void setNewAmount(int mana) {
		newAmt = mana;
	}
	
	/**
	 * The maximum amount of mana the player can have.
	 * @return max mana
	 */
	public int getMaxMana() {
		return maxMana;
	}
	
	/**
	 * Gets the reason for the mana change (regen, jutsu cost, mana potion, or other).
	 * @return the reason
	 */
	public ManaChangeReason getReason() {
		return reason;
	}

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
