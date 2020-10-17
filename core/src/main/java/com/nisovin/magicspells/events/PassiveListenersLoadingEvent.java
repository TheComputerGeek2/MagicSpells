package com.nisovin.magicspells.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.managers.PassiveManager;

/**
 * This event is fired whenever MagicSpells begins loading passive listeners.
 */
public class PassiveListenersLoadingEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private final MagicSpells plugin;

	private final PassiveManager passiveManager;

	public PassiveListenersLoadingEvent(MagicSpells plugin, PassiveManager passiveManager) {
		this.plugin = plugin;
		this.passiveManager = passiveManager;
	}

	/**
	 * Gets the instance of the MagicSpells plugin
	 * @return plugin instance
	 */
	public MagicSpells getPlugin() {
		return plugin;
	}

	public PassiveManager getPassiveManager() {
		return passiveManager;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
