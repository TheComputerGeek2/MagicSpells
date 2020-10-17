package com.nisovin.magicspells.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.managers.ConditionManager;

/**
 * This event is fired whenever MagicSpells begins loading conditions.
 */
public class ConditionsLoadingEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private final MagicSpells plugin;

	private final ConditionManager conditionManager;

	public ConditionsLoadingEvent(MagicSpells plugin, ConditionManager conditionManager) {
		this.plugin = plugin;
		this.conditionManager = conditionManager;
	}

	/**
	 * Gets the instance of the MagicSpells plugin
	 * @return plugin instance
	 */
	public MagicSpells getPlugin() {
		return plugin;
	}

	public ConditionManager getConditionManager() {
		return conditionManager;
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
