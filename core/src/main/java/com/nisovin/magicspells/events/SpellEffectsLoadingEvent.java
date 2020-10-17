package com.nisovin.magicspells.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.managers.SpellEffectManager;

/**
 * This event is fired whenever MagicSpells begins loading spell effects.
 */
public class SpellEffectsLoadingEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private final MagicSpells plugin;

	private final SpellEffectManager spellEffectManager;

	public SpellEffectsLoadingEvent(MagicSpells plugin, SpellEffectManager spellEffectManager) {
		this.plugin = plugin;
		this.spellEffectManager = spellEffectManager;
	}

	/**
	 * Gets the instance of the MagicSpells plugin
	 * @return plugin instance
	 */
	public MagicSpells getPlugin() {
		return plugin;
	}

	public SpellEffectManager getSpellEffectManager() {
		return spellEffectManager;
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
