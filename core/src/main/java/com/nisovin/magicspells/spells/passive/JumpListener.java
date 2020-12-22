package com.nisovin.magicspells.spells.passive;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is currently used
public class JumpListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}

	@OverridePriority
	@EventHandler
	public void onJump(EntityJumpEvent event) {
		LivingEntity entity = event.getEntity();
		if (!hasSpell(entity)) return;
		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(entity);
		if (!cancelDefaultAction(casted)) return;
		event.setCancelled(true);
	}

}
