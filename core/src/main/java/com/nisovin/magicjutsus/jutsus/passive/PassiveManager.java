package com.nisovin.magicjutsus.jutsus.passive;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.event.HandlerList;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;

public class PassiveManager {

	Set<PassiveListener> listeners = new HashSet<>();
	Set<PassiveTrigger> triggers = new HashSet<>();
	boolean initialized = false;
	
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		triggers.add(trigger);
		PassiveListener listener = trigger.getListener();
		if (listener != null) {
			listeners.add(listener);
			listener.registerJutsu(jutsu, trigger, var);
		} else {
			MagicJutsus.error("Failed to register passive jutsu (no listener): " + jutsu.getInternalName() + ", " + trigger.getName());
		}
	}
	
	public void initialize() {
		if (initialized) return;
		initialized = true;
		for (PassiveListener listener : listeners) {
			listener.initialize();
		}
	}
	
	public void turnOff() {
		for (PassiveListener listener : listeners) {
			HandlerList.unregisterAll(listener);
			listener.turnOff();
		}
		for (PassiveTrigger trigger : triggers) {
			trigger.listener = null;
		}
		listeners.clear();
	}
	
}
