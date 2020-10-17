package com.nisovin.magicspells.spells.passive.util;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.PassiveSpell;

public abstract class PassiveListener implements Listener {

	protected PassiveSpell passiveSpell;

	protected EventPriority priority;

	public PassiveSpell getPassiveSpell() {
		return passiveSpell;
	}

	public void setPassiveSpell(PassiveSpell passiveSpell) {
		this.passiveSpell = passiveSpell;
	}

	public EventPriority getEventPriority() {
		return priority;
	}

	public void setEventPriority(EventPriority priority) {
		this.priority = priority;
	}

	public boolean canTrigger(LivingEntity livingEntity) {
		return passiveSpell.getTriggerList().canTarget(livingEntity, true);
	}

	public boolean canTrigger(LivingEntity livingEntity, boolean ignoreGameMode) {
		return passiveSpell.getTriggerList().canTarget(livingEntity, ignoreGameMode);
	}
		
	public boolean cancelDefaultAction(boolean casted) {
		if (passiveSpell == null) return true;
		if (casted && passiveSpell.cancelDefaultAction()) return true;
		if (!casted && passiveSpell.cancelDefaultActionWhenCastFails()) return true;
		return false;
	}
	
	public boolean isCancelStateOk(boolean cancelled) {
		if (passiveSpell == null) return false;
		if (passiveSpell.ignoreCancelled() && cancelled) return false;
		if (passiveSpell.requireCancelledEvent() && !cancelled) return false;
		return true;
	}

	public boolean hasSpell(LivingEntity entity) {
		if (entity instanceof Player) {
			return MagicSpells.getSpellbook((Player) entity).hasSpell(passiveSpell);
		}
		return true;
	}
	
	public abstract void initialize(String var);

	public void turnOff() {
		// No op
	}
	
	@Override
	public boolean equals(Object other) {
		return this == other; // Don't want to make things equal unless they are the same object
	}
	
}
