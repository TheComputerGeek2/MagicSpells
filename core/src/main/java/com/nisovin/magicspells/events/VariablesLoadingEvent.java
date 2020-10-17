package com.nisovin.magicspells.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.managers.VariableManager;

/**
 * This event is fired whenever MagicSpells begins loading variables.
 */
public class VariablesLoadingEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private final MagicSpells plugin;

	private final VariableManager variableManager;

	public VariablesLoadingEvent(MagicSpells plugin, VariableManager variableManager) {
		this.plugin = plugin;
		this.variableManager = variableManager;
	}

	/**
	 * Gets the instance of the MagicSpells plugin
	 * @return plugin instance
	 */
	public MagicSpells getPlugin() {
		return plugin;
	}

	public VariableManager getVariableManager() {
		return variableManager;
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
