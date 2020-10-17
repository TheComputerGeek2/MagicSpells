package com.nisovin.magicspells.spells.passive;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityToggleGlideEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is currently used
public class StartGlideListener extends PassiveListener {
	
	@Override
	public void initialize(String var) {

	}
	
	@OverridePriority
	@EventHandler
	public void onGlide(EntityToggleGlideEvent event) {
		if (!(event.getEntity() instanceof LivingEntity)) return;
		LivingEntity entity = (LivingEntity) event.getEntity();

		if (!event.isGliding()) return;
		if (!hasSpell(entity)) return;
		if (!canTrigger(entity)) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(entity);
		if (!cancelDefaultAction(casted)) return;
		event.setCancelled(true);
	}

}
