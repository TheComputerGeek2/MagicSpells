package com.nisovin.magicjutsus.jutsus.passive;

import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nisovin.magicjutsus.jutsus.PassiveJutsu;

public abstract class PassiveListener implements Listener {

	EventPriority priority;
	
	public EventPriority getEventPriority() {
		return priority;
	}
		
	public static boolean cancelDefaultAction(PassiveJutsu jutsu, boolean casted) {
		if (casted && jutsu.cancelDefaultAction()) return true;
		if (!casted && jutsu.cancelDefaultActionWhenCastFails()) return true;
		return false;
	}
	
	public static boolean isCancelStateOk(PassiveJutsu jutsu, boolean cancelled) {
		if (jutsu.ignoreCancelled() && cancelled) return false;
		if (jutsu.requireCancelledEvent() && !cancelled) return false;
		return true;
	}
	
	public abstract void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var);
	
	public void initialize() {
		// No op
	}
	
	public void turnOff() {
		// No op
	}
	
	@Override
	public boolean equals(Object other) {
		return this == other; // Don't want to make things equal unless they are the same object
	}
	
}
